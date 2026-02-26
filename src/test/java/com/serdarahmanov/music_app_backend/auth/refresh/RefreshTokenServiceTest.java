package com.serdarahmanov.music_app_backend.auth.refresh;

import com.serdarahmanov.music_app_backend.entity.AbstractEntity;
import com.serdarahmanov.music_app_backend.auth.refresh.exceptions.RefreshTokenReuseDetectedException;
import com.serdarahmanov.music_app_backend.auth.identity.Users;
import com.serdarahmanov.music_app_backend.utility.config.AppRefreshProperties;
import com.serdarahmanov.music_app_backend.utility.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;
    private Users user;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setRefreshExpiration(1_209_600_000L);
        AppRefreshProperties appRefreshProperties = new AppRefreshProperties();
        appRefreshProperties.setCleanupRetentionMs(604_800_000L);

        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtProperties, appRefreshProperties);
        user = new Users("alice@example.com", "alice", "password");
    }

    @Test
    void issueStoresOnlyHashedToken() {
        String issued = refreshTokenService.issue(user, "ua", "127.0.0.1");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertNotNull(issued);
        assertFalse(issued.isBlank());
        assertNotEquals(issued, saved.getTokenHash());
        assertNotNull(saved.getFamilyId());
        assertEquals(user, saved.getUser());
    }

    @Test
    void rotateRevokesCurrentAndReturnsNewToken() {
        AtomicLong idSequence = new AtomicLong(100);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                setId(token, idSequence.incrementAndGet());
            }
            return token;
        });

        RefreshToken current = new RefreshToken(
                user,
                "current-hash",
                "family-1",
                LocalDateTime.now().plusDays(1),
                "ua-old",
                "10.0.0.1"
        );

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(current));

        RefreshRotationResult result = refreshTokenService.rotate("plain-token", "ua-new", "10.0.0.2");

        assertEquals("alice", result.username());
        assertNotNull(result.refreshToken());
        assertFalse(result.refreshToken().isBlank());
        assertNotNull(current.getRevokedAt());
        assertNotNull(current.getLastUsedAt());
        verify(refreshTokenRepository, atLeastOnce()).save(any(RefreshToken.class));
    }

    @Test
    void rotateOnRevokedTokenRevokesFamilyAndThrows() {
        RefreshToken current = new RefreshToken(
                user,
                "current-hash",
                "family-2",
                LocalDateTime.now().plusDays(1),
                "ua-old",
                "10.0.0.1"
        );
        current.setRevokedAt(LocalDateTime.now().minusMinutes(1));

        RefreshToken sibling = new RefreshToken(
                user,
                "sibling-hash",
                "family-2",
                LocalDateTime.now().plusDays(1),
                "ua-sibling",
                "10.0.0.3"
        );

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(current));
        when(refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull("family-2")).thenReturn(List.of(sibling));

        RefreshTokenReuseDetectedException exception = assertThrows(
                RefreshTokenReuseDetectedException.class,
                () -> refreshTokenService.rotate("reused-token", "ua-new", "10.0.0.2")
        );

        assertEquals("Refresh token reuse detected", exception.getMessage());
        assertNotNull(sibling.getRevokedAt());
        verify(refreshTokenRepository).saveAll(List.of(sibling));
    }

    @Test
    void revokeForUserRevokesOnlyTokensInProvidedFamily() {
        setId(user, 42L);

        RefreshToken current = new RefreshToken(
                user,
                "current-hash",
                "family-logout",
                LocalDateTime.now().plusDays(1),
                "ua-current",
                "10.0.0.9"
        );

        RefreshToken sibling = new RefreshToken(
                user,
                "sibling-hash",
                "family-logout",
                LocalDateTime.now().plusDays(1),
                "ua-sibling",
                "10.0.0.10"
        );

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(current));
        when(refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull("family-logout"))
                .thenReturn(List.of(current, sibling));

        refreshTokenService.revokeForUser(42L, "device-token");

        assertNotNull(current.getRevokedAt());
        assertNotNull(sibling.getRevokedAt());
        verify(refreshTokenRepository).saveAll(List.of(current, sibling));
    }

    @Test
    void cleanupDeletesExpiredAndRevokedOlderThanRetention() {
        when(refreshTokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(3L);
        when(refreshTokenRepository.deleteByRevokedAtBefore(any(LocalDateTime.class))).thenReturn(2L);

        long deleted = refreshTokenService.cleanupExpiredAndRevokedTokens();

        ArgumentCaptor<LocalDateTime> expiresCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> revokedCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(refreshTokenRepository, times(1)).deleteByExpiresAtBefore(expiresCaptor.capture());
        verify(refreshTokenRepository, times(1)).deleteByRevokedAtBefore(revokedCaptor.capture());

        LocalDateTime cutoff = expiresCaptor.getValue();
        long deltaSeconds = Math.abs(Duration.between(
                LocalDateTime.now().minusDays(7),
                cutoff
        ).getSeconds());
        assertTrue(deltaSeconds < 5);
        assertEquals(cutoff, revokedCaptor.getValue());
        assertEquals(5L, deleted);
    }

    private static void setId(AbstractEntity entity, long value) {
        try {
            Field idField = AbstractEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, value);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }
}
