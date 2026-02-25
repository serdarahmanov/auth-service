package com.serdarahmanov.music_app_backend.utility.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
@Validated
@Getter
@Setter
public class AppSecurityProperties {

    @NotEmpty
    private List<String> allowedOrigins = List.of("http://localhost:3000");
}
