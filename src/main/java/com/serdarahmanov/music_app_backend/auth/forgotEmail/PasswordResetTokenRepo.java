package com.serdarahmanov.music_app_backend.auth.forgotEmail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepo extends JpaRepository<PasswordResetToken,Long> {

    Optional<PasswordResetToken> findByCode(String code);
}
