package com.serdarahmanov.music_app_backend.auth.forgotEmail;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ForgotEmailRequest {

    @Email
    private String email;

}
