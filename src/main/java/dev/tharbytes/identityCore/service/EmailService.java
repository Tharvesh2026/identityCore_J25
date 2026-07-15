package dev.tharbytes.identityCore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Reset your i.Core password");
            message.setText(
                    "We received a request to reset your password.\n\n" +
                            "Click the link below to choose a new password. This link expires in 30 minutes:\n\n" +
                            resetLink + "\n\n" +
                            "If you didn't request this, you can safely ignore this email."
            );
            mailSender.send(message);
        } catch (Exception e) {
            // Dev-friendly fallback: if SMTP isn't configured (or fails), log the
            // link so the flow is still testable locally without real email.
            log.warn("Failed to send password reset email — falling back to log output. "
                    + "Configure spring.mail.* properties for real delivery.", e);
            log.info("PASSWORD RESET LINK for {}: {}", toEmail, resetLink);
        }
    }
}