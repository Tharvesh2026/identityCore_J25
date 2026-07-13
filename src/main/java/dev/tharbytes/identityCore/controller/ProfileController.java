package dev.tharbytes.identityCore.controller;

import dev.tharbytes.identityCore.entity.UserEntity;
import dev.tharbytes.identityCore.security.AuthHelper;
import dev.tharbytes.identityCore.service.*;
import dev.tharbytes.identityCore.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final UserService userService;
    private final PermissionService permissionService;
    private final AuthHelper authHelper;

    public ProfileController(UserService userService, PermissionService permissionService, AuthHelper authHelper) {
        this.userService = userService;
        this.permissionService = permissionService;
        this.authHelper = authHelper;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        UserEntity user = authHelper.requireCurrentUser();
        model.addAttribute("user", user);
        model.addAttribute("userPermissions", userService.getPermissions(user.getId()));
        return "profile";
    }

    @PostMapping("/profile")
    public String profilePost(
            @RequestParam String action,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmPassword,
            RedirectAttributes ra) {

        UserEntity user = authHelper.requireCurrentUser();

        if ("updateProfile".equals(action)) {
            return handleUpdateProfile(user, name, username, email, ra);
        } else if ("changePassword".equals(action)) {
            return handleChangePassword(user, currentPassword, newPassword, confirmPassword, ra);
        }

        ra.addFlashAttribute("error", "Unknown action");
        return "redirect:/profile";
    }

    private String handleUpdateProfile(UserEntity user, String name, String username, String email, RedirectAttributes ra) {
        String sanitizedName     = ValidationUtil.sanitizeName(name);
        String sanitizedUsername = ValidationUtil.sanitizeUsername(username);
        String sanitizedEmail    = email;

        if (sanitizedName.isBlank() || sanitizedUsername.isBlank() || sanitizedEmail.isBlank()) {
            ra.addFlashAttribute("error", "All fields are required.");
            ra.addFlashAttribute("tab", "edit");
            return "redirect:/profile";
        }
        if (!ValidationUtil.isValidEmail(sanitizedEmail)) {
            ra.addFlashAttribute("error", "Invalid email address.");
            ra.addFlashAttribute("tab", "edit");
            return "redirect:/profile";
        }
        if (sanitizedUsername.length() < 3 || sanitizedUsername.length() > 30) {
            ra.addFlashAttribute("error", "Username must be 3-30 characters.");
            ra.addFlashAttribute("tab", "edit");
            return "redirect:/profile";
        }

        userService.updateProfile(user.getId(), sanitizedName, sanitizedUsername, sanitizedEmail);
        ra.addFlashAttribute("success", "Profile updated successfully.");
        log.info("Profile updated for user {}", user.getUsername());
        return "redirect:/profile";
    }

    private String handleChangePassword(UserEntity user, String currentPassword, String newPassword, String confirmPassword, RedirectAttributes ra) {
        if (isBlank(currentPassword) || isBlank(newPassword) || isBlank(confirmPassword)) {
            ra.addFlashAttribute("error", "All password fields are required.");
            ra.addFlashAttribute("tab", "password");
            return "redirect:/profile";
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New passwords do not match.");
            ra.addFlashAttribute("tab", "password");
            return "redirect:/profile";
        }
        if (newPassword.length() < 6) {
            ra.addFlashAttribute("error", "New password must be at least 6 characters.");
            ra.addFlashAttribute("tab", "password");
            return "redirect:/profile";
        }
        if (newPassword.equals(currentPassword)) {
            ra.addFlashAttribute("error", "New password must differ from current password.");
            ra.addFlashAttribute("tab", "password");
            return "redirect:/profile";
        }

        userService.changePassword(user.getId(), currentPassword, newPassword);
        ra.addFlashAttribute("success", "Password changed successfully.");
        return "redirect:/profile";
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
