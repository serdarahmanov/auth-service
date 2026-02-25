package com.serdarahmanov.music_app_backend.utility.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.oauth2")
@Validated
@Getter
@Setter
public class AppOauth2Properties {

    // TTL for temporary OAuth2 authorization codes created by this service.
    @Min(1)
    private long authorizationCodeTtlSeconds = 3600;

    // Kept for scheduled cleanup placeholders in YAML.
    @Min(1)
    private long cleanupIntervalMs = 3_600_000;

    // Kept for scheduled cleanup placeholders in YAML.
    @Min(1)
    private long cleanupInitialDelayMs = 60_000;
}
