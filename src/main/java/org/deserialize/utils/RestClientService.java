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
    private static final String HEADER_KEY_CONTENT_TYPE = "Content-Type";
    private static final int MAX_BODY_SIZE = 4096;
    private static final String OMISSIS = "{...}";

    // request composition fields
    private String url;
    private HttpMethod method;
    private Map<String, Object> queryParameters;
    private Map<String, Object> pathParameters;
    private MultiValueMap<String, String> headers;
    private HttpAuthenticationHeader authenticationHeader;
    private Object requestBody;

    // response success handler
    private Class<?> resultClass;
    private BiFunction<String, HttpHeaders, ?> successResponseParser;

    // request error handler
    private Map<HttpStatus, HttpStatusErrorHandler> httpStatusErrorConsumers;
    private BiFunction<BfwException, String, BfwException> exceptionErrorResponseParser;

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

    public static HttpStatusErrorHandler instanceHttpStatusConsumer(BiPredicate<RestClientService, String> errorResponseFunction) {
        return new HttpStatusErrorHandler(errorResponseFunction);
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

    public RestClientService pathParameters(Map<String, Object> pathParamterMap) {
        this.pathParameters = pathParamterMap;
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

    public RestClientService successResponseParser(BiFunction<String, HttpHeaders, ?> successResponseParser) {
        this.successResponseParser = successResponseParser;
        return this;
    }

    public RestClientService httpStatusErrorHandler(HttpStatus httpStatus, BiPredicate<RestClientService, String> httpStatusErrorFunction) {
        if (httpStatus == null || httpStatusErrorFunction == null) {
            return this;
        }

        if (httpStatusErrorConsumers == null) {
            httpStatusErrorConsumers = new EnumMap<>(HttpStatus.class);
        }

        httpStatusErrorConsumers.put(httpStatus, new HttpStatusErrorHandler(httpStatusErrorFunction));
        return this;
    }

    public RestClientService exceptionErrorResponseParser(BiFunction<BfwException, String, BfwException> exceptionErrorResponseParser) {
        this.exceptionErrorResponseParser = exceptionErrorResponseParser;
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
        if (headers == null) {
            headers = new LinkedMultiValueMap<>();
            headers.put(HEADER_KEY_CONTENT_TYPE, Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
        }

        headers.putIfAbsent(HEADER_KEY_CONTENT_TYPE, Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
        restClientBuilder.defaultHeaders(httpHeaders -> httpHeaders.addAll(headers));

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
            requestSpec.body(RequestBodyStrategy.getStrategy(headers.getFirst(HEADER_KEY_CONTENT_TYPE)).parse(requestBody));
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        return requestSpec.exchange((request, response) -> {
            stopWatch.stop();

            String result = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);

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
                HttpStatusErrorHandler httpStatusErrorHandler;
				if (httpStatusErrorConsumers != null
						&& (httpStatusErrorHandler = httpStatusErrorConsumers.get(HttpStatus.valueOf(response.getStatusCode().value()))) != null
						&& (Boolean.FALSE.equals(httpStatusErrorHandler.retryDone) 
						&& httpStatusErrorHandler.getErrorHandlerFunction().test(this, result))) {
					httpStatusErrorHandler.retryDone = true;
					return exchange();
				}

                BfwException bfwException = new BfwException();
                bfwException.setHttpStatus(HttpStatus.valueOf(response.getStatusCode().value()));
                bfwException.setSystemErrorResponse(mapper.readValue(result, Object.class));

                if (exceptionErrorResponseParser != null) {
                    throw exceptionErrorResponseParser.apply(bfwException, result);
                }

                throw bfwException;
            } else {

                if (successResponseParser != null) {
                    return (T) successResponseParser.apply(result, response.getHeaders());
                }
                
                if (StringUtils.isBlank(result)) {
                    return null;
                }

                if (resultClass != null) {
                    return (T) mapper.readValue(result, resultClass);
                }

                return mapper.readValue(result, new TypeReference<>() {});
            }
        });
    }

    @Getter
    public static class HttpStatusErrorHandler {
        private Boolean retryDone;
        private final BiPredicate<RestClientService, String> errorHandlerFunction;

        public HttpStatusErrorHandler(BiPredicate<RestClientService, String> errorHandlerFunction) {
            this.errorHandlerFunction = errorHandlerFunction;
            retryDone = false;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class HttpAuthenticationHeader {
        private String headerName;
        private String token;
    }

    @AllArgsConstructor
    private enum RequestBodyStrategy {
        FORM_URLENCODED(MediaType.APPLICATION_FORM_URLENCODED_VALUE) {
            @Override
            Object parse(Object body) {
                if (body == null) {
                    return null;
                }

                if (body instanceof LinkedMultiValueMap<?, ?>) {
                    return body;
                }

                Map<String, Object> map = new ObjectMapper().convertValue(body, new TypeReference<>() {});

                MultiValueMap<String, Object> result = new LinkedMultiValueMap<>();
                result.setAll(map);

                return result;
            }
        },
        JSON (MediaType.APPLICATION_JSON_VALUE) {
            @Override
            Object parse(Object body) {
                return body;
            }
        };

        final String mediaType;

        static RequestBodyStrategy getStrategy(String contentType) {
            if (StringUtils.isBlank(contentType)) {
                return JSON;
            }

            return Arrays.stream(values())
                    .filter(requestBodyStrategy -> requestBodyStrategy.mediaType.equals(contentType))
                    .findFirst()
                    .orElse(JSON);
        }
        abstract Object parse(Object body);
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
        }
    }
}
