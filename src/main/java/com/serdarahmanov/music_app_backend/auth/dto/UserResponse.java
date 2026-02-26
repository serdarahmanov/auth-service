package com.serdarahmanov.music_app_backend.auth.dto;

import com.serdarahmanov.music_app_backend.auth.identity.Users;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String userName;
    private boolean enabled;
    private boolean passwordSet;
    private Set<String> role;

    public static UserResponse fromEntity(Users user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .userName(user.getUsername())
                .enabled(user.isEnabled())
                .passwordSet(user.isPasswordSet())
                .role(user.getRolesAsString())
                .build();
    }
}
