package dev.tharbytes.identityCore.service;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {

        try {

            Context context = new Context();
            context.setVariable("resetLink", resetLink);

            String html = templateEngine.process("mail/reset-password", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(new InternetAddress(fromAddress, "Tharvbytes.dev"));
            helper.setTo(toEmail);
            helper.setSubject("Reset your Identity Core password");
            helper.setText(html, true);

            mailSender.send(mimeMessage);

            log.info("Password reset email sent to {}", toEmail);

        } catch (Exception e) {

            log.error("Failed to send password reset email to {}", toEmail, e);

            // Fallback
            log.info("PASSWORD RESET LINK for {} : {}", toEmail, resetLink);
        }
    }

    public MimeMessage inviteMail(String to)
            throws IOException, MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                true,
                StandardCharsets.UTF_8.name()
        );

        helper.setFrom(new InternetAddress(fromAddress,"Tharvbytes.dev"));
        helper.setTo(to);
        helper.setSubject("Welcome to Identity Core by Tharvbytes.dev, Your Account Is Ready");;

        ClassPathResource resource =
                new ClassPathResource("templates/mail/invite.html");

        String html = new String(
                resource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        helper.setText(html, true);

        mailSender.send(message);

        return message;
    }
}