package com.serdarahmanov.music_app_backend.auth.security;

import com.serdarahmanov.music_app_backend.auth.security.store.LoginAttemptStore;
import com.serdarahmanov.music_app_backend.utility.config.AppLoginProtectionProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
public class LoginAttemptService {

    private final AppLoginProtectionProperties appLoginProtectionProperties;
    private final LoginAttemptStore loginAttemptStore;
    private final Counter rateLimitedCounter;

    public LoginAttemptService(
            AppLoginProtectionProperties appLoginProtectionProperties,
            LoginAttemptStore loginAttemptStore,
            ObjectProvider<MeterRegistry> meterRegistryProvider
    ) {
        this.appLoginProtectionProperties = appLoginProtectionProperties;
        this.loginAttemptStore = loginAttemptStore;
        MeterRegistry registry = meterRegistryProvider.getIfAvailable(SimpleMeterRegistry::new);
        this.rateLimitedCounter = Counter.builder("auth_login_rate_limited_total")
                .description("Total number of login requests blocked by rate limiting")
                .register(registry);
    }

    public void assertNotBlocked(String username, String ipAddress) {
        String key = toKey(username, ipAddress);
        if (loginAttemptStore.isBlocked(key)) {
            rateLimitedCounter.increment();
            throw new LoginRateLimitExceededException("Too many failed login attempts. Try again later.");
        }
    }

    public void recordFailedAttempt(String username, String ipAddress) {
        String key = toKey(username, ipAddress);
        loginAttemptStore.onFailure(
                key,
                appLoginProtectionProperties.getMaxFailedAttempts(),
                Duration.ofMillis(appLoginProtectionProperties.getLockDurationMs())
        );
    }

    public void recordSuccessfulLogin(String username, String ipAddress) {
        loginAttemptStore.onSuccess(toKey(username, ipAddress));
    }

    private static String toKey(String username, String ipAddress) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        String normalizedIp = ipAddress == null ? "" : ipAddress.trim();
        return normalizedUsername + "|" + normalizedIp;
    }
}
