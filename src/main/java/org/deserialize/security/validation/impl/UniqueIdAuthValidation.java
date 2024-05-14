package com.enelx.bfw.framework.security.validation.impl;

import com.enelx.bfw.framework.resolver.ExwUniqueId;
import com.enelx.bfw.framework.resolver.MasheryUniqueId;
import com.enelx.bfw.framework.security.validation.AbstractAuthValidation;
import com.enelx.bfw.framework.util.RequestParameterUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.HandlerMethod;

import java.io.IOException;

public class UniqueIdAuthValidation implements AbstractAuthValidation {

    @Override
    public boolean authenticate(HttpServletRequest request, HandlerMethod handlerMethod) throws IOException {
        ExwUniqueId exwUniqueIdAnnotation = null;
        MasheryUniqueId masheryUniqueIdAnnotation = null;

        for (MethodParameter methodParameter : handlerMethod.getMethodParameters()) {
            if (methodParameter.hasParameterAnnotation(ExwUniqueId.class)) {
                exwUniqueIdAnnotation = methodParameter.getParameterAnnotation(ExwUniqueId.class);
            }
            if (methodParameter.hasParameterAnnotation(MasheryUniqueId.class)) {
                masheryUniqueIdAnnotation = methodParameter.getParameterAnnotation(MasheryUniqueId.class);
            }
        }

        if (exwUniqueIdAnnotation == null || masheryUniqueIdAnnotation == null) {
            return false;
        }

        String uniqueId = (String) RequestParameterUtils.getValueFromRequest(request, masheryUniqueIdAnnotation.type(), masheryUniqueIdAnnotation.value());
        String exwUniqueId = (String) RequestParameterUtils.getValueFromRequest(request, exwUniqueIdAnnotation.type(), exwUniqueIdAnnotation.value());

        if (StringUtils.isBlank(exwUniqueId) || StringUtils.isBlank(uniqueId)) {
            return false;
        }

        return StringUtils.equals(uniqueId, exwUniqueId);
    }

    @Override
    public String unauthorizedMessage() {
        return "Forbidden: Request User not recognized";
    }
}
