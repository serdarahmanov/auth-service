package com.serdarahmanov.music_app_backend.auth.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender emailSender;


    public void sendHtmlMessage(List<String> to,String subject, String htmlBody){

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to.toArray(new String[0]));
            helper.setFrom("no-reply@localhost");
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // HTML mode

            emailSender.send(message);
            log.info("Sending welcome email to: {}", to);

        } catch (MessagingException e) {
            throw new RuntimeException("Error sending email", e);
        }

    }

    public void sendSimpleEmail(List<String> to, String subject, String content) {
        log.info("Sending email to: {}", to);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to.toArray(new String[0]));
        message.setSubject(subject);
        message.setText(content);

        emailSender.send(message);
    }
}
