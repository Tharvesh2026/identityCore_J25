package dev.tharbytes.identityCore.controller;

import dev.tharbytes.identityCore.entity.UserEntity;
import dev.tharbytes.identityCore.repository.UserRepository;
import dev.tharbytes.identityCore.security.AuthHelper;
import dev.tharbytes.identityCore.service.EmailService;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;

@Controller
public class VerificationController {

    private static final Logger log = LoggerFactory.getLogger(VerificationController.class);

    private final AuthHelper authHelper;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public VerificationController(AuthHelper authHelper, UserRepository userRepository, EmailService emailService) {
        this.authHelper = authHelper;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @GetMapping("/verify-otp")
    public String verifyOtpPage(Model model) {
        UserEntity currentUser = authHelper.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (!"PENDING_VERIFICATION".equalsIgnoreCase(currentUser.getStatus())) {
            return "redirect:/welcome";
        }
        model.addAttribute("email", currentUser.getMailId());
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(
            @RequestParam String otp,
            RedirectAttributes ra) {
        UserEntity currentUser = authHelper.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (!"PENDING_VERIFICATION".equalsIgnoreCase(currentUser.getStatus())) {
            return "redirect:/welcome";
        }

        if (currentUser.getVerificationOtp() == null ||
            currentUser.getOtpExpiresAt() == null ||
            !currentUser.getVerificationOtp().equals(otp.trim()) ||
            LocalDateTime.now().isAfter(currentUser.getOtpExpiresAt())) {

            log.warn("OTP verification failed for user [{}]: invalid or expired code.", currentUser.getUsername());
            ra.addFlashAttribute("error", "Invalid or expired verification code.");
            return "redirect:/verify-otp";
        }

        // OTP is valid
        currentUser.setStatus("ACTIVE");
        currentUser.setVerified(true);
        currentUser.setVerificationOtp(null);
        currentUser.setOtpExpiresAt(null);
        userRepository.save(currentUser);

        log.info("OTP verification successful for user [{}]. Account status updated to ACTIVE.", currentUser.getUsername());

        // Send final welcome/invite email
        try {
            emailService.inviteMail(currentUser.getMailId());
        } catch (IOException | MessagingException e) {
            log.error("Failed to send welcome invite email after verification to {}", currentUser.getMailId(), e);
        }

        ra.addFlashAttribute("success", "Account verified successfully! Welcome to i.Core.");
        return "redirect:/welcome";
    }

    @PostMapping("/verify-otp/resend")
    public String resendOtp(RedirectAttributes ra) {
        UserEntity currentUser = authHelper.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (!"PENDING_VERIFICATION".equalsIgnoreCase(currentUser.getStatus())) {
            return "redirect:/welcome";
        }

        String newOtp = String.format("%06d", new Random().nextInt(1000000));
        currentUser.setVerificationOtp(newOtp);
        currentUser.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(currentUser);

        log.info("New OTP generated and saved for user [{}].", currentUser.getUsername());

        try {
            emailService.sendOtpVerificationEmail(currentUser.getMailId(), newOtp);
            ra.addFlashAttribute("success", "A new verification code has been sent to your email.");
        } catch (Exception e) {
            log.error("Failed to send resent OTP verification email to {}", currentUser.getMailId(), e);
            ra.addFlashAttribute("error", "Failed to send verification email. Please try again.");
        }

        return "redirect:/verify-otp";
    }
}
