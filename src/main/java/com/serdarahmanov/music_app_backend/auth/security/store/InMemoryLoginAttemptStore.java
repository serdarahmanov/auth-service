package com.serdarahmanov.music_app_backend.auth.security.store;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryLoginAttemptStore implements LoginAttemptStore {

    private final ConcurrentMap<String, AttemptState> attempts = new ConcurrentHashMap<>();

    @Override
    public boolean isBlocked(String key) {
        AttemptState state = attempts.get(key);
        if (state == null) {
            return false;
        }

        Instant now = Instant.now();
        if (state.blockedUntil != null && state.blockedUntil.isAfter(now)) {
            return true;
        }

        if (state.blockedUntil != null && !state.blockedUntil.isAfter(now)) {
            attempts.remove(key);
        }
        return false;
    }

    @Override
    public void onFailure(String key, int maxFailedAttempts, Duration lockDuration) {
        Instant now = Instant.now();
        attempts.compute(key, (ignored, current) -> {
            AttemptState next = current == null ? new AttemptState() : current;

            if (next.blockedUntil != null && next.blockedUntil.isAfter(now)) {
                return next;
            }

            if (next.blockedUntil != null && !next.blockedUntil.isAfter(now)) {
                next.failedAttempts = 0;
                next.blockedUntil = null;
            }

            next.failedAttempts++;
            if (next.failedAttempts >= maxFailedAttempts) {
                next.failedAttempts = 0;
                next.blockedUntil = now.plus(lockDuration);
            }
            return next;
        });
    }

    @Override
    public void onSuccess(String key) {
        attempts.remove(key);
    }

    private static final class AttemptState {
        private int failedAttempts;
        private Instant blockedUntil;
    }
}
