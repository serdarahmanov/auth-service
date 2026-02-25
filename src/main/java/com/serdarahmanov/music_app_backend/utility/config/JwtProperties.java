package com.serdarahmanov.music_app_backend.utility.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jwt")
@Validated
@Getter
@Setter
public class JwtProperties {

    // Access token validity in milliseconds.
    @Min(1)
    private long expiration;

    // Refresh token validity in milliseconds.
    @Min(1)
    private long refreshExpiration;

    // Issuer claim included in JWTs and checked during validation.
    private String issuer = "music-app-backend";

    // Key id exposed in JWKS and written into JWT header as "kid".
    private String keyId = "dev-rsa-key-1";

    // Optional PEM private key (PKCS#8). If missing with public key, app generates a dev keypair at startup.
    private String rsaPrivateKeyPem;

    // Optional PEM public key (X.509). Must be provided together with private key.
    private String rsaPublicKeyPem;

    // Allows dev-mode fallback keypair generation when PEM keys are not configured.
    private boolean allowEphemeralKeys = true;
}
