package com.serdarahmanov.music_app_backend.auth.jobs.resetEmailJob;

import com.serdarahmanov.music_app_backend.auth.email.EmailService;
import com.serdarahmanov.music_app_backend.auth.forgotEmail.PasswordResetToken;
import com.serdarahmanov.music_app_backend.auth.forgotEmail.PasswordResetTokenRepo;
import com.serdarahmanov.music_app_backend.auth.identity.Users;
import com.serdarahmanov.music_app_backend.auth.identity.repo.UserRepository;
import com.serdarahmanov.music_app_backend.utility.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Comparator;
import java.util.List;

@Component
@Slf4j
public class SendResetPasswordEmailJobHandler
        implements JobRequestHandler<SendResetPasswordEmailJob> {

    private final UserRepository userRepo;
    private final PasswordResetTokenRepo passwordResetTokenRepo;
    private final ApplicationProperties applicationProperties;
    private final SpringTemplateEngine emailTemplateEngine;
    private final EmailService emailService;

    public SendResetPasswordEmailJobHandler(
            UserRepository userRepo,
            PasswordResetTokenRepo passwordResetTokenRepo,
            ApplicationProperties applicationProperties,
            @Qualifier("emailTemplateEngine") SpringTemplateEngine emailTemplateEngine,
            EmailService emailService
    ) {
        this.userRepo = userRepo;
        this.passwordResetTokenRepo = passwordResetTokenRepo;
        this.applicationProperties = applicationProperties;
        this.emailTemplateEngine = emailTemplateEngine;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public void run(SendResetPasswordEmailJob job) {

        Users user = userRepo.findById(job.getUserId()).orElse(null);
        if (user == null) {
            log.warn("User not found for id {}", job.getUserId());
            return;
        }

        log.info("Processing reset-password email for user {}", user.getId());

        PasswordResetToken token = user.getResetTokens()
                .stream()
                .filter(t -> !t.isEmailSent())
                .max(Comparator.comparing(PasswordResetToken::getCreatedAt))
                .orElse(null);

        if (token == null) {
            log.info("No pending unsent reset-password tokens for user {}", user.getId());
            return;
        }

        sendResetPassword(user, token);
    }

    private void sendResetPassword(Users user, PasswordResetToken token) {

        String verificationLink =
                applicationProperties.getBaseUrl()
                        + "/api/auth/forgot-password?code=" + token.getCode();

        Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable("verificationLink", verificationLink);
        context.setVariable("applicationName", applicationProperties.getApplicationName());

        String htmlBody = emailTemplateEngine.process("forgot-password", context);

        emailService.sendHtmlMessage(
                List.of(user.getEmail()),
                "Password Reset Requested",
                htmlBody
        );

        token.onEmailSent();
        passwordResetTokenRepo.save(token);

        log.info("Reset-password email sent to {}", user.getEmail());
    }
}
