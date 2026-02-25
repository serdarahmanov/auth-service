package com.serdarahmanov.music_app_backend.auth.forcodex.entity;

import com.serdarahmanov.music_app_backend.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "oauth2_auth_code")
@Getter
@Setter
@NoArgsConstructor
public class Oauth2AuthorizationCode extends AbstractEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    public Oauth2AuthorizationCode(String username, long ttlSeconds) {
        this.code = UUID.randomUUID().toString();
        this.username = username;
        this.expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}
