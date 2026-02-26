package com.serdarahmanov.music_app_backend.auth.dto;

import java.util.Set;

public record AuthMeResponse(
        Long id,
        String username,
        String email,
        boolean enabled,
        boolean passwordSet,
        Set<String> roles
) {
}
