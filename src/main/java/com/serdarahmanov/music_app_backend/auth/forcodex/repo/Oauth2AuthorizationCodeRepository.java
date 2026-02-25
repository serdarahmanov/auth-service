package com.serdarahmanov.music_app_backend.auth.forcodex.repo;

import com.serdarahmanov.music_app_backend.auth.forcodex.entity.Oauth2AuthorizationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface Oauth2AuthorizationCodeRepository extends JpaRepository<Oauth2AuthorizationCode, Long> {
    Optional<Oauth2AuthorizationCode> findByCode(String code);

    void deleteByExpiresAtBeforeOrUsed(LocalDateTime now, boolean used);
}
