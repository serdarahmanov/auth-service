package com.serdarahmanov.music_app_backend.auth.refresh.jobs;

import com.serdarahmanov.music_app_backend.auth.refresh.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final RefreshTokenService refreshTokenService;

    @Scheduled(
            fixedDelayString = "${app.refresh.cleanup-interval-ms:3600000}",
            initialDelayString = "${app.refresh.cleanup-initial-delay-ms:60000}"
    )
    public void cleanupExpiredAndRevokedTokens() {
        long deletedCount = refreshTokenService.cleanupExpiredAndRevokedTokens();
        log.debug("refresh token cleanup executed, deleted {} rows", deletedCount);
    }
}
