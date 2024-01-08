package com.enelx.bfw.framework.rest;

import com.enelx.bfw.framework.exception.BfwException;
import com.enelx.bfw.framework.util.LabelUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.ParameterMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Slf4j
public class RestClientService {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final String AUTH_PREFIX_BASIC = "Basic ";
    private static final String AUTH_PREFIX_BEARER = "Bearer ";
    private static final String HEADER_KEY_AUTH = "Authorization";
    private static final int MAX_BODY_SIZE = 4096;
    private static final String OMISSIS = "{...}";

    // request composition fields
    private String url;
    private HttpMethod method;
    private Map<String, Object> queryParameters;
    private Map<String, String> pathParameters;
    private MultiValueMap<String, String> headers;
    private HttpAuthenticationHeader authenticationHeader;
    private Object requestBody;
    private Class<?> resultClass;

    // request error handler
    private Map<HttpStatus, HttpStatusErrorFunction> httpStatusErrorConsumers;
    private BiFunction<BfwException, String, BfwException> parseErrorResponseException;

    // request config
    private Integer connectionTimeout = 55*1000;
    private Integer requestTimeout = 55*1000;
    private List<String> sslHostnameVerifierList;
    private RequestFactoryStrategy requestFactoryStrategy;

    private final BiFunction<String, Map<String, Object>, String> encodeUrlParameterVariableFunction = (url, urlParameterMap) -> {
        StringBuilder builder = new StringBuilder(url);
        if (urlParameterMap != null && !urlParameterMap.isEmpty()) {
            builder.append("?").append(urlParameterMap.keySet().stream().map(key -> key + "=" + "{" + key + "}").collect(Collectors.joining("&")));
        }
        return builder.toString();
    };

    private RestClientService() {
        requestFactoryStrategy = RequestFactoryStrategyEnum.HANDSHAKE_CERT_VALIDATION;
    }

    public static RestClientService instance() {
        return new RestClientService();
    }

    public static HttpStatusErrorFunction instanceHttpStatusConsumer(BiFunction<RestClientService, String, Boolean> errorResponseFunction) {
        return new HttpStatusErrorFunction(errorResponseFunction);
    }

    public RestClientService url(String url) {
        this.url = url;
        return this;
    }

    public RestClientService method(HttpMethod method) {
        this.method = method;
        return this;
    }

    public RestClientService queryParameter(String key, Object value) {
        if (queryParameters == null) {
            queryParameters = new HashMap<>();
        }
        queryParameters.put(key, value);
        return this;
    }

    public RestClientService queryParameters(Map<String, Object> queryParameterMap) {
        this.queryParameters = queryParameterMap;
        return this;
    }

    public RestClientService pathParameter(String key, String value) {
        if (pathParameters == null) {
            pathParameters = new ParameterMap<>();
        }
        pathParameters.put(key, value);
        return this;
    }

    public RestClientService pathParameters(Map<String, String> pathParamterMap) {
        this.pathParameters = pathParamterMap;
        return this;
    }

