package com.serdarahmanov.music_app_backend.auth.security.store;

import java.time.Duration;

public interface LoginAttemptStore {

    boolean isBlocked(String key);

    void onFailure(String key, int maxFailedAttempts, Duration lockDuration);

    void onSuccess(String key);
}
