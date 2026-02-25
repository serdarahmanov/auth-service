package com.serdarahmanov.music_app_backend.auth.refresh.exceptions;

public class RefreshTokenReuseDetectedException extends RuntimeException {
    public RefreshTokenReuseDetectedException(String message) {
        super(message);
    }
}
