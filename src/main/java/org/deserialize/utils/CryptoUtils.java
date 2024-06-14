public class CryptoUtils {

    private CryptoUtils() {

    }

    private static final String ALGORITHM  = "HmacSHA256";
    private static final String ALG_RSA = "RSA";

    private static final String PUBLIC_KEY_BEGIN_PREFIX = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_KEY_END_PREFIX = "-----END PUBLIC KEY-----";
    private static final String PRIVATE_KEY_BEGIN_PREFIX = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_KEY_END_PREFIX = "-----END PRIVATE KEY-----";

    private static final Function<byte[], String> encode = b -> Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(b);

    public static synchronized PrivateKey generatePrivateKeyFromString(String privateKeyAsString) {

        try {
            privateKeyAsString = privateKeyAsString
                    .replace(PRIVATE_KEY_BEGIN_PREFIX, "")
                    .replaceAll("\n", "")
                    .replace(PRIVATE_KEY_END_PREFIX, "");

            KeyFactory kf = KeyFactory.getInstance(ALG_RSA);
            PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyAsString));
            return kf.generatePrivate(keySpecPKCS8);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            throw new BfwException(e.getMessage());
        }
    }

    public static synchronized PublicKey generatePublicKeyFromString(String publicKeyAsString) {
        try {
            publicKeyAsString = publicKeyAsString
                    .replace(PUBLIC_KEY_BEGIN_PREFIX, "")
                    .replaceAll("\n", "")
                    .replace(PUBLIC_KEY_END_PREFIX, "");

            KeyFactory kf = KeyFactory.getInstance(ALG_RSA);
            X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyAsString));
            return kf.generatePublic(keySpecX509);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new BfwException(e.getMessage());
        }
    }

    @Deprecated
    public static synchronized String encrypt(String input, String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(), ALGORITHM);
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return encode.apply(mac.doFinal(input.getBytes()));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new BfwException(ResponseMessage.ENCRYPTION_ERROR);
        }
    }

    @Deprecated
    public static synchronized Boolean verify(String input, String hashed, String secret) {
        String newHashed = encrypt(input, secret);
        return newHashed.equals(hashed);
    }

    public static void main(String[] args) throws Exception {
        String jwtHeader = "{\"typ\":\"JWT\",\"alg\":\"RS256\",\"kid\":\"NEMyMEFCMzUwMTE1QTNBOUFDMEQ1ODczRjk5NzBGQzY4QTk1Q0ZEOQ\"}";

        JwtHeader header = new JwtHeader();
        header.setAlgorithm("RS256");
        header.setTokenType("JWT");
        header.setKeyId("NEMyMEFCMzUwMTE1QTNBOUFDMEQ1ODczRjk5NzBGQzY4QTk1Q0ZEOQ");

        CompletedJwtPayload payload = new CompletedJwtPayload();
        payload.setUniqueId(UUID.randomUUID().toString());
        payload.setEmail("tinni.cris@gmail.com");
        payload.setCountry("IT");
        payload.setLanguage("it_IT");
        payload.setDeviceId(UUID.randomUUID().toString());
        payload.setFirstname("Cristopher");
        payload.setLastname("Tinnirello");
        payload.setSubject("ctinnirello");
        payload.setIssuer("b4f");
        payload.setIssuedAt(new Date().getTime());
        payload.setExpirationTime(new Date().getTime());

        CompletedJwt jwt = new CompletedJwt(header, payload);
        String message = jwt.getEncodedHeader() + "." + jwt.getEncodedPayload();

        // Key generation
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair key = keyGen.generateKeyPair();

        // sign
        Signature sig = Signature.getInstance("SHA256WithRSA");
        System.out.println("Private Key: " + Base64.getEncoder().encodeToString(key.getPrivate().getEncoded()));
        sig.initSign(key.getPrivate());
        sig.update(message.getBytes(StandardCharsets.UTF_8));
        byte[] signature = sig.sign();

        // verification
        System.out.println("Public Key: " + Base64.getEncoder().encodeToString(key.getPublic().getEncoded()));
        sig.initVerify(key.getPublic());
        sig.update(message.getBytes(StandardCharsets.UTF_8));
        boolean result = sig.verify(signature);

        // result
        System.out.println("Message   = " + message);
        System.out.println("Signature = " + Base64.getEncoder().encodeToString(signature));
        System.out.println("Verification Result = " + result);

        // crypt/decrypt
        Function<String, byte[]> shaFunction = (mes) -> {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-512");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            digest.update(message.getBytes(StandardCharsets.UTF_8));
            return digest.digest();
        };

        // sign
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key.getPrivate());
        byte[] messageHash = shaFunction.apply(message);
        signature = cipher.doFinal(messageHash);

        // verification
        cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key.getPublic());
        byte[] decryptedMessageHash = cipher.doFinal(signature);
        messageHash = shaFunction.apply(message);
        result = Arrays.equals(decryptedMessageHash, messageHash);

        System.out.println("Message   = " + message);
        System.out.println("Signature = "
                + Base64.getEncoder().encodeToString(signature));
        System.out.println("Verification Result = " + result);
    }
