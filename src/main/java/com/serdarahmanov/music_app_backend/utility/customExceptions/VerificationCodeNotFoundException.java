package com.serdarahmanov.music_app_backend.utility.customExceptions;



public class VerificationCodeNotFoundException extends RuntimeException{
    public VerificationCodeNotFoundException(String message) {
        super(message);
    }
}
