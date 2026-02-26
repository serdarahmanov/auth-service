package com.serdarahmanov.music_app_backend.auth.dto;
import com.serdarahmanov.music_app_backend.utility.validators.PasswordMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@PasswordMatch(
        passwordField = "password",
        passwordConfirmationField = "passwordConfirmation",
        message = "Passwords do not match"
)
public class CreateUserRequest {
//DTO = Data Transfer Object.

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String passwordConfirmation;

    public CreateUserRequest(
            String email,
            String username,
            String password,
            String passwordConfirmation
    ) {
        this.email = email;
        this.username=username;
        this.password = password;
        this.passwordConfirmation = passwordConfirmation;
    }


}
