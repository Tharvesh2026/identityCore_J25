package dev.tharbytes.identityCore.controller;

import dev.tharbytes.identityCore.entity.*;
import dev.tharbytes.identityCore.security.AuthHelper;
import dev.tharbytes.identityCore.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final RoleService roleService;
    private final AuthHelper authHelper;

    public UserController(UserService userService, RoleService roleService, AuthHelper authHelper) {
        this.userService = userService;
        this.roleService = roleService;
        this.authHelper = authHelper;
    }

    /** GET /users — list all users */
    @GetMapping("/users")
    public String users(Model model) {
        List<UserEntity> users = userService.getAllUsers();
        model.addAttribute("users", users);

        long activeUsers = users.stream().filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).count();
        long adminUsers = users.stream().filter(u -> {
            String r = u.getRoleName();
            return r != null && (r.contains("ADMIN"));
        }).count();

        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("adminUsers", adminUsers);

        UserEntity current = authHelper.requireCurrentUser();
        model.addAttribute("user", current);
        model.addAttribute("canManageUsers", userService.hasPermission(current.getId(), "USER_UPDATE"));
        return "users";
    }

    /** GET /manage-user?id={id} — manage user detail page */
    @GetMapping("/manage-user")
    public String manageUser(@RequestParam Long id, Model model) {
        UserEntity selectedUser = userService.getById(id);
        List<RoleEntity> roles = roleService.getAllRoles();
        model.addAttribute("selectedUser", selectedUser);
        model.addAttribute("roles", roles);
        return "manage-user";
    }

    /** POST /manage-user — handle updateRole / updateStatus / resetPassword */
    @PostMapping("/manage-user")
    public String manageUserPost(
            @RequestParam Long id,
            @RequestParam String action,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String newPassword,
            RedirectAttributes ra) {

        UserEntity currentUser = authHelper.requireCurrentUser();

        switch (action) {
            case "updateRole" -> {
                userService.updateRole(id, roleId);
                ra.addFlashAttribute("success", "Role updated successfully");
            }
            case "updateStatus" -> {
                UserEntity target = userService.getById(id);
                userService.updateStatus(id, status);
                log.info("ACTION=STATUS_UPDATE | BY={} | TARGET={} | OLD={} | NEW={}",
                    currentUser.getUsername(), target.getUsername(), target.getStatus(), status);
                ra.addFlashAttribute("success", "Status updated successfully");
            }
            case "resetPassword" -> {
                if (newPassword == null || newPassword.length() < 6) {
                    ra.addFlashAttribute("error", "Password must be at least 6 characters");
                    return "redirect:/manage-user?id=" + id;
                }
                UserEntity target = userService.getById(id);
                userService.resetPassword(id, newPassword);
                log.info("ACTION=PASSWORD_RESET | BY={} | TARGET={}", currentUser.getUsername(), target.getUsername());
                ra.addFlashAttribute("success", "Password reset successfully");
            }
            default -> ra.addFlashAttribute("error", "Invalid action");
        }

        return "redirect:/manage-user?id=" + id;
    }

    /** POST /register — register new user */
    @PostMapping("/register")
    public String register(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String username,
            @RequestParam String password,
            RedirectAttributes ra) {

        if (name == null || name.isBlank()) { ra.addFlashAttribute("error", "Name is required"); return "redirect:/login?tab=register"; }
        if (username == null || username.isBlank()) { ra.addFlashAttribute("error", "Username is required"); return "redirect:/login?tab=register"; }
        if (email == null || !email.contains("@")) { ra.addFlashAttribute("error", "Invalid email address"); return "redirect:/login?tab=register"; }
        if (password == null || password.length() < 6) { ra.addFlashAttribute("error", "Password must be at least 6 characters"); return "redirect:/login?tab=register"; }

        userService.register(name, username, email, password);
        ra.addFlashAttribute("success", "Registration successful. Please login.");
        log.info("User registered: {}", username);
        return "redirect:/login";
    }
}
