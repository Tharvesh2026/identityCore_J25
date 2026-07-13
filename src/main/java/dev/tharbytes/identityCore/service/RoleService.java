package dev.tharbytes.identityCore.service;

import dev.tharbytes.identityCore.entity.*;
import dev.tharbytes.identityCore.exception.*;
import dev.tharbytes.identityCore.repository.*;
import dev.tharbytes.identityCore.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public List<RoleEntity> getAllRoles() {
        return roleRepository.findAll();
    }

    public RoleEntity getById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    @Transactional
    public RoleEntity createRole(String roleName) {
        String sanitized = ValidationUtil.sanitizeRoleName(roleName);
        if (sanitized.isBlank()) throw new ValidationException("Role name is required");
        if (roleRepository.existsByRoleName(sanitized)) {
            throw new ValidationException("Role already exists: " + sanitized);
        }
        // Grant PROFILE_READ by default
        PermissionEntity profileRead = permissionRepository.findByPermissionKey("PROFILE_READ").orElse(null);
        RoleEntity role = new RoleEntity(sanitized, "ACTIVE");
        if (profileRead != null) role.getPermissions().add(profileRead);
        return roleRepository.save(role);
    }

    @Transactional
    public void renameRole(Long roleId, String newName) {
        RoleEntity role = getById(roleId);
        String sanitized = ValidationUtil.sanitizeRoleName(newName);
        if (sanitized.isBlank()) throw new ValidationException("Role name is required");
        role.setRoleName(sanitized);
        roleRepository.save(role);
        log.info("Role {} renamed to {}", roleId, sanitized);
    }

    @Transactional
    public void updateStatus(Long roleId, String status) {
        RoleEntity role = getById(roleId);
        role.setStatus(status);
        roleRepository.save(role);
        log.info("Role {} status updated to {}", roleId, status);
    }

    @Transactional
    public void updatePermissions(Long roleId, List<Long> permissionIds) {
        RoleEntity role = getById(roleId);
        Set<PermissionEntity> newPerms = new HashSet<>();
        if (permissionIds != null) {
            for (Long pid : permissionIds) {
                permissionRepository.findById(pid).ifPresent(newPerms::add);
            }
        }
        role.setPermissions(newPerms);
        roleRepository.save(role);
        log.info("Role {} permissions updated, count={}", roleId, newPerms.size());
    }
}
