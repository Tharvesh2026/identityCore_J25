package dev.tharbytes.identityCore.service;

import dev.tharbytes.identityCore.entity.*;
import dev.tharbytes.identityCore.exception.*;
import dev.tharbytes.identityCore.repository.*;
import dev.tharbytes.identityCore.util.PasswordUtil;
import dev.tharbytes.identityCore.util.ValidationUtil;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, EmailService emailService, PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    /** Validate credentials (returns true if password matches) */
    public boolean validateUser(String mailId, String password) {
        log.info("Authentication initiated for username [{}].", mailId);
        boolean valid = userRepository.findByMailId(mailId)
                .map(u -> PasswordUtil.verify(password, u.getPassword()))
                .orElse(false);
        if (valid) {
            log.info("Authentication successful for username [{}].", mailId);
        } else {
            log.warn("Authentication failed for username [{}].", mailId);
        }
        return valid;
    }

    /** Find user by email */
    public Optional<UserEntity> findByEmail(String mailId) {
        return userRepository.findByMailId(mailId);
    }

    /** Find user by ID */
    public UserEntity getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User [{}] not found.", id);
                    return new ResourceNotFoundException("User not found");
                });
    }

    /** Get all users */
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    /** Register a new user with USER role and pending verification status */
    @Transactional
    public UserEntity register(String name, String username, String email, String password,
                               String businessName, String location,
                               String awsAccountId, String gcpProjectId, String azureSubscriptionId) throws MessagingException, IOException {

        if (userRepository.existsByMailId(email)) {
            log.warn("Registration failed: email [{}] is already registered.", email);
            throw new ValidationException("Email is already registered");
        }
        if (userRepository.existsByUsername(username)) {
            log.warn("Registration failed: username [{}] is already taken.", username);
            throw new ValidationException("Username is already taken");
        }

        RoleEntity userRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));

        UserEntity user = new UserEntity();
        user.setName(ValidationUtil.sanitizeName(name));
        user.setUsername(ValidationUtil.sanitizeUsername(username));
        user.setMailId(email.toLowerCase());
        user.setPassword(PasswordUtil.hash(password));
        user.setRole(userRole);
        user.setProvider("LOCAL");
        user.setVerified(false);
        user.setStatus("PENDING_VERIFICATION");
        
        user.setBusinessName(businessName);
        user.setLocation(location);
        user.setAwsAccountId(awsAccountId);
        user.setGcpProjectId(gcpProjectId);
        user.setAzureSubscriptionId(azureSubscriptionId);

        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
        user.setVerificationOtp(otp);
        user.setOtpExpiresAt(java.time.LocalDateTime.now().plusMinutes(10));

        UserEntity saved = userRepository.save(user);
        log.info("User registered (pending verification): {}", saved.getUsername());
        return saved;
    }

    /** Update profile (name, username, email, businessName, location) */
    @Transactional
    public UserEntity updateProfile(Long userId, String name, String username, String email, String businessName, String location) {
        UserEntity user = getById(userId);

        // Check uniqueness only if changed
        if (!email.equalsIgnoreCase(user.getMailId()) && userRepository.existsByMailId(email)) {
            log.warn("Profile update failed for user [{}]: email [{}] already in use.", userId, email);
            throw new ValidationException("Email is already in use");
        }
        if (!username.equalsIgnoreCase(user.getUsername()) && userRepository.existsByUsername(username)) {
            log.warn("Profile update failed for user [{}]: username [{}] already in use.", userId, username);
            throw new ValidationException("Username is already in use");
        }

        user.setName(ValidationUtil.sanitizeName(name));
        user.setUsername(ValidationUtil.sanitizeUsername(username));
        user.setMailId(email.toLowerCase());
        user.setBusinessName(businessName);
        user.setLocation(location);

        UserEntity saved = userRepository.save(user);
        log.info("User [{}] updated successfully.", userId);
        return saved;
    }

    /** Delete user by ID */
    @Transactional
    public void deleteUser(Long id) {
        passwordResetTokenRepository.deleteByUserId(id);
        userRepository.deleteById(id);
        log.info("User ID [{}] deleted successfully.", id);
    }

    /** Verify current password */
    public boolean verifyPassword(Long userId, String plainPassword) {
        UserEntity user = getById(userId);
        return PasswordUtil.verify(plainPassword, user.getPassword());
    }

    /** Reset password (admin) */
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        UserEntity user = getById(userId);
        user.setPassword(PasswordUtil.hash(newPassword));
        userRepository.save(user);
        log.info("Password reset for user id={}", userId);
    }

    /** Change own password */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        if (!verifyPassword(userId, currentPassword)) {
            log.warn("Password change failed for user [{}]: current password is incorrect.", userId);
            throw new ValidationException("Current password is incorrect");
        }
        resetPassword(userId, newPassword);
    }

    /** Update user role */
    @Transactional
    public void updateRole(Long userId, Long roleId) {
        UserEntity user = getById(userId);
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        user.setRole(role);
        userRepository.save(user);
        log.info("User {} role updated to {}", userId, role.getRoleName());
    }

    /** Update user status */
    @Transactional
    public void updateStatus(Long userId, String status) {
        UserEntity user = getById(userId);
        user.setStatus(status);
        userRepository.save(user);
        log.info("User {} status updated to {}", userId, status);
    }

    /** Complete verification with OTP */
    @Transactional
    public boolean verifyOtp(Long userId, String otp) {
        UserEntity user = getById(userId);
        if (!"PENDING_VERIFICATION".equalsIgnoreCase(user.getStatus())) {
            return true;
        }
        if (user.getVerificationOtp() == null ||
            user.getOtpExpiresAt() == null ||
            !user.getVerificationOtp().equals(otp.trim()) ||
            java.time.LocalDateTime.now().isAfter(user.getOtpExpiresAt())) {
            return false;
        }
        user.setStatus("ACTIVE");
        user.setVerified(true);
        user.setVerificationOtp(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);
        log.info("User {} verified successfully via OTP.", userId);
        
        try {
            emailService.inviteMail(user.getMailId());
        } catch (Exception e) {
            log.error("Failed to send welcome invite email after verification to {}", user.getMailId(), e);
        }
        return true;
    }

    /** Resend registration verification OTP */
    @Transactional
    public void resendOtp(Long userId) throws MessagingException, IOException {
        UserEntity user = getById(userId);
        if (!"PENDING_VERIFICATION".equalsIgnoreCase(user.getStatus())) {
            throw new ValidationException("Account is already verified");
        }
        String newOtp = String.format("%06d", new java.util.Random().nextInt(1000000));
        user.setVerificationOtp(newOtp);
        user.setOtpExpiresAt(java.time.LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        log.info("Regenerated OTP for user {}", userId);
        emailService.sendOtpVerificationEmail(user.getMailId(), newOtp);
    }

    /** Get user permissions */
    public List<String> getPermissions(Long userId) {
        UserEntity user = getById(userId);
        return user.getRole().getPermissions().stream()
                .map(p -> p.getPermissionKey())
                .sorted()
                .toList();
    }

    /** Check if user has a specific permission */
    public boolean hasPermission(Long userId, String permissionKey) {
        UserEntity user = getById(userId);
        return user.getRole().getPermissions().stream()
                .anyMatch(p -> p.getPermissionKey().equals(permissionKey));
    }
}