package com.serdarahmanov.music_app_backend.utility.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.refresh")
@Validated
@Getter
@Setter
public class AppRefreshProperties {

    @Min(1)
    private long cleanupIntervalMs = 3_600_000;

    @Min(1)
    private long cleanupInitialDelayMs = 60_000;

    @Min(1)
    private long cleanupRetentionMs = 604_800_000;
}
