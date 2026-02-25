package com.serdarahmanov.music_app_backend.auth.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter

public class UserNameAndPassword {
    @NotBlank(message = "User name is required")
    private String username;
    @NotBlank(message = "Password is required")
    private String password;

    public UserNameAndPassword(String username, String password) {


        this.username = username;

        this.password = password;
    }
}
