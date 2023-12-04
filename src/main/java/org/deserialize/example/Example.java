public class Example {

  public RestUserResponse findUserByUniqueId(String uniqueId, String transactionId) {
        String url = "http://usrmng-test:8080/v1/user/{uniqueId}";

        MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
//        headerMap.put("X-UniqueID", Collections.singletonList(uniqueId));
        headerMap.put("trace-id", Collections.singletonList(transactionId));

        Map<String, String> pathParameterMap = new HashMap<>();
        pathParameterMap.put("uniqueId", uniqueId);

        Map<String, Object> urlParameterMap = new HashMap<>();
//        urlParameterMap.put("transactionId", transactionId.replace("-", "+").concat("@"));
        return RestClientService.instance()
                .url(url)
                .method(HttpMethod.GET)
                .pathParameters(pathParameterMap)
                .queryParameters(urlParameterMap)
                .headers(headerMap)
                .resultClass(RestUserResponse.class)
                .errorResponseConsumer(HttpStatus.FORBIDDEN, RestClientService.instanceHttpStatusConsumer((restClientService, result) -> {
                    restClientService.header("X-UniqueID", Collections.singletonList(uniqueId));
                    return true;
                }))
                .errorResponseConsumer(HttpStatus.INTERNAL_SERVER_ERROR, RestClientService.instanceHttpStatusConsumer((restClientService, result) -> {
                    restClientService.header("trace-id", Collections.singletonList(UUID.randomUUID().toString()));
                    return true;
                }))
                .parseErrorBodyResponse((bfwException, result) -> {
                    try {
                        UserManagerErrorResponse errorResponse = mapper.readValue(result, UserManagerErrorResponse.class);
                        bfwException.setErrorCode(errorResponse.getCode() + "");
                        bfwException.setSystemErrorResponse(errorResponse);
                    } catch (JsonProcessingException e) {
//                        throw new RuntimeException(e);
                    }
                })
                .exchange();
    }
}
