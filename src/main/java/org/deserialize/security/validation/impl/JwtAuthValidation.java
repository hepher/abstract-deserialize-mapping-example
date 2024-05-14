package com.enelx.bfw.framework.security.validation.impl;

import com.enelx.bfw.framework.resolver.Jwt;
import com.enelx.bfw.framework.resolver.MasheryUniqueId;
import com.enelx.bfw.framework.security.jwt.impl.StandardJwt;
import com.enelx.bfw.framework.security.validation.AbstractAuthValidation;
import com.enelx.bfw.framework.util.RequestParameterUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.HandlerMethod;

import java.io.IOException;

public class JwtAuthValidation implements AbstractAuthValidation {

    @Override
    public boolean authenticate(HttpServletRequest request, HandlerMethod handlerMethod) throws IOException {
        Jwt jwtAnnotation = null;
        MasheryUniqueId masheryUniqueIdAnnotation = null;

        for (MethodParameter methodParameter : handlerMethod.getMethodParameters()) {
            if (methodParameter.hasParameterAnnotation(Jwt.class)) {
                jwtAnnotation = methodParameter.getParameterAnnotation(Jwt.class);
            }
            if (methodParameter.hasParameterAnnotation(MasheryUniqueId.class)) {
                masheryUniqueIdAnnotation = methodParameter.getParameterAnnotation(MasheryUniqueId.class);
            }
        }

        if (jwtAnnotation == null || masheryUniqueIdAnnotation == null) {
            return false;
        }

        String uniqueId = (String) RequestParameterUtils.getValueFromRequest(request, masheryUniqueIdAnnotation.type(), masheryUniqueIdAnnotation.value());
        String encodedJwt = (String) RequestParameterUtils.getValueFromRequest(request, jwtAnnotation.type(), jwtAnnotation.value());

        if (StringUtils.isBlank(encodedJwt) || StringUtils.isBlank(uniqueId)) {
            return false;
        }

        StandardJwt jwt = new StandardJwt(encodedJwt);

        return StringUtils.equals(uniqueId, jwt.getPayload().getClaim("uniqueid").toString());
    }

    @Override
    public String unauthorizedMessage() {
        return "Missing or invalid jwt or uniqueID";
    }
}
