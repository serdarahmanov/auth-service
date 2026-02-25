package com.serdarahmanov.music_app_backend.auth.jobs.welcomeEmailJob;

import com.serdarahmanov.music_app_backend.auth.email.EmailService;
import com.serdarahmanov.music_app_backend.auth.verification.VerificationCode;
import com.serdarahmanov.music_app_backend.users.Users;
import com.serdarahmanov.music_app_backend.users.repo.UserRepository;
import com.serdarahmanov.music_app_backend.auth.verification.VerificationRepo;
import com.serdarahmanov.music_app_backend.utility.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.List;

@Component
@Slf4j
public class SendWelcomeEmailJobHandler
        implements JobRequestHandler<SendWelcomeEmailJob> {

    private final UserRepository userRepo;
    private final VerificationRepo verificationRepo;
    private final ApplicationProperties applicationProperties;
    private final SpringTemplateEngine emailTemplateEngine;
    private final EmailService emailService;

    public SendWelcomeEmailJobHandler(
            UserRepository userRepo,
            VerificationRepo verificationRepo,
            ApplicationProperties applicationProperties,
            @Qualifier("emailTemplateEngine") SpringTemplateEngine emailTemplateEngine,
            EmailService emailService
    ) {
        this.userRepo = userRepo;
        this.verificationRepo = verificationRepo;
        this.applicationProperties = applicationProperties;
        this.emailTemplateEngine = emailTemplateEngine;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public void run(SendWelcomeEmailJob job) {

        Users user = userRepo.findById(job.getUserId())
                .orElseThrow(() ->
                        new RuntimeException("User not found: " + job.getUserId()));

        log.info("Sending welcome email to user {}", user.getId());

        VerificationCode verificationCode = user.getVerificationCode();
        if (verificationCode == null || verificationCode.isEmailSent()) {
            log.info("Welcome email already sent for user {}", user.getId());
            return;
        }

        sendWelcomeEmail(user, verificationCode);
    }

    private void sendWelcomeEmail(Users user, VerificationCode verificationCode) {

        String verificationLink =
                applicationProperties.getBaseUrl()
                        + "/api/auth/verify-email?token=" + verificationCode.getCode();

        Context context = new Context();
        context.setVariable("firstName", user.getFirstName());
        context.setVariable("verificationLink", verificationLink);
        context.setVariable("applicationName", applicationProperties.getApplicationName());

        String htmlBody = emailTemplateEngine.process("welcome-email", context);

        emailService.sendHtmlMessage(
                List.of(user.getEmail()),
                "Welcome to Wavy Music",
                htmlBody
        );

        verificationCode.setEmailSent(true);
        verificationRepo.save(verificationCode);

        log.info("Welcome email sent to {}", user.getEmail());
    }
}
