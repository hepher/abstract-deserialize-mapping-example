package com.enelx.bfw.framework.security.jwt.impl;

import com.enelx.bfw.framework.security.jwt.JwtKeys;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class JwtPayload {

    private final static ObjectMapper mapper = new ObjectMapper();

    @JsonProperty(JwtKeys.PayloadClaim.ISSUER)
    protected String issuer;

    @JsonProperty(JwtKeys.PayloadClaim.SUBJECT)
    protected String subject;

    @JsonProperty(JwtKeys.PayloadClaim.AUDIENCE)
    protected List<String> audiences;

    @JsonProperty(JwtKeys.PayloadClaim.NOT_BEFORE)
    protected Long notValidBefore;

    @JsonProperty(JwtKeys.PayloadClaim.AUTHORIZED_PARTY)
    protected String authorizedParty;

    @JsonProperty(JwtKeys.PayloadClaim.EXPIRATION_TIME)
    protected Long expirationTime;

    @JsonProperty(JwtKeys.PayloadClaim.ISSUED_AT)
    protected Long issuedAt;

    @JsonProperty(JwtKeys.PayloadClaim.TOKEN_HASH_VALUE)
    protected String tokenHashValue;

    @JsonProperty(JwtKeys.PayloadClaim.AUTHENTICATION_METHODS)
    protected List<String> authenticationMethods;

    @JsonProperty(JwtKeys.PayloadClaim.JWT_ID)
    protected String tokenId;

    @JsonAnyGetter
    @JsonAnySetter
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    protected Map<String, Object> claims = new HashMap<>();

    public Object getClaim(String claimKey) {
        return claims.get(claimKey);
    }

    public void setClaim(String claimKey, Object claimValue) {
        if (claims == null) {
            claims = new HashMap<>();
        }

        claims.put(claimKey, claimValue);
    }
}
