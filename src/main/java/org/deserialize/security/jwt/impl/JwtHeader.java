package com.enelx.bfw.framework.security.jwt.impl;

import com.enelx.bfw.framework.security.jwt.JwtKeys;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JwtHeader {

    @JsonProperty(JwtKeys.HeaderClaim.ALGORITHM)
    private String algorithm;

    @JsonProperty(JwtKeys.HeaderClaim.TOKEN_TYPE)
    private String tokenType;

    @JsonProperty(JwtKeys.HeaderClaim.X509_FINGERPRINT)
    private String x509FingerPrint;

    @JsonProperty(JwtKeys.HeaderClaim.KEY_ID)
    private String keyId;

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
