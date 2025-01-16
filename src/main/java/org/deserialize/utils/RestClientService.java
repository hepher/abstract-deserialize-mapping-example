package com.enel.eic.commons.rest;


import com.enel.eic.commons.exception.CommonsException;
import com.enel.eic.commons.util.LabelUtils;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StopWatch;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
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

    private static final Map<Class<?>, Function<String, ?>> WRAPPER_TYPE_EVAL_MAP = Map.of(
            Integer.class, Integer::valueOf,
            Byte.class, (resultAsString) -> resultAsString.getBytes(StandardCharsets.UTF_8),
            Character.class, (resultAsString) -> resultAsString.charAt(0),
            Boolean.class, Boolean::valueOf,
            Double.class, Double::valueOf,
            Float.class, Float::valueOf,
            Long.class, Long::valueOf,
            Short.class, Long::valueOf,
            String.class, (resultAsString) -> resultAsString
    );

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
    private TypeReference<?> resultTypeReference;
    private BiFunction<String, HttpHeaders, ?> successResponseParser;

    // request error handler
    private Map<HttpStatus, HttpStatusErrorHandler> httpStatusErrorConsumers;
    private HttpStatusErrorHandler serverErrorHandler;
    private HttpStatusErrorHandler clientErrorHandler;
    private BiFunction<CommonsException, String, CommonsException> exceptionErrorResponseParser;

    // request config
    private Integer connectionTimeout = 55*1000;
    private Integer requestTimeout = 55*1000;
    private List<String> sslHostnameVerifierList;
    private RequestFactoryStrategy requestFactoryStrategy;

    private RestClientService() {
        requestFactoryStrategy = RequestFactoryStrategyEnum.HANDSHAKE_CERT_VALIDATION;
    }

    public static RestClientService instance() {
        return new RestClientService();
    }

    public static HttpStatusErrorHandler instanceHttpStatusConsumer(Function<HttStatusHandlerParam, ?> errorResponseFunction) {
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
        if (queryParameters == null) {
            queryParameters = new HashMap<>();
        }
        this.queryParameters.putAll(queryParameterMap);
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

    public RestClientService header(String key, String value) {
        return header(key, new String[]{value});
    }

    public RestClientService header(String key, String... values) {
        if (headers == null) {
            headers = new LinkedMultiValueMap<>();
        }

        if (values == null) {
            headers.put(key, null);
        } else {
            headers.put(key, Arrays.stream(values).toList());
        }

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

    public RestClientService resultTypeReference(TypeReference<?> typeReference) {
        this.resultTypeReference = typeReference;
        return this;
    }

    public RestClientService onSuccessResponse(BiFunction<String, HttpHeaders, ?> successResponseParser) {
        this.successResponseParser = successResponseParser;
        return this;
    }

    public RestClientService onHttpStatusError(HttpStatus httpStatus, Function<HttStatusHandlerParam, ?> httpStatusErrorFunction) {
        if (httpStatus == null || httpStatusErrorFunction == null) {
            return this;
        }

        if (httpStatusErrorConsumers == null) {
            httpStatusErrorConsumers = new EnumMap<>(HttpStatus.class);
        }

        httpStatusErrorConsumers.put(httpStatus, new HttpStatusErrorHandler(httpStatusErrorFunction));
        return this;
    }

    public RestClientService on5xxServerError(Function<HttStatusHandlerParam, ?> httpServerErrorFunction) {
        if (httpServerErrorFunction == null) {
            return this;
        }

        serverErrorHandler = new HttpStatusErrorHandler(httpServerErrorFunction);
        return this;
    }

    public RestClientService on4xxServerError(Function<HttStatusHandlerParam, ?> httpClientErrorFunction) {
        if (httpClientErrorFunction == null) {
            return this;
        }

        clientErrorHandler = new HttpStatusErrorHandler(httpClientErrorFunction);
        return this;
    }

    public RestClientService exceptionErrorResponseParser(BiFunction<CommonsException, String, CommonsException> exceptionErrorResponseParser) {
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

        if (StringUtils.isBlank(url) || method == null || (resultClass == null && resultTypeReference == null && successResponseParser == null)) {
            throw new CommonsException("Missing required parameter[ url or method or result ]");
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

	StringBuilder builder = new StringBuilder(url);
        Map<String, Object> uriParameterMap = new HashMap<>();
        // resolve url parameters
        // resolve url parameters for no-post request
        if (queryParameters != null && !queryParameters.isEmpty()) {
            builder.append("?");
            int keySetIndex = 0;
            for (String key : queryParameters.keySet()) {
                if (queryParameters.get(key).getClass().isArray()) {
                    var array = (Object[]) queryParameters.get(key);
                    for (int i = 0; i < array.length; i++) {
                        // prepare uri map parameter
                        uriParameterMap.put(key + i, array[i]);

                        // append query list parameter
                        builder.append(key).append("=").append("{").append(key).append(i).append("}");
                        if (i < array.length-1) {
                            builder.append("&");
                        }
                    }
                } if (queryParameters.get(key) instanceof Collection) {
                    var list = (Collection<Object>) queryParameters.get(key);
                    int i = 0;
                    for (Object value : list) {
                        // prepare uri map parameter
                        uriParameterMap.put(key + i, value);

                        // append query list parameter
                        builder.append(key).append("=").append("{").append(key).append(i).append("}");
                        if (i < list.size()-1) {
                            builder.append("&");
                        }
                        i++;
                    }
                } else {
                    builder.append(key).append("=").append("{").append(key).append("}");
                    uriParameterMap.put(key, queryParameters.get(key));
                }

                if (keySetIndex < queryParameters.keySet().size()-1) {
                    builder.append("&");
                }
                keySetIndex++;
            }
        }

        // replace url parameter
        if (pathParameters != null && !pathParameters.isEmpty()) {
            uriParameterMap.putAll(pathParameters);
        }

        restClientBuilder.defaultUriVariables(uriParameterMap);
        restClientBuilder.baseUrl(builder.toString());

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

            log.info(LabelUtils.LOG_REST_CALL_REQUEST_RESPONSE,
                    request.getURI(),
                    request.getMethod(),
                    request.getHeaders(),
                    stopWatch.getTotalTimeMillis() + "ms",
                    requestBody != null ? mapper.writeValueAsString(requestBody) : null,
                    response.getStatusCode(),
                    response.getHeaders(),
                    result.getBytes().length > MAX_BODY_SIZE ? ApplicationContextUtils.replaceContextLineSeparator(new String(result.getBytes(), 0, MAX_BODY_SIZE).concat(OMISSIS)) : ApplicationContextUtils.replaceContextLineSeparator(result));

            if (response.getStatusCode().isError()) {
                HttpStatusErrorHandler httpStatusErrorHandler = null;

                Function<HttpStatusErrorHandler, T> evalHttpStatusErrorFunction = (httpStatus) -> {
                    var handlerResult = httpStatus.getErrorHandlerFunction().apply(new HttStatusHandlerParam(this, result, httpStatus.retryDone));

                    if (handlerResult == null) {
                        return null;
                    }

                    if (handlerResult instanceof Boolean) {
                        if (Boolean.TRUE.equals(handlerResult) && Boolean.FALSE.equals(httpStatus.retryDone)) {
                            httpStatus.retryDone = true;
                            return exchange();
                        }
                    } else {
                        return (T) handlerResult;
                    }

                    return null;
                };

		if (httpStatusErrorConsumers != null && (httpStatusErrorHandler = httpStatusErrorConsumers.get(HttpStatus.valueOf(response.getStatusCode().value()))) != null) {
                    var handlerResult = evalHttpStatusErrorFunction.apply(httpStatusErrorHandler);
                    if (handlerResult != null) {
                        return handlerResult;
                    }
		}

                if (clientErrorHandler != null
                        &&response.getStatusCode().is4xxClientError()
                        && httpStatusErrorHandler == null) {
                    var handlerResult = evalHttpStatusErrorFunction.apply(clientErrorHandler);
                    if (handlerResult != null) {
                        return handlerResult;
                    }
                }

                if (serverErrorHandler != null
                        && response.getStatusCode().is5xxServerError()
                        && httpStatusErrorHandler == null) {
                    var handlerResult = evalHttpStatusErrorFunction.apply(serverErrorHandler);
                    if (handlerResult != null) {
                        return handlerResult;
                    }
                }

                CommonsException commonsException = new CommonsException();
                commonsException.setHttpStatus(HttpStatus.valueOf(response.getStatusCode().value()));
                commonsException.setSystemErrorResponse(StringUtils.isNotBlank(result) ? mapper.readValue(result, Object.class) : null);
                commonsException.setErrorCode(String.valueOf(response.getStatusCode().value()));

                if (exceptionErrorResponseParser != null) {
                    throw exceptionErrorResponseParser.apply(commonsException, result);
                }

                throw commonsException;
            } else {

                if (successResponseParser != null) {
                    return (T) successResponseParser.apply(result, response.getHeaders());
                }

                if (StringUtils.isBlank(result)) {
                    return null;
                }

                if (resultClass != null) {
                    if (WRAPPER_TYPE_EVAL_MAP.containsKey(resultClass)) {
                        return (T) WRAPPER_TYPE_EVAL_MAP.get(resultClass).apply(result);
                    }

                    return (T) mapper.readValue(result, resultClass);
                }

                return (T) mapper.readValue(result, resultTypeReference);
            }
        });
    }

    public record HttStatusHandlerParam(RestClientService restClientService, String errorResponse, Boolean retryDone) {}

    @Getter
    public static class HttpStatusErrorHandler {
        private Boolean retryDone;
        private final Function<HttStatusHandlerParam, ?> errorHandlerFunction;

        public HttpStatusErrorHandler(Function<HttStatusHandlerParam, ?> errorHandlerFunction) {
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

    @Override
    public String toString() {
        return String.format("url='%s', method='%s'", url, method);
    }
}
