package com.serdarahmanov.music_app_backend.auth.integration;

import com.serdarahmanov.music_app_backend.auth.security.RedisStartupValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisStartupFailureIntegrationTest {

    @Test
    void applicationStartupFailsWhenRedisIsUnreachable() {
        Exception startupException = assertThrows(Exception.class, () -> {
            ConfigurableApplicationContext context = null;
            try {
                context = new SpringApplicationBuilder(RedisOnlyBootstrapConfig.class)
                        .web(WebApplicationType.NONE)
                        .properties(
                                "spring.data.redis.host=127.0.0.1",
                                "spring.data.redis.port=1",
                                "spring.data.redis.timeout=1s",
                                "spring.autoconfigure.exclude="
                                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
                        )
                        .run();
            } finally {
                if (context != null) {
                    context.close();
                }
            }
        });

        assertTrue(hasMessageInCauseChain(
                startupException,
                "Redis is required but unreachable during startup."
        ));
    }

    private static boolean hasMessageInCauseChain(Throwable throwable, String expectedFragment) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor.getMessage() != null && cursor.getMessage().contains(expectedFragment)) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    @TestConfiguration
    @EnableAutoConfiguration
    @Import(RedisStartupValidator.class)
    static class RedisOnlyBootstrapConfig {
    }
}
