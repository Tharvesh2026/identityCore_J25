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
public class RoleController {

    private static final Logger log = LoggerFactory.getLogger(RoleController.class);

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final AuthHelper authHelper;

    public RoleController(RoleService roleService, PermissionService permissionService, AuthHelper authHelper) {
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.authHelper = authHelper;
    }

    /** GET /roles — list all roles */
    @GetMapping("/roles")
    public String roles(Model model) {
        model.addAttribute("roles", roleService.getAllRoles());

        UserEntity current = authHelper.requireCurrentUser();
        model.addAttribute("user", current);
        return "roles";
    }

    /** POST /roles — create new role */
    @PostMapping("/roles")
    public String createRole(@RequestParam String roleName, RedirectAttributes ra) {
        if (roleName == null || roleName.isBlank()) {
            log.warn("Role creation validation failed: role name is required.");
            ra.addFlashAttribute("error", "Role name is required");
            return "redirect:/roles";
        }
        roleService.createRole(roleName.trim().toUpperCase());
        log.info("Role [{}] created successfully.", roleName.trim().toUpperCase());
        ra.addFlashAttribute("success", "Role created successfully");
        return "redirect:/roles";
    }

    /** POST /roles/rename */
    @PostMapping("/roles/rename")
    public String renameRole(
            @RequestParam Long roleId,
            @RequestParam String roleName,
            RedirectAttributes ra) {
        roleService.renameRole(roleId, roleName);
        log.info("Role [{}] renamed successfully to [{}].", roleId, roleName);
        ra.addFlashAttribute("success", "Role renamed successfully");
        return "redirect:/roles";
    }

    /** POST /roles/status */
    @PostMapping("/roles/status")
    public String updateStatus(
            @RequestParam Long roleId,
            @RequestParam String status,
            RedirectAttributes ra) {
        roleService.updateStatus(roleId, status);
        log.info("Role [{}] status updated to [{}].", roleId, status);
        ra.addFlashAttribute("success", "Role status updated");
        return "redirect:/roles";
    }

    /** GET /manage-role?id={id} */
    @GetMapping("/manage-role")
    public String manageRole(@RequestParam Long id, Model model) {
        RoleEntity role = roleService.getById(id);
        List<PermissionEntity> allPermissions = permissionService.getAllPermissions();
        List<Long> assignedPermissionIds = role.getPermissions().stream()
                .map(p -> p.getId())
                .toList();

        model.addAttribute("role", role);
        model.addAttribute("permissions", allPermissions);
        model.addAttribute("assignedPermissionIds", assignedPermissionIds);

        UserEntity current = authHelper.requireCurrentUser();
        model.addAttribute("user", current);
        return "manage-role";
    }

    /** POST /manage-role — update permissions */
    @PostMapping("/manage-role")
    public String updatePermissions(
            @RequestParam Long roleId,
            @RequestParam(required = false) List<Long> permissionIds,
            RedirectAttributes ra) {
        roleService.updatePermissions(roleId, permissionIds);
        log.info("Permissions updated successfully for role [{}].", roleId);
        ra.addFlashAttribute("success", "Permissions updated successfully");
        return "redirect:/manage-role?id=" + roleId;
    }
}