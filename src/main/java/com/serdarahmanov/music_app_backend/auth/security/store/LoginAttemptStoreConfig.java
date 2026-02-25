package com.serdarahmanov.music_app_backend.auth.security.store;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class LoginAttemptStoreConfig {

    @Bean
    public LoginAttemptStore redisLoginAttemptStore(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return new RedisLoginAttemptStore(redisTemplate);
    }
}
