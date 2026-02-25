package com.serdarahmanov.music_app_backend.auth.refresh.exceptions;

public class RefreshTokenOwnershipException extends RuntimeException {
    public RefreshTokenOwnershipException(String message) {
        super(message);
    }
}
