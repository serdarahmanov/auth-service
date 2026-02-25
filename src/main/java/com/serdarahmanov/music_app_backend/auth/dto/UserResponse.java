package com.serdarahmanov.music_app_backend.auth.dto;

import com.serdarahmanov.music_app_backend.users.Users;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String userName;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String bio;
    private Set<String> role;

    public static UserResponse fromEntity(Users user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .userName(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarKey())
                .bio(user.getBio())
                .role(user.getRolesAsString())
                .build();
    }
}