package com.enelx.bfw.framework.security.jwt;

public interface JwtKeys {

    interface HeaderClaim {
        String ALGORITHM = "alg";
        String TOKEN_TYPE = "typ";
        String X509_FINGERPRINT = "x5t";
        String KEY_ID = "kid";
    }

    interface PayloadClaim {
        String ISSUER = "iss";
        String SUBJECT = "sub";
        String AUDIENCE = "aud";
        String EXPIRATION_TIME = "exp";
        String NOT_BEFORE = "nbf";
        String ISSUED_AT = "iat";
        String JWT_ID = "jti";
        String TOKEN_HASH_VALUE = "at_hash";
        String AUTHENTICATION_METHODS = "amr";
        String AUTHORIZED_PARTY = "azp";
    }

    interface Parameter {
        String KEY_TYPE = "kty";
        String PUBLIC_KEY_USE = "use"; // values: sig, enc
        String ALGORITHM = "alg";
        String KEY_ID = "kid";
        String RSA_PUBLIC_KEY_MODULUS = "n";
        String RSA_PUBLIC_KEY_EXPONENT = "e";
        String X509_CERT_CHAIN = "x5c";
        String X509_CERT_TUMBLING= "x5t";
    }
}
