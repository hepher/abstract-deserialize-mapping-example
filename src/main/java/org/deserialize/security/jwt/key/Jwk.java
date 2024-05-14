package com.enelx.bfw.framework.security.jwt.key;

import com.enelx.bfw.framework.entity.AbstractEntity;
import com.enelx.bfw.framework.security.jwt.JwtKeys;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;

@Getter
@Setter
public class Jwk extends AbstractEntity {

    @JsonProperty(JwtKeys.Parameter.ALGORITHM)
    private String algorithm;

    @JsonProperty(JwtKeys.Parameter.PUBLIC_KEY_USE)
    private String usage;

    @JsonProperty(JwtKeys.Parameter.KEY_TYPE)
    private String keyType;

    @JsonProperty(JwtKeys.Parameter.KEY_ID)
    private String keyId;

    @JsonProperty(JwtKeys.Parameter.RSA_PUBLIC_KEY_MODULUS)
    private String modulus;

    @JsonProperty(JwtKeys.Parameter.RSA_PUBLIC_KEY_EXPONENT)
    private String exponent;

    @JsonProperty(JwtKeys.Parameter.X509_CERT_TUMBLING)
    private String x509CertTumbling;

    @JsonProperty(JwtKeys.Parameter.X509_CERT_CHAIN)
    private List<String> x509CertList;

    public Jwk() {}

    public Jwk(RSAPublicKey rsaPublicKey) {
        algorithm = "RS256";
        keyType = rsaPublicKey.getAlgorithm();
        modulus = Base64.getUrlEncoder().encodeToString(rsaPublicKey.getModulus().toByteArray());
        exponent = Base64.getUrlEncoder().encodeToString(rsaPublicKey.getPublicExponent().toByteArray());
    }
}
