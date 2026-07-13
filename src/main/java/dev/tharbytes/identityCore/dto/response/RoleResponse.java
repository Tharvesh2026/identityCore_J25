package dev.tharbytes.identityCore.dto.response;

import dev.tharbytes.identityCore.entity.RoleEntity;
import java.util.List;
import java.util.stream.Collectors;

public class RoleResponse {
    private Long id;
    private String roleName;
    private String status;
    private List<String> permissions;

    public RoleResponse() {}

    public RoleResponse(Long id, String roleName, String status, List<String> permissions) {
        this.id = id;
        this.roleName = roleName;
        this.status = status;
        this.permissions = permissions;
    }

    public static RoleResponse from(RoleEntity r) {
        List<String> perms = r.getPermissions().stream()
            .map(p -> p.getPermissionKey())
            .sorted()
            .collect(Collectors.toList());

        return new RoleResponse(
            r.getId(),
            r.getRoleName(),
            r.getStatus(),
            perms
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
