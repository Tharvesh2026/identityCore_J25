package dev.tharbytes.identityCore.controller.api;

import dev.tharbytes.identityCore.dto.request.ForgotPasswordRequest;
import dev.tharbytes.identityCore.dto.request.PublicResetPasswordRequest;
import dev.tharbytes.identityCore.dto.request.RegisterRequest;
import dev.tharbytes.identityCore.dto.response.*;
import dev.tharbytes.identityCore.entity.UserEntity;
import dev.tharbytes.identityCore.exception.ValidationException;
import dev.tharbytes.identityCore.security.AuthHelper;
import dev.tharbytes.identityCore.service.LogParserService;
import dev.tharbytes.identityCore.service.PasswordResetService;
import dev.tharbytes.identityCore.service.RoleService;
import dev.tharbytes.identityCore.service.UserService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
public class UserApiController {

    private static final Logger log = LoggerFactory.getLogger(UserApiController.class);

    private final UserService userService;
    private final RoleService roleService;
    private final LogParserService logParserService;
    private final PasswordResetService passwordResetService;
    private final AuthHelper authHelper;

    @Value("${app.legal.terms-updated}")
    private String termsUpdated;

    @Value("${app.legal.privacy-updated}")
    private String privacyUpdated;

    @Value("${app.legal.contact-email}")
    private String contactEmail;

    @Value("${app.legal.company-name}")
    private String companyName;

    @Value("${app.legal.cookie-policy-updated:July 15, 2026}")
    private String cookiePolicyUpdated;

    public UserApiController(UserService userService,
                             RoleService roleService,
                             LogParserService logParserService,
                             PasswordResetService passwordResetService,
                             AuthHelper authHelper) {
        this.userService = userService;
        this.roleService = roleService;
        this.logParserService = logParserService;
        this.passwordResetService = passwordResetService;
        this.authHelper = authHelper;
    }

    /** GET /user/profile — current user profile */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        UserEntity user = authHelper.requireCurrentUser();
        log.info("Profile retrieved successfully for user [{}].", user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved", UserResponse.from(user)));
    }

    /** GET /user/profile/details — profile details */
    @GetMapping("/user/profile/details")
    public ResponseEntity<ApiResponse<UserResponse>> getProfileDetails() {
        UserEntity user = authHelper.requireCurrentUser();
        log.info("Profile details retrieved successfully for user [{}].", user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Profile details", UserResponse.from(user)));
    }

    /** GET /user/welcome — current user dashboard welcome info */
    @GetMapping("/welcome")
    public ResponseEntity<ApiResponse<UserResponse>> getWelcome() {
        UserEntity user = authHelper.requireCurrentUser();
        log.info("Welcome details retrieved successfully for user [{}].", user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Welcome details retrieved", UserResponse.from(user)));
    }

    /** GET /user/settings — current user dashboard settings info */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<DashboardSettingsResponse>> getSettings(HttpServletRequest request) {
        UserEntity user = authHelper.requireCurrentUser();
        log.info("Settings details retrieved successfully for user [{}].", user.getId());
        DashboardSettingsResponse resp = new DashboardSettingsResponse(
                UserResponse.from(user),
                request.getSession().getId(),
                request.getSession().getCreationTime(),
                request.getSession().getLastAccessedTime(),
                request.getSession().getMaxInactiveInterval()
        );
        return ResponseEntity.ok(ApiResponse.ok("Settings details retrieved", resp));
    }

    /** POST /user/refresh-session — session refresh endpoint */
    @PostMapping("/refresh-session")
    public ResponseEntity<Void> refreshSession(HttpServletRequest request) {
        if (request.getSession(false) == null) {
            log.warn("Session refresh denied: no active session found.");
            return ResponseEntity.status(401).build();
        }
        request.getSession().setAttribute("refreshed", System.currentTimeMillis());
        log.info("Session [{}] refreshed successfully.", request.getSession().getId());
        return ResponseEntity.ok().build();
    }

    /** GET /user/users — list all users with stats */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ApiResponse<UserListResponse>> getAllUsers() {
        log.info("Request received to retrieve all users.");
        List<UserEntity> userEntities = userService.getAllUsers();
        List<UserResponse> users = userEntities.stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());

        long activeUsers = userEntities.stream().filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).count();
        long adminUsers = userEntities.stream().filter(u -> {
            String r = u.getRoleName();
            return r != null && r.contains("ADMIN");
        }).count();

        UserEntity current = authHelper.requireCurrentUser();
        boolean canManageUsers = userService.hasPermission(current.getId(), "USER_UPDATE");

        UserListResponse resp = new UserListResponse(users, activeUsers, adminUsers, canManageUsers);
        log.info("Retrieved [{}] users successfully.", users.size());
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved", resp));
    }

