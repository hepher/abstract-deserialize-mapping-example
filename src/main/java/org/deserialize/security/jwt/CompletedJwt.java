package com.enelx.bfw.framework.security.jwt.impl;

import com.enelx.bfw.framework.security.jwt.Jwt;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;

public class CompletedJwt extends Jwt<JwtHeader, CompletedJwtPayload> {

    public CompletedJwt(JwtHeader header, CompletedJwtPayload payload) throws JsonProcessingException {
        super(header, payload);
    }

    public CompletedJwt(String jwt) throws IOException {
        super(jwt);
    }
}
