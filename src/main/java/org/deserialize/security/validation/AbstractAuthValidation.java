package com.enelx.bfw.framework.security.validation;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.method.HandlerMethod;

import java.io.IOException;

public interface AbstractAuthValidation {

    boolean authenticate(HttpServletRequest request, HandlerMethod handlerMethod) throws Exception;
    String unauthorizedMessage();
}
