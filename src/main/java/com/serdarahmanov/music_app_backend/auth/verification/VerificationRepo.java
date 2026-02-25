package com.serdarahmanov.music_app_backend.auth.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationRepo extends JpaRepository<VerificationCode, Long> {


    Optional<VerificationCode> findByCode(String code);

    Optional<VerificationCode> findByUserId(Long userId);
}
