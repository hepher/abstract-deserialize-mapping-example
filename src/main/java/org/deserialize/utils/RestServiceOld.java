package com.x.bfw.framework.rest;

import com.enelx.bfw.framework.exception.BfwException;
import com.enelx.bfw.framework.util.LabelUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.ParameterMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.Response;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StopWatch;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
    private Class<?> responseErrorClass;
    private Map<HttpStatus, HttpStatusErrorFunction> errorResponseConsumers;
    private BiFunction<BfwException, String, BfwException> handleErrorResponseException;

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

    public RestClientService resultClass(Class<?> resultClass) {
        this.resultClass = resultClass;
        return this;
    }

    public RestClientService errorResponseConsumer(HttpStatus httpStatus, HttpStatusErrorFunction httpStatusErrorFunction) {
        if (errorResponseConsumers == null) {
            errorResponseConsumers = new HashMap<>();
        }

        errorResponseConsumers.put(httpStatus, httpStatusErrorFunction);
        return this;
    }

    public RestClientService handleErrorResponseException(BiFunction<BfwException, String, BfwException> handleErrorResponseException) {
        this.handleErrorResponseException = handleErrorResponseException;
        return this;
    }

    public <T> T exchange() throws JsonProcessingException {

        if (StringUtils.isBlank(url) || method == null) {
            throw new BfwException(String.format("Missing required parameter[ url: %s, method: %s ]", url, method));
        }

        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();

        // resolve headers
        restTemplateBuilder.defaultHeader("Content-Type", "application/json");
        if (headers != null) {
            headers.forEach((key, values) -> {
                restTemplateBuilder.defaultHeader(key, values.toArray(new String[0]));
            });
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

        URI uri = UriComponentsBuilder
            .fromHttpUrl(encodeUrlParameterVariableFunction.apply(url, queryParameters))
            .build(uriParameterMap);

        RestTemplate restTemplate = restTemplateBuilder.build();

        HttpEntity<String> entity;
        if (requestBody != null) {
            entity = new HttpEntity<>(mapper.writeValueAsString(requestBody), headers);
        } else {
            entity = new HttpEntity<>(headers);
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {

            ResponseEntity<String> responseEntity = restTemplate.exchange(uri, method, entity, String.class);
            stopWatch.stop();

            String resultAsString = responseEntity.getBody();

            log.info(LabelUtils.LOG_ALL,
                    uri,
                    method,
                    headers,
                    stopWatch.getTotalTimeMillis() + "ms",
                    requestBody != null ? mapper.writeValueAsString(requestBody) : null,
                    responseEntity.getStatusCode(),
                    responseEntity.getHeaders(),
                    resultAsString
            );

            if (resultClass != null) {
                return (T) mapper.readValue(resultAsString, resultClass);
            }

            return mapper.readValue(resultAsString, new TypeReference<>() {});

        } catch (RestClientResponseException responseException) {
            stopWatch.stop();

            String responseErrorAsString = responseException.getResponseBodyAsString();

            log.info(LabelUtils.LOG_ALL,
                    uri,
                    method,
                    headers,
                    stopWatch.getTotalTimeMillis() + "ms",
                    requestBody != null ? mapper.writeValueAsString(requestBody) : null,
                    HttpStatus.valueOf(responseException.getRawStatusCode()),
                    responseException.getResponseHeaders(),
                    responseErrorAsString
            );

            HttpStatusErrorFunction httpStatusErrorFunction;
            if (errorResponseConsumers != null && (httpStatusErrorFunction = errorResponseConsumers.get(HttpStatus.valueOf(responseException.getRawStatusCode()))) != null) {
                if (Boolean.FALSE.equals(httpStatusErrorFunction.retryDone) && httpStatusErrorFunction.getErrorResponseFunction().apply(this, responseErrorAsString)) {
                    httpStatusErrorFunction.retryDone = true;
                    return exchange();
                }
            }

            BfwException bfwException = new BfwException();
            bfwException.setHttpStatus(HttpStatus.valueOf(responseException.getRawStatusCode()));
            bfwException.setTransactionId(MDC.get(LabelUtils.TRANSACTION_ID));
            bfwException.setSystemErrorResponse(responseErrorAsString);

            if (handleErrorResponseException != null) {
                throw handleErrorResponseException.apply(bfwException, responseErrorAsString);
            }

            throw bfwException;
        }
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
