package com.serdarahmanov.music_app_backend.auth.verification;

import com.serdarahmanov.music_app_backend.entity.AbstractEntity;
import com.serdarahmanov.music_app_backend.users.Users;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@NoArgsConstructor
@Getter
public class VerificationCode extends AbstractEntity {
    private String code;
    @Setter
    private boolean isEmailSent = false;
    @Setter
    private boolean used = false;
    private LocalDateTime expiresAt;

    @OneToOne
    private Users user;

    public VerificationCode(Users user){
        this.user = user;
        this.code =  UUID.randomUUID().toString();
        this.expiresAt = LocalDateTime.now().plusMinutes(20);
    }
}
