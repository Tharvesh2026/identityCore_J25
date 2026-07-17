package dev.tharbytes.identityCore.service;

import dev.tharbytes.identityCore.entity.PasswordResetToken;
import dev.tharbytes.identityCore.entity.UserEntity;
import dev.tharbytes.identityCore.repository.PasswordResetTokenRepository;
import dev.tharbytes.identityCore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.password-reset.token-validity-minutes:30}")
    private int tokenValidityMinutes;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Always appears to succeed from the caller's perspective, even if the
     * email doesn't exist — prevents "forgot password" being used to
     * enumerate which emails have accounts.
     */
    @Transactional
    public void initiateReset(String email) {
        Optional<UserEntity> userOpt = userRepository.findByMailId(email);
        if (userOpt.isEmpty()) {
            return;
        }

        UserEntity user = userOpt.get();

        // Invalidate any previously issued, still-outstanding tokens first.
        tokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(
                token, user.getId(), LocalDateTime.now().plusMinutes(tokenValidityMinutes));
        tokenRepository.save(resetToken);

        String resetLink = baseUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getMailId(), resetLink);
    }

    public Optional<PasswordResetToken> validateToken(String token) {
        return tokenRepository.findByToken(token)
                .filter(t -> !t.isUsed() && !t.isExpired());
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = validateToken(token);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        Optional<UserEntity> userOpt = userRepository.findById(resetToken.getUserId());
        if (userOpt.isEmpty()) {
            return false;
        }

        UserEntity user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        return true;
    }
}