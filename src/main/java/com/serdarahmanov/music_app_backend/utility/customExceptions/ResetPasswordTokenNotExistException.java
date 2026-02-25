package com.serdarahmanov.music_app_backend.utility.customExceptions;

public class ResetPasswordTokenNotExistException extends RuntimeException{
    public ResetPasswordTokenNotExistException(String message) {
        super(message);
    }
}
