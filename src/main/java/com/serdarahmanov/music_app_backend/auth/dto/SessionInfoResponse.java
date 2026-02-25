package com.serdarahmanov.music_app_backend.auth.dto;

import java.time.LocalDateTime;

public record SessionInfoResponse(
        Long id,
        String familyId,
        String userAgent,
        String ipAddress,
        LocalDateTime createdAt,
        LocalDateTime lastUsedAt,
        LocalDateTime expiresAt
) {
}
