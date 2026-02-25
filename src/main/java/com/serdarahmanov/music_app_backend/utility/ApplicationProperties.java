package com.serdarahmanov.music_app_backend.utility;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "application")
@Validated
@Getter
@Setter
public class ApplicationProperties {
    @NotBlank
    private String baseUrl;
    @NotBlank
    private String applicationName;
    @NotBlank
    private String loginPageUrl;
    @NotBlank
    private String resetPasswordUrl;
    @NotBlank
    private String frontendResetPasswordUrl;
    @NotBlank
    private String loginSuccessUrl;
}
