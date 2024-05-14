package com.enelx.bfw.framework.security.jwt;

import com.enelx.bfw.framework.exception.BfwException;
import com.enelx.bfw.framework.security.jwt.impl.JwtHeader;
import com.enelx.bfw.framework.security.jwt.impl.JwtPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.function.Supplier;

@Getter
public abstract class Jwt<H extends JwtHeader, P extends JwtPayload> {

    private static final String RSA_ALG = "SHA256withRSA";

    private static final Base64.Decoder decoder = Base64.getUrlDecoder();
    private static final Base64.Encoder encoder = Base64.getUrlEncoder();
    private static final ObjectMapper mapper = new ObjectMapper();

    private String encodedHeader;
    private String encodedPayload;
    private String signature;

    private H header;
    private P payload;

    protected Supplier<byte[]> computeSignedContentFunction = () -> {
        if (encodedHeader == null || encodedPayload == null) {
            return null;
        }
        return encodedHeader.concat(".").concat(encodedPayload).getBytes(StandardCharsets.UTF_8);
    };

    protected Jwt(H header, P payload) throws JsonProcessingException {
        setHeader(header);
        setPayload(payload);
    }

    protected Jwt(String jwt) throws IOException {
        String[] jwtParts;
        if (jwt == null || (jwtParts = jwt.split("\\.")).length != 3) {
            throw new BfwException(String.format("Invalid jwt input: %s", jwt));
        }

        // jwt encoded value
        encodedHeader = jwtParts[0];
        encodedPayload = jwtParts[1];
        signature = jwtParts[2];

        // header and value object
        ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
        header = mapper.readValue(decoder.decode(encodedHeader), (Class<H>) genericSuperclass.getActualTypeArguments()[0]);
        payload = mapper.readValue(decoder.decode(encodedPayload), (Class<P>) genericSuperclass.getActualTypeArguments()[1]);
    }

    public void setHeader(H header) throws JsonProcessingException {
        this.header = header;

        if (header != null) {
            encodedHeader = encoder.withoutPadding().encodeToString(mapper.writeValueAsString(header).getBytes(StandardCharsets.UTF_8));
        } else {
            encodedHeader = null;
        }
    }

    public void setPayload(P payload) throws JsonProcessingException {
        this.payload = payload;

        if (payload != null)  {
            encodedPayload = encoder.withoutPadding().encodeToString(mapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));
        } else {
            encodedPayload = null;
        }
    }

    public synchronized void sign(PrivateKey privateKey) {
        if (header == null || payload == null) {
            throw new BfwException("Missing header/payload to sign jwt");
        }

        try {
            final Signature sig = Signature.getInstance(RSA_ALG);
            sig.initSign(privateKey);
            sig.update(computeSignedContentFunction.get());

            signature = encoder.withoutPadding().encodeToString(sig.sign());
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new BfwException(e.getMessage());
        }
    }

    public boolean verify(PublicKey publicKey) {
        if (header == null || payload == null || signature == null) {
            throw new BfwException("Incomplete jwt");
        }

        try {
            Signature sig = Signature.getInstance(RSA_ALG);
            sig.initVerify(publicKey);
            sig.update(computeSignedContentFunction.get());

            return sig.verify(decoder.decode(signature));
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new BfwException(e.getMessage());
        }
    }

    @Override
    public String toString() {
        return encodedPayload;
    }

    public String asString() {
        return encodedHeader + "." + encodedPayload + "." + signature;
    }
}
