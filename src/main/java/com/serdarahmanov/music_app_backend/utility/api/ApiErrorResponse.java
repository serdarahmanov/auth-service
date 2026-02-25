package com.serdarahmanov.music_app_backend.utility.api;

public record ApiErrorResponse(
        String code,
        String message,
        String traceId
) {
}
