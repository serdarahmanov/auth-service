package com.serdarahmanov.music_app_backend.auth.forcodex.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Oauth2ExchangeRequest {

    @NotBlank
    private String code;
}
