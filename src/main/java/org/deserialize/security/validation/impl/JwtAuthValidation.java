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
        EicUniqueId eicUniqueId = null;
        CacheManagerService cacheManagerService = ApplicationContextUtils.getBean(CacheManagerService.class);

        Function<String, String> openIdConfigFunction = (url) -> {
            if (url.endsWith("/")) {
                return url + OPEN_ID_CONFIGURATION;
            }

            return url + "/" + OPEN_ID_CONFIGURATION;
        };

        Function<UniqueIdJwt, Boolean> jwtDateValidationFunction = (jwt) ->
            jwt.getPayload().getNotValidBefore() * 1000 <= System.currentTimeMillis()
                    && jwt.getPayload().getExpirationTime() * 1000 >= System.currentTimeMillis()
                    && jwt.getPayload().getIssuedAt() * 1000 <= System.currentTimeMillis();


        String jwtHeader = request.getHeader(HeaderConstants.AUTHORIZATION).split(" ")[1];
        UniqueIdJwt jwt = new UniqueIdJwt(jwtHeader);

        final String issuer = jwt.getPayload().getIssuer();
        if (StringUtils.isBlank(issuer) || !StringUtils.startsWith(issuer, "http")) {
            return false;
        }

        Jwks jwks = cacheManagerService.find(CACHE_JWT_KEY, issuer, Jwks.class)
                .orElseGet(() -> {

                    // rest call to open id configuration
                    JsonNode node = RestClientService.instance()
                            .method(HttpMethod.GET)
                            .url(openIdConfigFunction.apply(issuer))
                            .resultClass(JsonNode.class)
                            .exchange();

                    if (node == null) {
                        log.error("error: missing endpoint to access open id configuration");
                        return null;
                    }

                    JsonNode jwksNode = node.findValue(OPEN_ID_JWKS_KEY);
                    if (jwksNode == null) {
                        log.error("error: missing jwks_uri on open id configuration endpoint");
                        return null;
                    }

                    String jwksUrl = jwksNode.asText();
                    if (StringUtils.isBlank(jwksUrl)) {
                        log.error("error: missing jwks_uri on open id configuration endpoint");
                        return null;
                    }

                    Jwks jwksResult = RestClientService.instance()
                            .method(HttpMethod.GET)
                            .url(jwksUrl)
                            .resultClass(Jwks.class)
                            .exchange();

                    cacheManagerService.put(CACHE_JWT_KEY, issuer, jwksResult, 86400000);

                    return jwksResult;
                });

        if (jwks == null) {
            log.error("error on find jwks");
            return false;
        }

        Jwk jwk = jwks.findJwkByKey(jwt.getHeader().getKeyId());

        if (jwk == null) {
            log.error("error on find jwk");
            return false;
        }

        PublicKey publicKey = CipherUtils.createPublicKey(jwk);

        if (publicKey == null) {
            log.error("Error on generate public key");
            return false;
        }

        if (!jwt.verify(publicKey)) {
            log.error("Error during uniqueIdJwt verification");
            return false;
        }

        if (!jwtDateValidationFunction.apply(jwt)) {
            log.error("Error during verifyJwtDate");
            return false;
        }

        for (MethodParameter methodParameter : handlerMethod.getMethodParameters()) {
            if (methodParameter.hasParameterAnnotation(EicUniqueId.class)) {
                eicUniqueId = methodParameter.getParameterAnnotation(EicUniqueId.class);
            }
        }

        if (eicUniqueId == null) {
            log.error("EicUniqueId is null");
            return false;
        }

        String uniqueId = (String) RequestParameterUtils.getValueFromRequest(request, eicUniqueId.type(), eicUniqueId.value());

        if (StringUtils.isBlank(jwt.getPayload().getUniqueId()) || StringUtils.isBlank(uniqueId)) {
            return false;
        }

        return StringUtils.equals(uniqueId, jwt.getPayload().getUniqueId());
    }

    @Override
    public String unauthorizedMessage() {
        return "Missing or invalid jwt or uniqueID";
    }
}
