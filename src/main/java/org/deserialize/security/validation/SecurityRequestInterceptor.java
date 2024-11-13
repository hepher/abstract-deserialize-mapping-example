package com.enel.eic.commons.security.validation;

import com.enel.eic.commons.exception.CommonsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

@Component
public class SecurityRequestInterceptor implements HandlerInterceptor {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object) throws Exception {
        if (!(object instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (!handlerMethod.hasMethodAnnotation(Authenticated.class)) {
            return true;
        }

        Authenticated methodAnnotation = handlerMethod.getMethodAnnotation(Authenticated.class);
        if (methodAnnotation == null) {
            return true;
        }

        SecurityRequestAuthenticator securityRequestAuthenticatorClass = methodAnnotation.validationClass().getDeclaredConstructor().newInstance();
        if (!securityRequestAuthenticatorClass.authenticate(request, response, handlerMethod)) {
            Map<String, Object> unauthorizedResponse = new HashMap<>();
            unauthorizedResponse.put("message", StringUtils.defaultIfBlank(methodAnnotation.unauthorizedMessage(), securityRequestAuthenticatorClass.unauthorizedMessage()));

            response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getOutputStream().print(mapper.writeValueAsString(unauthorizedResponse));

            return false;
        }

        return true;
    }

}
