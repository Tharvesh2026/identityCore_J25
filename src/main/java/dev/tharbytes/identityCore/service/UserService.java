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

    public UserService(UserRepository userRepository, RoleRepository roleRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
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

    /** Register a new user with USER role */
    @Transactional
    public UserEntity register(String name, String username, String email, String password) throws MessagingException, IOException {

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
        user.setStatus("ACTIVE");

        UserEntity saved = userRepository.save(user);
        log.info("User registered: {}", saved.getUsername());
        emailService.inviteMail(email.toLowerCase());
        return saved;
    }

    /** Update profile (name, username, email) */
    @Transactional
    public UserEntity updateProfile(Long userId, String name, String username, String email) {
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

        UserEntity saved = userRepository.save(user);
        log.info("User [{}] updated successfully.", userId);
        return saved;
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