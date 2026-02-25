package com.serdarahmanov.music_app_backend.auth.forcodex.jobs;

import com.serdarahmanov.music_app_backend.auth.forcodex.service.Oauth2AuthorizationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Oauth2AuthorizationCodeCleanupJob {

    private final Oauth2AuthorizationCodeService authorizationCodeService;

    @Scheduled(
            fixedDelayString = "${app.oauth2.cleanup-interval-ms:3600000}",
            initialDelayString = "${app.oauth2.cleanup-initial-delay-ms:60000}"
    )
    public void cleanupExpiredAndUsedCodes() {
        authorizationCodeService.cleanupExpiredAndUsedCodes();
        log.debug("oauth2 authCode cleanup executed");
    }
}
