package dev.tharbytes.identityCore.controller.api;

import dev.tharbytes.identityCore.dto.request.RegisterRequest;
import dev.tharbytes.identityCore.dto.response.ApiResponse;
import dev.tharbytes.identityCore.dto.response.UserResponse;
import dev.tharbytes.identityCore.entity.UserEntity;
import dev.tharbytes.identityCore.exception.ValidationException;
import dev.tharbytes.identityCore.security.AppUserDetailsService;
import dev.tharbytes.identityCore.security.AuthHelper;
import dev.tharbytes.identityCore.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
public class UserApiController {

    private final UserService userService;
    private final AuthHelper authHelper;


    private static final Logger log =
            LoggerFactory.getLogger(AppUserDetailsService.class);


    public UserApiController(UserService userService, AuthHelper authHelper) {
        this.userService = userService;
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
    @GetMapping("/profile/details")
    public ResponseEntity<ApiResponse<UserResponse>> getProfileDetails() {
        UserEntity user = authHelper.requireCurrentUser();
        log.info("Profile details retrieved successfully for user [{}].", user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Profile details", UserResponse.from(user)));
    }

    /** GET /user/users — all users */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        log.info("Request received to retrieve all users.");
        List<UserResponse> users = userService.getAllUsers().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        log.info("Retrieved [{}] users successfully.", users.size());
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved", users));
    }

    /** GET /user/user?id={id} — user by id */
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@RequestParam Long id) {
        log.info("Request received to retrieve user [{}].", id);
        UserEntity user = userService.getById(id);
        log.info("User [{}] retrieved successfully.", id);
        return ResponseEntity.ok(ApiResponse.ok("User retrieved", UserResponse.from(user)));
    }

    /** POST /user/register — register new user */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody RegisterRequest req) {
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
                req.getName(), req.getUsername(), req.getEmail(), req.getPassword());
        log.info("User [{}] registered successfully.", user.getId());
        return ResponseEntity.status(201).body(ApiResponse.ok("Registration successful", UserResponse.from(user)));
    }
}