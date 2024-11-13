package com.enel.eic.commons.util;


import com.enel.eic.commons.constant.HeaderConstants;
import com.enel.eic.commons.resolver.ParameterType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestParameterUtils {

    private final static ObjectMapper mapper = new ObjectMapper();

    public static Object getValueFromRequest(HttpServletRequest request, ParameterType parameterType, String parameterName) throws IOException {
        return switch (parameterType) {
            case HEADER -> {
                if (StringUtils.isNotBlank(request.getHeader(parameterName))) {
                    yield request.getHeader(parameterName);
                }
                yield null;
            }
            case BODY -> {
                if (request.getInputStream() != null) {
                    String bodyAsString = StreamUtils.copyToString(request.getInputStream(), Charset.defaultCharset());
                    try {
                        // parse body as json object
                        JsonNode body = mapper.readTree(bodyAsString);
                        if (body != null) {
                            JsonNode parameterNode = body.findValue(parameterName);
                            if (parameterNode != null && !parameterNode.isNull()) {
                                yield parameterNode.asText();
                            }
                        }
                    } catch (JsonProcessingException e) {
                        // parse body as url encoding
                        Matcher matcher = Pattern.compile("(\\w+)=(.*?)(?=,\\w+=|$)").matcher(bodyAsString);
                        while (matcher.find()) {
                            if (parameterName.equals(matcher.group(1))) {
                                yield matcher.group(2);
                            }
                        }

                    }
                }

                yield null;
            }
            case QUERY -> {
                if (StringUtils.isNotBlank(request.getParameter(parameterName))) {
                    yield request.getParameter(parameterName);
                }
                yield null;
            }
            case PATH -> {
                Map<String, String> pathVariableMap = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                if (pathVariableMap != null) {
                    yield pathVariableMap.get(parameterName);
                }
                yield null;
            }
        };
    }

    public static String getUserIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(HeaderConstants.X_FORWARDED_FOR);

        // Gestisci il caso in cui l'applicazione è dietro un proxy
        if (StringUtils.isNotBlank(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }

        return request.getRemoteAddr();
    }
}
