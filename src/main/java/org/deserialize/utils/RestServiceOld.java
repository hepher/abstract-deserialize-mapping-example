package com.x.bfw.framework.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class RestServiceOld {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Function<String, String> pathVariableFunction = (parameterName) -> "{" + parameterName + "}";
    private final BiFunction<String, Object, String> urlParameterFunction = (parameterName, parameterValue) -> parameterName + "=" + parameterValue;
    private final BiFunction<String, List<String>, String> normalizeUrlFunction = (url, urlParameterList) -> {
        String endpoint = url;

        if (urlParameterList != null && !urlParameterList.isEmpty()) {
            endpoint += "?" + String.join("&", urlParameterList);
        }

        return endpoint;
    };

    /** GET CALL **/
    protected <T> T getCall(String url, Map<String, Object> urlParameterMap, ParameterizedTypeReference<T> parameterizedTypeReference) throws JsonProcessingException {
        return restCall(url, HttpMethod.GET, null, urlParameterMap, null, null, parameterizedTypeReference, null);
    }

    protected <T> T getCall(String url, Map<String, Object> urlParameterMap, Class<T> resultType) throws JsonProcessingException {
        return restCall(url, HttpMethod.GET, null, urlParameterMap, null, null,null, resultType);
    }

    protected <T> T getCall(String url, Map<String, Object> urlParameterMap, Map<String, String> pathParameterMap, Map<String, String> headerMap, ParameterizedTypeReference<T> parameterizedTypeReference) throws JsonProcessingException {
        return restCall(url, HttpMethod.GET, null, urlParameterMap, pathParameterMap, headerMap, parameterizedTypeReference, null);
    }

    protected <T> T getCall(String url, Map<String, Object> urlParameterMap, Map<String, String> pathParameterMap, Map<String, String> headerMap, Class<T> resultType) throws JsonProcessingException {
        return restCall(url, HttpMethod.GET, null, urlParameterMap, pathParameterMap, headerMap, null, resultType);
    }

    /** POST CALL **/
    protected <T> T postCall(String url, Object jsonBody, ParameterizedTypeReference<T> parameterizedTypeReference) throws JsonProcessingException {
        return restCall(url, HttpMethod.POST, jsonBody, null, null, null, parameterizedTypeReference, null);
    }

    protected <T> T postCall(String url, Object jsonBody, Class<T> resultType) throws JsonProcessingException {
        return restCall(url, HttpMethod.POST, jsonBody, null, null, null, null, resultType);
    }

    protected <T> T postCall(String url, Object jsonBody, Map<String, String> pathParameterMap, Map<String, String> headerMap, ParameterizedTypeReference<T> parameterizedTypeReference) throws JsonProcessingException {
        return restCall(url, HttpMethod.POST, jsonBody, null, pathParameterMap, headerMap, parameterizedTypeReference, null);
    }

    protected <T> T postCall(String url, Object jsonBody, Map<String, String> pathParameterMap, Map<String, String> headerMap, Class<T> resultType) throws JsonProcessingException {
        return restCall(url, HttpMethod.POST, jsonBody, null, pathParameterMap, headerMap, null, resultType);
    }

    /** PATCH CALL **/
    protected <T> T patchCall(String url, Map<String, Object> urlParameterMap, Class<T> resultType) throws JsonProcessingException {
        return restCall(url, HttpMethod.PATCH, null, urlParameterMap, null, null, null, resultType);
    }

    protected <T> T patchCall(String url, Map<String, Object> urlParameterMap, ParameterizedTypeReference<T> parameterizedTypeReference) throws JsonProcessingException {
        return restCall(url, HttpMethod.PATCH, null, urlParameterMap, null, null, parameterizedTypeReference, null);
    }

    protected <T> T patchCall(String url, Map<String, Object> urlParameterMap, Map<String, String> pathParameterMap, Map<String, String> headerMap, Class<T> resultType) throws JsonProcessingException {
        return restCall(url, HttpMethod.PATCH, null, urlParameterMap, pathParameterMap, headerMap, null, resultType);
    }

    protected <T> T patchCall(String url, Map<String, Object> urlParameterMap, Map<String, String> pathParameterMap, Map<String, String> headerMap, ParameterizedTypeReference<T> parameterizedTypeReference) throws JsonProcessingException {
        return restCall(url, HttpMethod.PATCH, null, urlParameterMap, pathParameterMap, headerMap, parameterizedTypeReference, null);
    }

    /** PUT CALL **/
    protected <T> T putCall(String url, Map<String, Object> urlParameterMap, ParameterizedTypeReference<T> parameterizedTypeReference) throws JsonProcessingException {
        return restCall(url, HttpMethod.PATCH, null, urlParameterMap, null, null, parameterizedTypeReference, null);
    }

    protected <T> T putCall(String url, Map<String, Object> urlParameterMap, Class<T> resultType) throws JsonProcessingException {
        return restCall(url, HttpMethod.PATCH, null, urlParameterMap, null, null,null, resultType);
    }

    protected <T> T putCall(String url, Map<String, Object> urlParameterMap, Map<String, String> pathParameterMap, Map<String, String> headerMap, ParameterizedTypeReference<T> parameterizedTypeReference) throws JsonProcessingException {
        return restCall(url, HttpMethod.PATCH, null, urlParameterMap, pathParameterMap, headerMap, parameterizedTypeReference, null);
    }

    protected <T> T putCall(String url, Map<String, Object> urlParameterMap, Map<String, String> pathParameterMap, Map<String, String> headerMap, Class<T> resultType) throws JsonProcessingException {
        return restCall(url, HttpMethod.PATCH, null, urlParameterMap, pathParameterMap, headerMap,null, resultType);
    }

    /** DELETE CALL **/
    protected <T> T deleteCall(String url, Map<String, Object> urlParameterMap, ParameterizedTypeReference<T> parameterizedTypeReference) throws JsonProcessingException {
        return restCall(url, HttpMethod.PATCH, null, urlParameterMap, null, null, parameterizedTypeReference, null);
    }

    protected <T> T deleteCall(String url, Map<String, Object> urlParameterMap, Class<T> resultType) throws JsonProcessingException {
        return restCall(url, HttpMethod.PATCH, null, urlParameterMap, null, null,null, resultType);
    }

    protected <T> T deleteCall(String url, Map<String, Object> urlParameterMap, Map<String, String> pathParameterMap, Map<String, String> headerMap, ParameterizedTypeReference<T> parameterizedTypeReference) throws JsonProcessingException {
        return restCall(url, HttpMethod.PATCH, null, urlParameterMap, pathParameterMap, headerMap, parameterizedTypeReference, null);
    }

    protected <T> T deleteCall(String url, Map<String, Object> urlParameterMap, Map<String, String> pathParameterMap, Map<String, String> headerMap, Class<T> resultType) throws JsonProcessingException {
        return restCall(url, HttpMethod.PATCH, null, urlParameterMap, pathParameterMap, headerMap,null, resultType);
    }

    private <T> T restCall(String url, HttpMethod method, Object bodyContent, Map<String, Object> urlParameterMap, Map<String, String> pathParameterMap, Map<String, String> headerMap, ParameterizedTypeReference<T> parameterizedTypeReference, Class<T> resultType) throws JsonProcessingException {

//        RestClient.Builder restClientBuilder = RestClient.builder();

        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

        String endpoint = url;

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
//        restClientBuilder.defaultHeader("Content-Type", "application/json");

        if (headerMap != null) {
            headerMap.forEach(headers::add);
//            restClientBuilder.defaultHeaders(httpHeaders -> headerMap.forEach(httpHeaders::add));
        }

        HttpEntity<String> entity;
        if (bodyContent != null) {
            entity = new HttpEntity<>(mapper.writeValueAsString(bodyContent), headers);
        } else {
            entity = new HttpEntity<>(headers);
        }

        List<String> urlParameterList = null;
        if (urlParameterMap != null && !urlParameterMap.isEmpty()) {
            urlParameterList = urlParameterMap
                    .entrySet()
                    .stream()
                    .map(entry -> urlParameterFunction.apply(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
//            restClientBuilder.defaultUriVariables(urlParameterMap);
        }

        if (pathParameterMap != null && !pathParameterMap.isEmpty()) {
            for (Map.Entry<String, String> entry : pathParameterMap.entrySet()) {
                endpoint = endpoint.replace(pathVariableFunction.apply(entry.getKey()), entry.getValue());
            }
        }
//        restClientBuilder.baseUrl(endpoint);

        endpoint = normalizeUrlFunction.apply(endpoint, urlParameterList);

        if (parameterizedTypeReference != null) {
            return restTemplate.exchange(endpoint, method, entity, parameterizedTypeReference).getBody();
        }

        return restTemplate.exchange(endpoint, method, entity, resultType).getBody();
    }
}
