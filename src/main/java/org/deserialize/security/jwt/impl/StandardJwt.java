package com.enelx.bfw.framework.security.jwt.impl;

import com.enelx.bfw.framework.security.jwt.Jwt;

import java.io.IOException;

public class StandardJwt extends Jwt<JwtHeader, JwtPayload> {

    public StandardJwt(String jwt) throws IOException {
        super(jwt);
    }
}
