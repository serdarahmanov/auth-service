package com.serdarahmanov.music_app_backend.auth.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record AdminReplaceUserRolesRequest(
        @NotEmpty(message = "Roles are required")
        Set<String> roles
) {
}
