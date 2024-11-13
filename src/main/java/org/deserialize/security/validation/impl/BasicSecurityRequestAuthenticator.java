
package com.enel.eic.commons.security.validation.impl;

import com.enel.eic.commons.constant.HeaderConstants;
import com.enel.eic.commons.security.validation.SecurityRequestAuthenticator;
import com.enel.eic.commons.util.ApplicationContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class BasicSecurityRequestAuthenticator implements SecurityRequestAuthenticator {

    @Override
    public boolean authenticate(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

        String authorizationHeader = request.getHeader(HeaderConstants.AUTHORIZATION);
        if (StringUtils.isBlank(authorizationHeader)) {
            log.info("Missing authorization header");
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic");
            return false;
        }

        String encodedBasic = authorizationHeader.replaceFirst("^Basic ", "");

        if (StringUtils.isBlank(encodedBasic)) {
            log.info("Missing basic credential");
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic");
            return false;
        }

        String clientId;
        String clientSecret;
        try {
            clientId = String.valueOf(ApplicationContextUtils.evalExpression("${commons.security.basic.client-id}"));
            clientSecret = String.valueOf(ApplicationContextUtils.evalExpression("${commons.security.basic.client-secret}"));
        } catch (Exception exception) {
            log.error(ApplicationContextUtils.getExceptionMessage(exception));
            return false;
        }

        if (StringUtils.isBlank(clientId) || StringUtils.isBlank(clientSecret)) {
            log.info("Missing client id and/or client secret in configuration file");
            return false;
        }

        return encodedBasic.equals(Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String unauthorizedMessage() {
        return "HTTP Status 401: Missing basic auth";
    }
}
