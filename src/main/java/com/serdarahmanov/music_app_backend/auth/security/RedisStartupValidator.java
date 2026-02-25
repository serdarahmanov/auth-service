package com.serdarahmanov.music_app_backend.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisStartupValidator {

    private final RedisConnectionFactory redisConnectionFactory;

    @jakarta.annotation.PostConstruct
    public void validateRedisConnection() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            if (!"PONG".equalsIgnoreCase(pong)) {
                throw new IllegalStateException("Redis ping failed during startup.");
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Redis is required but unreachable during startup.", ex);
        }
    }
}
