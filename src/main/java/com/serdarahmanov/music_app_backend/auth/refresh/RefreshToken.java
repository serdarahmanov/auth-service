package com.serdarahmanov.music_app_backend.auth.refresh;

import com.serdarahmanov.music_app_backend.entity.AbstractEntity;
import com.serdarahmanov.music_app_backend.auth.identity.Users;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class RefreshToken extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(nullable = false, length = 64)
    private String familyId;

    @Setter
    private Long replacedById;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Setter
    private LocalDateTime revokedAt;

    @Setter
    private LocalDateTime lastUsedAt;

    @Column(length = 512)
    private String userAgent;

    @Column(length = 64)
    private String ipAddress;

    public RefreshToken(
            Users user,
            String tokenHash,
            String familyId,
            LocalDateTime expiresAt,
            String userAgent,
            String ipAddress
    ) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
