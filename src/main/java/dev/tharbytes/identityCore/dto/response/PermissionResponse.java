package dev.tharbytes.identityCore.dto.response;

import dev.tharbytes.identityCore.entity.PermissionEntity;

public class PermissionResponse {
    private Long id;
    private String permissionKey;
    private String description;

    public PermissionResponse() {}

    public PermissionResponse(Long id, String permissionKey, String description) {
        this.id = id;
        this.permissionKey = permissionKey;
        this.description = description;
    }

    public static PermissionResponse from(PermissionEntity p) {
        return new PermissionResponse(
            p.getId(),
            p.getPermissionKey(),
            p.getDescription()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPermissionKey() {
        return permissionKey;
    }

    public void setPermissionKey(String permissionKey) {
        this.permissionKey = permissionKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
