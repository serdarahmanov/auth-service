package com.serdarahmanov.music_app_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminRoleAssignmentRequest(
        @NotBlank(message = "Role name is required")
        String roleName
) {
}
