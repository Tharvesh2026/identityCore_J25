package dev.tharbytes.identityCore.service;

import dev.tharbytes.identityCore.entity.PermissionEntity;
import dev.tharbytes.identityCore.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public List<PermissionEntity> getAllPermissions() {
        return permissionRepository.findAllByOrderByPermissionKeyAsc();
    }
}
