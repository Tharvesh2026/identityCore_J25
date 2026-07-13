package dev.tharbytes.identityCore.controller.api;

import dev.tharbytes.identityCore.dto.request.*;
import dev.tharbytes.identityCore.dto.response.*;
import dev.tharbytes.identityCore.entity.*;
import dev.tharbytes.identityCore.exception.ValidationException;
import dev.tharbytes.identityCore.security.AuthHelper;
import dev.tharbytes.identityCore.service.*;
import dev.tharbytes.identityCore.util.ValidationUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        userService.updateStatus(req.getUserId(), req.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Status updated"));
    }

    /** POST /auth/update-role */
    @PostMapping("/update-role")
    public ResponseEntity<ApiResponse<?>> updateRole(@RequestBody UpdateRoleRequest req) {
        userService.updateRole(req.getUserId(), req.getRoleId());
        return ResponseEntity.ok(ApiResponse.ok("Role updated"));
    }

    /** POST /auth/reset-password */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(@RequestBody ResetPasswordRequest req) {
        if (req.getNewPassword() == null || req.getNewPassword().length() < 6)
            throw new ValidationException("Password must be at least 6 characters");
        userService.resetPassword(req.getUserId(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password reset"));
    }

    /** POST /auth/profile/update */
    @PostMapping("/profile/update")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@RequestBody UpdateProfileRequest req) {
        UserEntity current = authHelper.requireCurrentUser();

        String name = ValidationUtil.sanitizeName(req.getName());
        String username = ValidationUtil.sanitizeUsername(req.getUsername());
        String email = req.getEmail();

        if (name.isBlank() || username.isBlank() || email.isBlank())
            throw new ValidationException("All fields are required");
        if (!ValidationUtil.isValidEmail(email))
            throw new ValidationException("Invalid email address");

        UserEntity updated = userService.updateProfile(current.getId(), name, username, email);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", UserResponse.from(updated)));
    }

    /** POST /auth/profile/change-password */
    @PostMapping("/profile/change-password")
    public ResponseEntity<ApiResponse<?>> changePassword(@RequestBody ChangePasswordRequest req) {
        UserEntity current = authHelper.requireCurrentUser();

        if (req.getNewPassword() == null || req.getNewPassword().length() < 6)
            throw new ValidationException("Password must be at least 6 characters");
        if (!req.getNewPassword().equals(req.getConfirmPassword()))
            throw new ValidationException("Passwords do not match");

        userService.changePassword(current.getId(), req.getCurrentPassword(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    /** POST /auth/roles/create */
    @PostMapping("/roles/create")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@RequestBody CreateRoleRequest req) {
        RoleEntity role = roleService.createRole(req.getRoleName());
        return ResponseEntity.status(201).body(ApiResponse.ok("Role created", RoleResponse.from(role)));
    }

    /** POST /auth/roles/rename */
    @PostMapping("/roles/rename")
    public ResponseEntity<ApiResponse<?>> renameRole(
            @RequestParam Long roleId,
            @RequestParam String roleName) {
        roleService.renameRole(roleId, roleName);
        return ResponseEntity.ok(ApiResponse.ok("Role renamed"));
    }

    /** POST /auth/roles/activate */
    @PostMapping("/roles/activate")
    public ResponseEntity<ApiResponse<?>> activateRole(@RequestParam Long roleId) {
        roleService.updateStatus(roleId, "ACTIVE");
        return ResponseEntity.ok(ApiResponse.ok("Role activated"));
    }

    /** POST /auth/roles/deactivate */
    @PostMapping("/roles/deactivate")
    public ResponseEntity<ApiResponse<?>> deactivateRole(@RequestParam Long roleId) {
        roleService.updateStatus(roleId, "INACTIVE");
        return ResponseEntity.ok(ApiResponse.ok("Role deactivated"));
    }

    /** POST /auth/roles/update-permissions */
    @PostMapping("/roles/update-permissions")
    public ResponseEntity<ApiResponse<?>> updateRolePermissions(@RequestBody UpdateRolePermissionsRequest req) {
        roleService.updatePermissions(req.getRoleId(), req.getPermissionIds());
        return ResponseEntity.ok(ApiResponse.ok("Permissions updated"));
    }
}
