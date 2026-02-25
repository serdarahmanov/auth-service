package com.serdarahmanov.music_app_backend.auth.forcodex.service;

import com.serdarahmanov.music_app_backend.auth.forcodex.entity.Oauth2AuthorizationCode;
import com.serdarahmanov.music_app_backend.auth.forcodex.repo.Oauth2AuthorizationCodeRepository;
import com.serdarahmanov.music_app_backend.utility.config.AppOauth2Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class Oauth2AuthorizationCodeService {

    private final Oauth2AuthorizationCodeRepository repository;
    private final AppOauth2Properties appOauth2Properties;

    @Transactional
    public String createCode(String username) {
        Oauth2AuthorizationCode authCode = new Oauth2AuthorizationCode(username, appOauth2Properties.getAuthorizationCodeTtlSeconds());
        repository.save(authCode);
        return authCode.getCode();
    }

    @Transactional
    public String consumeUsername(String code) {
        Oauth2AuthorizationCode authCode = repository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid oauth2 code"));

        if (authCode.isUsed()) {
            throw new IllegalArgumentException("Oauth2 code already used");
        }

        if (authCode.isExpired()) {
            throw new IllegalArgumentException("Oauth2 code expired");
        }

        authCode.setUsed(true);
        repository.save(authCode);

        return authCode.getUsername();
    }

    @Transactional
    public void cleanupExpiredAndUsedCodes() {
        repository.deleteByExpiresAtBeforeOrUsed(LocalDateTime.now(), true);
    }
}
