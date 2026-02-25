package com.serdarahmanov.music_app_backend.auth.dto;

import java.util.Set;

public record AdminUserRolesResponse(
        Long userId,
        Set<String> roles
) {
}
