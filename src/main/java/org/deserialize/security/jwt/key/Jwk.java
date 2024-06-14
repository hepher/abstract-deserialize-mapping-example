package com.enelx.bfw.framework.security.jwt.key;

import com.enelx.bfw.framework.entity.AbstractEntity;
import com.enelx.bfw.framework.security.jwt.JwtKeys;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

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

    @JsonIgnore
    private Function<BigInteger, byte[]> parseBigIntFunction = bigInt -> {
        // Copied from Apache Commons Codec 1.8

        int bitlen = bigInt.bitLength();

        // round bitlen
        bitlen = ((bitlen + 7) >> 3) << 3;
        final byte[] bigBytes = bigInt.toByteArray();

        if (((bigInt.bitLength() % 8) != 0) && (((bigInt.bitLength() / 8) + 1) == (bitlen / 8))) {

            return bigBytes;

        }

        // set up params for copying everything but sign bit
        int startSrc = 0;
        int len = bigBytes.length;

        // if bigInt is exactly byte-aligned, just skip signbit in copy
        if ((bigInt.bitLength() % 8) == 0) {

            startSrc = 1;
            len--;
        }

        final int startDst = bitlen / 8 - len; // to pad w/ nulls as per spec
        final byte[] resizedBytes = new byte[bitlen / 8];
        System.arraycopy(bigBytes, startSrc, resizedBytes, startDst, len);
        return resizedBytes;
    };

    public Jwk() {}

    public Jwk(RSAPublicKey rsaPublicKey) {
        algorithm = "RS256";
        keyType = rsaPublicKey.getAlgorithm();
        modulus = Base64.getUrlEncoder().withoutPadding().encodeToString(parseBigIntFunction.apply(rsaPublicKey.getModulus()));
        exponent = Base64.getUrlEncoder().withoutPadding().encodeToString(parseBigIntFunction.apply(rsaPublicKey.getPublicExponent()));
    }
}
