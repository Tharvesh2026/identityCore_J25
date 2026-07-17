package dev.tharbytes.identityCore.controller.api;

import dev.tharbytes.identityCore.dto.request.*;
import dev.tharbytes.identityCore.dto.response.*;
import dev.tharbytes.identityCore.entity.*;
import dev.tharbytes.identityCore.exception.ValidationException;
import dev.tharbytes.identityCore.security.AuthHelper;
import dev.tharbytes.identityCore.service.*;
import dev.tharbytes.identityCore.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthApiController {

    private static final Logger log = LoggerFactory.getLogger(AuthApiController.class);

    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final AuthHelper authHelper;

    public AuthApiController(UserService userService,
                             RoleService roleService,
                             PermissionService permissionService,
                             AuthHelper authHelper) {
        this.userService = userService;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.authHelper = authHelper;
    }

    /** POST /auth/update-status */
    @PostMapping("/update-status")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<?>> updateStatus(@RequestBody UpdateStatusRequest req) {
        log.info("Status update request received for user [{}].", req.getUserId());
        userService.updateStatus(req.getUserId(), req.getStatus());
        log.info("Status updated successfully for user [{}] to [{}].", req.getUserId(), req.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Status updated"));
    }

    /** POST /auth/update-role */
    @PostMapping("/update-role")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResponse<?>> updateRole(@RequestBody UpdateRoleRequest req) {
        log.info("Role update request received for user [{}].", req.getUserId());
        userService.updateRole(req.getUserId(), req.getRoleId());
        log.info("Role [{}] updated successfully for user [{}].", req.getRoleId(), req.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Role updated"));
    }

    /** POST /auth/reset-password */
    @PostMapping("/reset-password")
    @PreAuthorize("hasAuthority('USER_PASSWORD_RESET')")
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@RequestBody UpdateProfileRequest req) {
        UserEntity current = authHelper.requireCurrentUser();
        log.info("Profile update request received for user [{}].", current.getId());

        String name = ValidationUtil.sanitizeName(req.getName());
        String username = ValidationUtil.sanitizeUsername(req.getUsername());
        String email = req.getEmail();

        if (name.isBlank() || username.isBlank() || email == null || email.isBlank()) {
            log.warn("Profile update validation failed for user [{}]: missing required fields.", current.getId());
            throw new ValidationException("All fields are required");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            log.warn("Profile update validation failed for user [{}]: invalid email address.", current.getId());
            throw new ValidationException("Invalid email address");
        }
        if (username.length() < 3 || username.length() > 30) {
            log.warn("Profile update validation failed for user [{}]: username length out of range.", current.getId());
            throw new ValidationException("Username must be 3-30 characters");
        }

        UserEntity updated = userService.updateProfile(current.getId(), name, username, email);
        log.info("Profile updated successfully for user [{}].", current.getId());
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", UserResponse.from(updated)));
    }

    /** POST /auth/profile/change-password */
    @PostMapping("/profile/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> changePassword(@RequestBody ChangePasswordRequest req) {
        UserEntity current = authHelper.requireCurrentUser();
        log.info("Password change request received for user [{}].", current.getId());

        if (isBlank(req.getCurrentPassword()) || isBlank(req.getNewPassword()) || isBlank(req.getConfirmPassword())) {
            log.warn("Password change validation failed for user [{}]: missing required fields.", current.getId());
            throw new ValidationException("All password fields are required");
        }
        if (req.getNewPassword().length() < 6) {
            log.warn("Password change validation failed for user [{}]: password too short.", current.getId());
            throw new ValidationException("Password must be at least 6 characters");
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            log.warn("Password change validation failed for user [{}]: passwords do not match.", current.getId());
            throw new ValidationException("Passwords do not match");
        }
        if (req.getNewPassword().equals(req.getCurrentPassword())) {
            log.warn("Password change validation failed for user [{}]: new password matches current password.", current.getId());
            throw new ValidationException("New password must differ from current password");
        }

        userService.changePassword(current.getId(), req.getCurrentPassword(), req.getNewPassword());
        log.info("Password changed successfully for user [{}].", current.getId());
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    /** GET /auth/roles */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        log.info("Request received to retrieve all roles.");
        List<RoleResponse> roles = roleService.getAllRoles().stream()
                .map(RoleResponse::from)
                .collect(Collectors.toList());
        log.info("Retrieved [{}] roles successfully.", roles.size());
        return ResponseEntity.ok(ApiResponse.ok("Roles retrieved", roles));
    }

    /** GET /auth/roles/manage */
    @GetMapping("/roles/manage")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<ApiResponse<ManageRoleResponse>> getManageRole(@RequestParam Long id) {
        log.info("Request received to manage role [{}] details.", id);
        RoleEntity role = roleService.getById(id);
        List<PermissionResponse> allPermissions = permissionService.getAllPermissions().stream()
                .map(PermissionResponse::from)
                .collect(Collectors.toList());
        List<Long> assignedPermissionIds = role.getPermissions().stream()
                .map(PermissionEntity::getId)
                .collect(Collectors.toList());

        ManageRoleResponse resp = new ManageRoleResponse(
                RoleResponse.from(role),
                allPermissions,
                assignedPermissionIds
        );
        log.info("Manage role [{}] details retrieved successfully.", id);
        return ResponseEntity.ok(ApiResponse.ok("Manage role details retrieved", resp));
    }

    /** POST /auth/roles/create */
    @PostMapping("/roles/create")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@RequestBody CreateRoleRequest req) {
        if (req.getRoleName() == null || req.getRoleName().isBlank()) {
            log.warn("Role creation validation failed: role name is required.");
            throw new ValidationException("Role name is required");
        }
        String normalizedRoleName = req.getRoleName().trim().toUpperCase().replaceAll("\\s+", "_");
        log.info("Role creation request received for role name [{}].", normalizedRoleName);
        RoleEntity role = roleService.createRole(normalizedRoleName);
        log.info("Role [{}] created successfully.", role.getId());
        return ResponseEntity.status(201).body(ApiResponse.ok("Role created", RoleResponse.from(role)));
    }

    /** POST /auth/roles/rename */
    @PostMapping("/roles/rename")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
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
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<?>> activateRole(@RequestParam Long roleId) {
        log.info("Role activation request received for role [{}].", roleId);
        roleService.updateStatus(roleId, "ACTIVE");
        log.info("Role [{}] activated successfully.", roleId);
        return ResponseEntity.ok(ApiResponse.ok("Role activated"));
    }

    /** POST /auth/roles/deactivate */
    @PostMapping("/auth/roles/deactivate")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<?>> deactivateRole(@RequestParam Long roleId) {
        log.info("Role deactivation request received for role [{}].", roleId);
        roleService.updateStatus(roleId, "INACTIVE");
        log.info("Role [{}] deactivated successfully.", roleId);
        return ResponseEntity.ok(ApiResponse.ok("Role deactivated"));
    }

    /** POST /auth/roles/update-permissions */
    @PostMapping("/roles/update-permissions")
    @PreAuthorize("hasAuthority('ROLE_PERMISSION_MANAGE')")
    public ResponseEntity<ApiResponse<?>> updateRolePermissions(@RequestBody UpdateRolePermissionsRequest req) {
        log.info("Permission update request received for role [{}].", req.getRoleId());
        roleService.updatePermissions(req.getRoleId(), req.getPermissionIds());
        log.info("Permissions updated successfully for role [{}].", req.getRoleId());
        return ResponseEntity.ok(ApiResponse.ok("Permissions updated"));
    }

    /** POST /auth/logout — API logout endpoint */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(HttpServletRequest request, HttpServletResponse response) {
        log.info("API logout requested.");
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) {
            log.info("Invalidating session [{}].", session.getId());
            session.invalidate();
        }
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        Cookie cookieJ = new Cookie("JSESSIONID", null);
        cookieJ.setPath("/");
        cookieJ.setMaxAge(0);
        response.addCookie(cookieJ);

        Cookie cookieR = new Cookie("ICORE_REMEMBER_ME", null);
        cookieR.setPath("/");
        cookieR.setMaxAge(0);
        response.addCookie(cookieR);

        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}