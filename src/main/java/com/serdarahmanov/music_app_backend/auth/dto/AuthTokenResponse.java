package com.serdarahmanov.music_app_backend.auth.dto;

public record AuthTokenResponse(
        String accessToken,
        String accessTokenType,
        long accessTokenExpiresInMs,
        String refreshToken,
        long refreshTokenExpiresInMs
) {
}
