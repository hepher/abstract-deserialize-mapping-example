package com.enelx.bfw.framework.rest;

import com.enelx.bfw.framework.exception.BfwException;
import com.enelx.bfw.framework.util.LabelUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.ParameterMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Slf4j
public class RestClientService {

    private final ObjectMapper mapper = new ObjectMapper();

    private String url;
    private HttpMethod method;
    private Map<String, Object> queryParameters;
    private Map<String, String> pathParameters;
    private MultiValueMap<String, String> headers;
    private Object requestBody;
    private Class<?> resultClass;
    private Map<HttpStatus, HttpStatusErrorFunction> errorResponseConsumers;
    private BiConsumer<BfwException, String> parseErrorBodyResponse;

    private final BiFunction<String, Map<String, Object>, String> encodeUrlParameterVariableFunction = (url, urlParameterMap) -> {
        StringBuilder builder = new StringBuilder(url);
        if (urlParameterMap != null && !urlParameterMap.isEmpty()) {
            builder.append("?").append(urlParameterMap.keySet().stream().map(key -> key + "=" + "{" + key + "}").collect(Collectors.joining("&")));
        }
        return builder.toString();
    };

    private RestClientService() {}

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

    public RestClientService requestBody(Object requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public RestClientService resultClass(Class<?> klass) {
        this.resultClass = klass;
        return this;
    }

    public RestClientService errorResponseConsumer(HttpStatus httpStatus, HttpStatusErrorFunction httpStatusErrorFunction) {
        if (errorResponseConsumers == null) {
            errorResponseConsumers = new HashMap<>();
        }

        errorResponseConsumers.put(httpStatus, httpStatusErrorFunction);
        return this;
    }

    public RestClientService parseErrorBodyResponse(BiConsumer<BfwException, String> parseErrorBodyResponse) {
        this.parseErrorBodyResponse = parseErrorBodyResponse;
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
                    result);

            if (response.getStatusCode().isError()) {
                HttpStatusErrorFunction httpStatusErrorFunction;
                if (errorResponseConsumers != null && (httpStatusErrorFunction = errorResponseConsumers.get(HttpStatus.valueOf(response.getStatusCode().value()))) != null) {
                    if (Boolean.FALSE.equals(httpStatusErrorFunction.retryDone) && httpStatusErrorFunction.getErrorResponseFunction().apply(this, result)) {
                        httpStatusErrorFunction.retryDone = true;
                        return exchange();
                    }
                }

                BfwException bfwException = new BfwException();
                bfwException.setHttpStatus(HttpStatus.valueOf(response.getStatusCode().value()));
                bfwException.setTransactionId(MDC.get(LabelUtils.TRANSACTION_ID));
                bfwException.setSystemErrorResponse(mapper.readValue(result, Object.class));

                if (parseErrorBodyResponse != null) {
                    parseErrorBodyResponse.accept(bfwException, result);
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

    private String getStringFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = inputStream.read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8);
    }
}
