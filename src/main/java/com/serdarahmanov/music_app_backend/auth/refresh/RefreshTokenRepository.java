package com.serdarahmanov.music_app_backend.auth.refresh;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByFamilyIdAndRevokedAtIsNull(String familyId);

    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);

    List<RefreshToken> findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId,
            LocalDateTime now
    );

    long deleteByExpiresAtBefore(LocalDateTime cutoff);

    long deleteByRevokedAtBefore(LocalDateTime cutoff);
}
