package com.serdarahmanov.music_app_backend.utility.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.login-protection")
@Validated
@Getter
@Setter
public class AppLoginProtectionProperties {

    @Min(1)
    private int maxFailedAttempts = 5;

    @Min(1)
    private long lockDurationMs = 900_000;
}