    /** GET /user/user?id={id} — user by id */
    @GetMapping("/user")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@RequestParam Long id) {
        log.info("Request received to retrieve user [{}].", id);
        UserEntity user = userService.getById(id);
        log.info("User [{}] retrieved successfully.", id);
        return ResponseEntity.ok(ApiResponse.ok("User retrieved", UserResponse.from(user)));
    }

    /** GET /user/manage?id={id} — manage user details and options */
    @GetMapping("/manage")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ApiResponse<ManageUserResponse>> getManageUser(@RequestParam Long id) {
        log.info("Request received to manage user [{}] details.", id);
        UserEntity selectedUser = userService.getById(id);
        List<RoleResponse> roles = roleService.getAllRoles().stream()
                .map(RoleResponse::from)
                .collect(Collectors.toList());

        ManageUserResponse resp = new ManageUserResponse(UserResponse.from(selectedUser), roles);
        log.info("Manage user [{}] details retrieved successfully.", id);
        return ResponseEntity.ok(ApiResponse.ok("Manage user details retrieved", resp));
    }

    /** GET /user/logs/raw — raw application log stream */
    @GetMapping("/logs/raw")
    @PreAuthorize("hasAuthority('LOG_VIEW')")
    public ResponseEntity<String> getRawLogs() {
        UserEntity user = authHelper.requireCurrentUser();
        log.info("Raw log file requested by user [{}].", user.getId());
        String rawLogs = logParserService.getRawLog();
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"application.log\"")
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body(rawLogs);
    }

    /** GET /user/logs/structured — structured paginated logs and metrics */
    @GetMapping("/logs/structured")
    @PreAuthorize("hasAuthority('LOG_VIEW')")
    public ResponseEntity<ApiResponse<LogPageResponse>> getStructuredLogs(
            @RequestParam(value = "page", defaultValue = "0") int page) {
        UserEntity user = authHelper.requireCurrentUser();
        log.info("Structured log page [{}] requested by user [{}].", page, user.getId());

        List<LogEntry> allEntries = logParserService.parseLogFile();
        Map<String, Long> stats = logParserService.computeStats(allEntries);

        int pageSize = 7;
        int totalItems = allEntries.size();
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil(totalItems / (double) pageSize);

        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        int fromIndex = currentPage * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);

        List<LogEntry> pageEntries = fromIndex >= totalItems
                ? List.of()
                : allEntries.subList(fromIndex, toIndex);

        boolean hasPrev = currentPage > 0;
        boolean hasNext = currentPage < totalPages - 1;

        LogPageResponse resp = new LogPageResponse(
                pageEntries, stats, currentPage, totalPages, pageSize, totalItems, hasPrev, hasNext
        );
        return ResponseEntity.ok(ApiResponse.ok("Logs retrieved", resp));
    }

    /** GET /user/terms — terms page details */
    @GetMapping("/terms")
    public ResponseEntity<ApiResponse<LegalResponse>> getTerms() {
        LegalResponse resp = new LegalResponse(termsUpdated, contactEmail, companyName, null);
        return ResponseEntity.ok(ApiResponse.ok("Terms retrieved", resp));
    }

    /** GET /user/privacy — privacy policy page details */
    @GetMapping("/privacy")
    public ResponseEntity<ApiResponse<LegalResponse>> getPrivacy() {
        LegalResponse resp = new LegalResponse(privacyUpdated, contactEmail, companyName, null);
        return ResponseEntity.ok(ApiResponse.ok("Privacy policy retrieved", resp));
    }

    /** GET /user/cookie-policy — cookie policy page details */
    @GetMapping("/cookie-policy")
    public ResponseEntity<ApiResponse<LegalResponse>> getCookiePolicy() {
        LegalResponse resp = new LegalResponse(null, contactEmail, null, cookiePolicyUpdated);
        return ResponseEntity.ok(ApiResponse.ok("Cookie policy retrieved", resp));
    }

    /** POST /user/forgot-password — forgot password request */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new ValidationException("Email is required");
        }
        log.info("API password reset request received for email [{}].", req.getEmail());
        passwordResetService.initiateReset(req.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("If the email exists, a reset link has been sent."));
    }

    /** GET /user/reset-password/verify — verify reset token validity */
    @GetMapping("/reset-password/verify")
    public ResponseEntity<ApiResponse<PasswordResetVerifyResponse>> verifyResetToken(@RequestParam String token) {
        boolean valid = passwordResetService.validateToken(token).isPresent();
        PasswordResetVerifyResponse resp = new PasswordResetVerifyResponse(token, valid);
        return ResponseEntity.ok(ApiResponse.ok("Token verification completed", resp));
    }

    /** POST /user/reset-password — complete password reset workflow */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@RequestBody PublicResetPasswordRequest req) {
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            throw new ValidationException("Password must be at least 6 characters");
        }
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new ValidationException("Passwords do not match");
        }
        boolean success = passwordResetService.resetPassword(req.getToken(), req.getPassword());
        if (!success) {
            throw new ValidationException("Invalid or expired token");
        }
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully."));
    }

    /** POST /user/register — register new user */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody RegisterRequest req)
            throws MessagingException, IOException {
        log.info("User registration request received for email [{}].", req.getEmail());

        if (req.getName() == null || req.getName().isBlank()) {
            log.warn("User registration validation failed for email [{}]: name is required.", req.getEmail());
            throw new ValidationException("Name is required");
        }
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            log.warn("User registration validation failed for email [{}]: username is required.", req.getEmail());
            throw new ValidationException("Username is required");
        }
        if (req.getEmail() == null || !req.getEmail().contains("@")) {
            log.warn("User registration validation failed: invalid email address.");
            throw new ValidationException("Invalid email address");
        }
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            log.warn("User registration validation failed for email [{}]: password too short.", req.getEmail());
            throw new ValidationException("Password must be at least 6 characters");
        }

        UserEntity user = userService.register(
                req.getName(), req.getUsername(), req.getEmail(), req.getPassword(),
                req.getBusinessName(), req.getLocation(),
                req.getAwsAccountId(), req.getGcpProjectId(), req.getAzureSubscriptionId());
        log.info("User [{}] registered successfully.", user.getId());
        return ResponseEntity.status(201).body(ApiResponse.ok("Registration successful", UserResponse.from(user)));
    }

    /** POST /user/verify-otp — complete email verification */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<?>> verifyOtp(@RequestBody Map<String, String> body) {
        String otp = body.get("otp");
        if (otp == null || otp.isBlank()) {
            throw new ValidationException("Verification code is required");
        }
        UserEntity current = authHelper.requireCurrentUser();
        boolean success = userService.verifyOtp(current.getId(), otp);
        if (!success) {
            throw new ValidationException("Invalid or expired verification code");
        }
        return ResponseEntity.ok(ApiResponse.ok("Verification successful"));
    }

    /** POST /user/verify-otp/resend — resend verification OTP */
    @PostMapping("/verify-otp/resend")
    public ResponseEntity<ApiResponse<?>> resendOtp() throws MessagingException, IOException {
        UserEntity current = authHelper.requireCurrentUser();
        userService.resendOtp(current.getId());
        return ResponseEntity.ok(ApiResponse.ok("Verification code resent successfully"));
    }

    /** DELETE /user — delete user by ID */
    @DeleteMapping
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<ApiResponse<?>> deleteUser(@RequestParam Long id) {
        UserEntity current = authHelper.requireCurrentUser();
        if (current.getId().equals(id)) {
            throw new ValidationException("You cannot delete your own account");
        }
        UserEntity target = userService.getById(id);
        userService.deleteUser(id);
        log.info("User [{}] deleted by admin [{}].", target.getUsername(), current.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("User deleted successfully"));
    }
}