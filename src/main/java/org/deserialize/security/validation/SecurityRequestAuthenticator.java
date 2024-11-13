package com.enel.eic.commons.security.validation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;

public interface SecurityRequestAuthenticator {

    boolean authenticate(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception;
    String unauthorizedMessage();
}
