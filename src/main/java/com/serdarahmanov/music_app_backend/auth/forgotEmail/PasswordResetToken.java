package com.serdarahmanov.music_app_backend.auth.forgotEmail;

import com.serdarahmanov.music_app_backend.entity.AbstractEntity;
import com.serdarahmanov.music_app_backend.users.Users;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class PasswordResetToken extends AbstractEntity {

    private String code;

    private boolean emailSent = false;

    private boolean used = false;

    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    public PasswordResetToken(Users user) {
        this.user = user;
        this.code = UUID.randomUUID().toString();
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public void onEmailSent(){
        this.setEmailSent(true);
        this.expiresAt = LocalDateTime.now().plusMinutes(20);
    }
}
