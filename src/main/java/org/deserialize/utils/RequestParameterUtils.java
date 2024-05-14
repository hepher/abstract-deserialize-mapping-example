package com.enelx.bfw.framework.util;

import com.enelx.bfw.framework.resolver.ParameterType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

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
                    JsonNode body = mapper.readTree(StreamUtils.copyToString(request.getInputStream(), Charset.defaultCharset()));
                    if (body != null) {
                        JsonNode parameterNode = body.findValue(parameterName);
                        if (parameterNode != null && !parameterNode.isNull()) {
                            yield parameterNode.asText();
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
}