    public RestClientService header(String key, String value, boolean replace) {
        if (headers == null) {
            headers = new LinkedMultiValueMap<>();
        }
        if (replace) {
            headers.put(key, Collections.singletonList(value));
        } else {
            headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return this;
    }

    public RestClientService header(String key, List<String> values) {
        if (headers == null) {
            headers = new LinkedMultiValueMap<>();
        }
        headers.put(key, values);
        return this;
    }

    public RestClientService headers(MultiValueMap<String, String> headerMap) {
        this.headers = headerMap;
        return this;
    }

    public RestClientService basicAuth(String user, String password) {
        this.authenticationHeader = new HttpAuthenticationHeader(HEADER_KEY_AUTH, AUTH_PREFIX_BASIC + new String(Base64.getEncoder().encode((user + ":" + password).getBytes(StandardCharsets.UTF_8))));
        return this;
    }

    public RestClientService bearerAuth(String bearerToken) {
        this.authenticationHeader = new HttpAuthenticationHeader(HEADER_KEY_AUTH, AUTH_PREFIX_BEARER + bearerToken);
        return this;
    }

    public RestClientService authenticationToken(String authenticationToken) {
        this.authenticationHeader = new HttpAuthenticationHeader(HEADER_KEY_AUTH, authenticationToken);
        return this;
    }

    public RestClientService authentication(String authenticationHeaderName, String authenticationToken) {
        this.authenticationHeader = new HttpAuthenticationHeader(authenticationHeaderName, authenticationToken);
        return this;
    }

    public RestClientService requestBody(Object requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public RestClientService resultClass(Class<?> klass) {
        this.resultClass = klass;
        return this;
    }

    public RestClientService httpStatusErrorConsumer(HttpStatus httpStatus, HttpStatusErrorFunction httpStatusErrorFunction) {
        if (httpStatusErrorConsumers == null) {
            httpStatusErrorConsumers = new HashMap<>();
        }

        httpStatusErrorConsumers.put(httpStatus, httpStatusErrorFunction);
        return this;
    }

    public RestClientService parseErrorResponseException(BiFunction<BfwException, String, BfwException> parseErrorResponseException) {
        this.parseErrorResponseException = parseErrorResponseException;
        return this;
    }

    public RestClientService connectionTimeout(int connectionTimeout) {
        if (connectionTimeout > 0) {
            this.connectionTimeout = connectionTimeout;
        }

        return this;
    }

    public RestClientService requestTimeout(int requestTimeout) {
        if (requestTimeout > 0) {
            this.requestTimeout = requestTimeout;
        }

        return this;
    }

    public RestClientService sslHostnameVerifierList(List<String> sslHostnameVerifierList) {
        this.sslHostnameVerifierList = sslHostnameVerifierList;
        return this;
    }

    public RestClientService requestFactoryStrategy(RequestFactoryStrategy requestFactoryStrategy) {

        if (requestFactoryStrategy != null) {
            this.requestFactoryStrategy = requestFactoryStrategy;
        }

        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T exchange() {

        if (StringUtils.isBlank(url) || method == null) {
            throw new BfwException(String.format("Missing required parameter[ url: %s, method: %s ]", url, method));
        }

        RestClient.Builder restClientBuilder = RestClient.builder();

        // resolve headers
        restClientBuilder.defaultHeader("Content-Type", "application/json");
        if (headers != null) {
            restClientBuilder.defaultHeaders(httpHeaders -> httpHeaders.addAll(headers));
        }

        if (authenticationHeader != null) {
            restClientBuilder.defaultHeader(authenticationHeader.headerName, authenticationHeader.token);
        }

        Map<String, Object> uriParameterMap = new HashMap<>();
        // resolve url parameters for no-post request
        if (queryParameters != null && !queryParameters.isEmpty()) {
            uriParameterMap.putAll(queryParameters);
        }

        // replace url parameter
        if (pathParameters != null && !pathParameters.isEmpty()) {
            uriParameterMap.putAll(pathParameters);
        }

        restClientBuilder.defaultUriVariables(uriParameterMap);
        restClientBuilder.baseUrl(encodeUrlParameterVariableFunction.apply(url, queryParameters));

        try {
            HttpComponentsClientHttpRequestFactory requestFactory = requestFactoryStrategy.createRequestFactory(sslHostnameVerifierList);
            requestFactory.setConnectionRequestTimeout(requestTimeout);
            requestFactory.setConnectTimeout(connectionTimeout);

            restClientBuilder.requestFactory(requestFactory);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {}

        RestClient restClient = restClientBuilder.build();

        RestClient.RequestBodyUriSpec requestSpec = restClient.method(method);
        if (requestBody != null) {
            requestSpec.body(requestBody);
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        return requestSpec.exchange((request, response) -> {
            stopWatch.stop();

            String result = getStringFromInputStream(response.getBody());

            log.info(LabelUtils.LOG_ALL,
                    request.getURI(),
                    request.getMethod(),
                    request.getHeaders(),
                    stopWatch.getTotalTimeMillis() + "ms",
                    requestBody != null ? mapper.writeValueAsString(requestBody) : null,
                    response.getStatusCode(),
                    response.getHeaders(),
                    result.getBytes().length > MAX_BODY_SIZE ? new String(result.getBytes(), 0, MAX_BODY_SIZE).concat(OMISSIS) : result);

            if (response.getStatusCode().isError()) {
                HttpStatusErrorFunction httpStatusErrorFunction;
                if (httpStatusErrorConsumers != null && (httpStatusErrorFunction = httpStatusErrorConsumers.get(HttpStatus.valueOf(response.getStatusCode().value()))) != null) {
                    if (Boolean.FALSE.equals(httpStatusErrorFunction.retryDone) && httpStatusErrorFunction.getErrorResponseFunction().apply(this, result)) {
                        httpStatusErrorFunction.retryDone = true;
                        return exchange();
                    }
                }

                BfwException bfwException = new BfwException();
                bfwException.setHttpStatus(HttpStatus.valueOf(response.getStatusCode().value()));
                bfwException.setTransactionId(MDC.get(LabelUtils.TRANSACTION_ID));
                bfwException.setSystemErrorResponse(mapper.readValue(result, Object.class));

                if (parseErrorResponseException != null) {
                    throw parseErrorResponseException.apply(bfwException, result);
                }

                throw bfwException;
            } else {
                if (resultClass != null) {
                    return (T) mapper.readValue(result, resultClass);
                }

                return mapper.readValue(result, new TypeReference<>() {});
            }
        });
    }

    @Getter
    public static class HttpStatusErrorFunction {
        private Boolean retryDone;
        private final BiFunction<RestClientService, String, Boolean> errorResponseFunction;

        public HttpStatusErrorFunction(BiFunction<RestClientService, String, Boolean> errorResponseFunction) {
            this.errorResponseFunction = errorResponseFunction;
            retryDone = false;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class HttpAuthenticationHeader {
        private String headerName;
        private String token;
    }

    public interface RequestFactoryStrategy {
        HttpComponentsClientHttpRequestFactory createRequestFactory(List<String> sslHostnameVerifierList) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException;
    }

    public enum RequestFactoryStrategyEnum implements RequestFactoryStrategy {
        HANDSHAKE_CERT_VALIDATION {
            @Override
            public HttpComponentsClientHttpRequestFactory createRequestFactory(List<String> sslHostnameVerifierList) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
                SSLContext sslContext = createSSLContext();

                SSLConnectionSocketFactory sslConnectionSocketFactory = createSSLConnectionSocketFactory(sslContext, sslHostnameVerifierList);

                PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(sslConnectionSocketFactory)
                        .build();

                CloseableHttpClient httpClient = HttpClients.custom()
                        .setConnectionManager(poolingHttpClientConnectionManager)
                        .build();

                return new HttpComponentsClientHttpRequestFactory(httpClient);
            }

            SSLContext createSSLContext() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
                return SSLContexts.custom()
                        .loadTrustMaterial(null, (x509Certificates, s) -> { // Check only server certificate's validation, ignore truststore
                            Arrays.stream(x509Certificates).forEach(x509Certificate -> {
                                try {
                                    x509Certificate.checkValidity();
                                } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                            return true;
                        })
                        .build();
            }

            SSLConnectionSocketFactory createSSLConnectionSocketFactory(SSLContext sslContext, List<String> sslHostnameVerifierList) {
                return SSLConnectionSocketFactoryBuilder.create()
                        .setSslContext(sslContext)
                        .setHostnameVerifier((s, sslSession) -> {
                            if (sslHostnameVerifierList == null || sslHostnameVerifierList.isEmpty()) {
                                return true;
                            }

                            return sslHostnameVerifierList.stream().anyMatch(s::matches);
                        })
                        .build();
            }
        },
        KEYSTORE_VALIDATION {
            @Override
            public HttpComponentsClientHttpRequestFactory createRequestFactory(List<String> sslHostnameVerifierList) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
                return new HttpComponentsClientHttpRequestFactory();
            }
        },
        IGNORE_VALIDATION {

            @Override
            public HttpComponentsClientHttpRequestFactory createRequestFactory(List<String> sslHostnameVerifierList) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
                SSLContext sslContext = createSSLContext();

                SSLConnectionSocketFactory sslConnectionSocketFactory = createSSLConnectionSocketFactory(sslContext, sslHostnameVerifierList);

                PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(sslConnectionSocketFactory)
                        .build();

                CloseableHttpClient httpClient = HttpClients.custom()
                        .setConnectionManager(poolingHttpClientConnectionManager)
                        .build();

                return new HttpComponentsClientHttpRequestFactory(httpClient);
            }

            SSLContext createSSLContext() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
                return SSLContexts.custom()
                        .loadTrustMaterial(TrustAllStrategy.INSTANCE) // <- ignore ssl authentication
                        .build();
            }

            SSLConnectionSocketFactory createSSLConnectionSocketFactory(SSLContext sslContext, List<String> sslHostnameVerifierList) {
                return SSLConnectionSocketFactoryBuilder.create()
                        .setSslContext(sslContext)
                        .setHostnameVerifier((s, sslSession) -> true)
                        .build();
            }
        };
    }

    private String getStringFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = inputStream.read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8);
    }
}
