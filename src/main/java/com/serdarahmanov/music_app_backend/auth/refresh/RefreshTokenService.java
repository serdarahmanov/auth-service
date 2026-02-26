package com.serdarahmanov.music_app_backend.auth.refresh;

import com.serdarahmanov.music_app_backend.auth.dto.SessionInfoResponse;
import com.serdarahmanov.music_app_backend.auth.identity.Users;
import com.serdarahmanov.music_app_backend.auth.refresh.exceptions.RefreshTokenExpiredException;
import com.serdarahmanov.music_app_backend.auth.refresh.exceptions.RefreshTokenInvalidException;
import com.serdarahmanov.music_app_backend.auth.refresh.exceptions.RefreshTokenOwnershipException;
import com.serdarahmanov.music_app_backend.auth.refresh.exceptions.RefreshTokenReuseDetectedException;
import com.serdarahmanov.music_app_backend.utility.config.AppRefreshProperties;
import com.serdarahmanov.music_app_backend.utility.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final AppRefreshProperties appRefreshProperties;

    @Transactional
    public String issue(Users user, String userAgent, String ipAddress) {
        String plainToken = generateRandomToken();
        String tokenHash = hash(plainToken);
        LocalDateTime now = LocalDateTime.now();

        RefreshToken refreshToken = new RefreshToken(
                user,
                tokenHash,
                UUID.randomUUID().toString(),
                now.plus(Duration.ofMillis(jwtProperties.getRefreshExpiration())),
                userAgent,
                ipAddress
        );

        refreshTokenRepository.save(refreshToken);
        return plainToken;
    }

    @Transactional
    public RefreshRotationResult rotate(String plainToken, String userAgent, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        String tokenHash = hash(plainToken);
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RefreshTokenInvalidException("Refresh token is invalid"));

        if (currentToken.isExpired(now)) {
            currentToken.setRevokedAt(now);
            refreshTokenRepository.save(currentToken);
            throw new RefreshTokenExpiredException("Refresh token has expired");
        }

        if (currentToken.isRevoked() || currentToken.getReplacedById() != null) {
            revokeFamily(currentToken.getFamilyId(), now);
            throw new RefreshTokenReuseDetectedException("Refresh token reuse detected");
        }

        String newPlainToken = generateRandomToken();
        String newTokenHash = hash(newPlainToken);

        RefreshToken nextToken = new RefreshToken(
                currentToken.getUser(),
                newTokenHash,
                currentToken.getFamilyId(),
                now.plus(Duration.ofMillis(jwtProperties.getRefreshExpiration())),
                userAgent,
                ipAddress
        );

        refreshTokenRepository.save(nextToken);

        currentToken.setRevokedAt(now);
        currentToken.setReplacedById(nextToken.getId());
        currentToken.setLastUsedAt(now);
        refreshTokenRepository.save(currentToken);

        return new RefreshRotationResult(currentToken.getUser().getUsername(), newPlainToken);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId);
        for (RefreshToken token : activeTokens) {
            token.setRevokedAt(now);
        }
        refreshTokenRepository.saveAll(activeTokens);
    }

    @Transactional
    public void revokeForUser(Long userId, String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        String tokenHash = hash(plainToken);
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RefreshTokenInvalidException("Refresh token is invalid"));

        if (!currentToken.getUser().getId().equals(userId)) {
            throw new RefreshTokenOwnershipException("Refresh token does not belong to user");
        }

        revokeFamily(currentToken.getFamilyId(), LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<SessionInfoResponse> listActiveSessionsForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return refreshTokenRepository
                .findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(userId, now)
                .stream()
                .map(token -> new SessionInfoResponse(
                        token.getId(),
                        token.getFamilyId(),
                        token.getUserAgent(),
                        token.getIpAddress(),
                        token.getCreatedAt(),
                        token.getLastUsedAt(),
                        token.getExpiresAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeSessionForUser(Long userId, Long sessionId) {
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!token.getUser().getId().equals(userId)) {
            throw new RefreshTokenOwnershipException("Session does not belong to user");
        }

        revokeFamily(token.getFamilyId(), LocalDateTime.now());
    }

    private void revokeFamily(String familyId, LocalDateTime revokedAt) {
        List<RefreshToken> activeFamily = refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull(familyId);
        for (RefreshToken token : activeFamily) {
            token.setRevokedAt(revokedAt);
        }
        refreshTokenRepository.saveAll(activeFamily);
    }

    private static String generateRandomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    @Transactional
    public long cleanupExpiredAndRevokedTokens() {
        LocalDateTime cutoff = LocalDateTime.now()
                .minus(Duration.ofMillis(appRefreshProperties.getCleanupRetentionMs()));

        long expiredDeleted = refreshTokenRepository.deleteByExpiresAtBefore(cutoff);
        long revokedDeleted = refreshTokenRepository.deleteByRevokedAtBefore(cutoff);
        return expiredDeleted + revokedDeleted;
    }
}
