package com.serdarahmanov.music_app_backend.auth.jwt;

import com.serdarahmanov.music_app_backend.utility.config.JwtProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtKeyProvider {

    private final JwtProperties jwtProperties;

    @Getter
    private PrivateKey privateKey;

    @Getter
    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        String privatePem = trimToNull(jwtProperties.getRsaPrivateKeyPem());
        String publicPem = trimToNull(jwtProperties.getRsaPublicKeyPem());

        try {
            if (privatePem == null && publicPem == null) {
                if (!jwtProperties.isAllowEphemeralKeys()) {
                    throw new IllegalStateException(
                            "JWT RSA keys are not configured and ephemeral keys are disabled. " +
                                    "Set JWT_RSA_PRIVATE_KEY_PEM and JWT_RSA_PUBLIC_KEY_PEM."
                    );
                }
                // Dev-friendly fallback: generated on each restart.
                KeyPair keyPair = generateRsaKeyPair();
                this.privateKey = keyPair.getPrivate();
                this.publicKey = keyPair.getPublic();
                log.warn("JWT RSA keys not configured. Generated an ephemeral development key pair.");
                return;
            }

            if (privatePem == null || publicPem == null) {
                throw new IllegalStateException("Both jwt.rsa-private-key-pem and jwt.rsa-public-key-pem must be set together.");
            }

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] privateKeyBytes = decodePem(privatePem, "PRIVATE KEY");
            byte[] publicKeyBytes = decodePem(publicPem, "PUBLIC KEY");

            this.privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            this.publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to initialize JWT RSA keys.", e);
        }
    }

    public String getCurrentKid() {
        return jwtProperties.getKeyId();
    }

    public PublicKey getVerificationKeyForKid(String kid) {
        if (kid == null || !jwtProperties.getKeyId().equals(kid)) {
            throw new IllegalArgumentException("Unknown JWT kid: " + kid);
        }
        return publicKey;
    }

    public Map<String, Object> getCurrentJwk() {
        if (!(publicKey instanceof RSAPublicKey rsaPublicKey)) {
            throw new IllegalStateException("Configured JWT public key is not RSA.");
        }

        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", jwtProperties.getKeyId());
        jwk.put("n", toBase64UrlUnsigned(rsaPublicKey.getModulus()));
        jwk.put("e", toBase64UrlUnsigned(rsaPublicKey.getPublicExponent()));
        return jwk;
    }

    private static KeyPair generateRsaKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static byte[] decodePem(String pem, String type) {
        String normalized = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(normalized);
    }

    private static String toBase64UrlUnsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
