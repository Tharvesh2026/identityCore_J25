package dev.tharbytes.identityCore.controller.api;

import dev.tharbytes.identityCore.dto.request.*;
import dev.tharbytes.identityCore.dto.response.*;
import dev.tharbytes.identityCore.entity.*;
import dev.tharbytes.identityCore.exception.ValidationException;
import dev.tharbytes.identityCore.security.AuthHelper;
import dev.tharbytes.identityCore.service.*;
import dev.tharbytes.identityCore.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthApiController {

    private final UserService userService;
    private final RoleService roleService;
    private final AuthHelper authHelper;

    public AuthApiController(UserService userService, RoleService roleService, AuthHelper authHelper) {
        this.userService = userService;
        this.roleService = roleService;
        this.authHelper = authHelper;
    }

    /** POST /auth/update-status */
    @PostMapping("/update-status")
    public ResponseEntity<ApiResponse<?>> updateStatus(@RequestBody UpdateStatusRequest req) {
        log.info("Status update request received for user [{}].", req.getUserId());
        userService.updateStatus(req.getUserId(), req.getStatus());
        log.info("Status updated successfully for user [{}] to [{}].", req.getUserId(), req.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Status updated"));
    }

    /** POST /auth/update-role */
    @PostMapping("/update-role")
    public ResponseEntity<ApiResponse<?>> updateRole(@RequestBody UpdateRoleRequest req) {
        log.info("Role update request received for user [{}].", req.getUserId());
        userService.updateRole(req.getUserId(), req.getRoleId());
        log.info("Role [{}] updated successfully for user [{}].", req.getRoleId(), req.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Role updated"));
    }

    /** POST /auth/reset-password */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@RequestBody ResetPasswordRequest req) {
        log.info("Password reset request received for user [{}].", req.getUserId());
        if (req.getNewPassword() == null || req.getNewPassword().length() < 6) {
            log.warn("Password reset validation failed for user [{}].", req.getUserId());
            throw new ValidationException("Password must be at least 6 characters");
        }
        userService.resetPassword(req.getUserId(), req.getNewPassword());
        log.info("Password reset successfully for user [{}].", req.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Password reset"));
    }

    /** POST /auth/profile/update */
    @PostMapping("/profile/update")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@RequestBody UpdateProfileRequest req) {
        UserEntity current = authHelper.requireCurrentUser();
        log.info("Profile update request received for user [{}].", current.getId());

        String name = ValidationUtil.sanitizeName(req.getName());
        String username = ValidationUtil.sanitizeUsername(req.getUsername());
        String email = req.getEmail();

        if (name.isBlank() || username.isBlank() || email.isBlank()) {
            log.warn("Profile update validation failed for user [{}]: missing required fields.", current.getId());
            throw new ValidationException("All fields are required");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            log.warn("Profile update validation failed for user [{}]: invalid email address.", current.getId());
            throw new ValidationException("Invalid email address");
        }

        UserEntity updated = userService.updateProfile(current.getId(), name, username, email);
        log.info("Profile updated successfully for user [{}].", current.getId());
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", UserResponse.from(updated)));
    }

    /** POST /auth/profile/change-password */
    @PostMapping("/profile/change-password")
    public ResponseEntity<ApiResponse<?>> changePassword(@RequestBody ChangePasswordRequest req) {
        UserEntity current = authHelper.requireCurrentUser();
        log.info("Password change request received for user [{}].", current.getId());

        if (req.getNewPassword() == null || req.getNewPassword().length() < 6) {
            log.warn("Password change validation failed for user [{}]: password too short.", current.getId());
            throw new ValidationException("Password must be at least 6 characters");
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            log.warn("Password change validation failed for user [{}]: passwords do not match.", current.getId());
            throw new ValidationException("Passwords do not match");
        }

        userService.changePassword(current.getId(), req.getCurrentPassword(), req.getNewPassword());
        log.info("Password changed successfully for user [{}].", current.getId());
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    /** POST /auth/roles/create */
    @PostMapping("/roles/create")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@RequestBody CreateRoleRequest req) {
        log.info("Role creation request received for role name [{}].", req.getRoleName());
        RoleEntity role = roleService.createRole(req.getRoleName());
        log.info("Role [{}] created successfully.", role.getId());
        return ResponseEntity.status(201).body(ApiResponse.ok("Role created", RoleResponse.from(role)));
    }

    /** POST /auth/roles/rename */
    @PostMapping("/roles/rename")
    public ResponseEntity<ApiResponse<?>> renameRole(
            @RequestParam Long roleId,
            @RequestParam String roleName) {
        log.info("Role rename request received for role [{}].", roleId);
        roleService.renameRole(roleId, roleName);
        log.info("Role [{}] renamed successfully to [{}].", roleId, roleName);
        return ResponseEntity.ok(ApiResponse.ok("Role renamed"));
    }

    /** POST /auth/roles/activate */
    @PostMapping("/roles/activate")
    public ResponseEntity<ApiResponse<?>> activateRole(@RequestParam Long roleId) {
        log.info("Role activation request received for role [{}].", roleId);
        roleService.updateStatus(roleId, "ACTIVE");
        log.info("Role [{}] activated successfully.", roleId);
        return ResponseEntity.ok(ApiResponse.ok("Role activated"));
    }

    /** POST /auth/roles/deactivate */
    @PostMapping("/roles/deactivate")
    public ResponseEntity<ApiResponse<?>> deactivateRole(@RequestParam Long roleId) {
        log.info("Role deactivation request received for role [{}].", roleId);
        roleService.updateStatus(roleId, "INACTIVE");
        log.info("Role [{}] deactivated successfully.", roleId);
        return ResponseEntity.ok(ApiResponse.ok("Role deactivated"));
    }

    /** POST /auth/roles/update-permissions */
    @PostMapping("/roles/update-permissions")
    public ResponseEntity<ApiResponse<?>> updateRolePermissions(@RequestBody UpdateRolePermissionsRequest req) {
        log.info("Permission update request received for role [{}].", req.getRoleId());
        roleService.updatePermissions(req.getRoleId(), req.getPermissionIds());
        log.info("Permissions updated successfully for role [{}].", req.getRoleId());
        return ResponseEntity.ok(ApiResponse.ok("Permissions updated"));
    }
}