package com.serdarahmanov.music_app_backend.auth.security.store;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;

@RequiredArgsConstructor
public class RedisLoginAttemptStore implements LoginAttemptStore {

    private static final String FAILURE_PREFIX = "auth:login:failures:";
    private static final String BLOCK_PREFIX = "auth:login:blocked:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean isBlocked(String key) {
        Boolean exists = redisTemplate.hasKey(blockKey(key));
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void onFailure(String key, int maxFailedAttempts, Duration lockDuration) {
        if (isBlocked(key)) {
            return;
        }

        String failureKey = failureKey(key);
        Long count = redisTemplate.opsForValue().increment(failureKey);
        if (count == null) {
            return;
        }

        if (count == 1L) {
            redisTemplate.expire(failureKey, lockDuration);
        }

        if (count >= maxFailedAttempts) {
            redisTemplate.opsForValue().set(blockKey(key), "1", lockDuration);
            redisTemplate.delete(failureKey);
        }
    }

    @Override
    public void onSuccess(String key) {
        redisTemplate.delete(List.of(failureKey(key), blockKey(key)));
    }

    private static String failureKey(String key) {
        return FAILURE_PREFIX + key;
    }

    private static String blockKey(String key) {
        return BLOCK_PREFIX + key;
    }
}
