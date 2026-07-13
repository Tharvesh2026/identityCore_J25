package dev.tharbytes.identityCore.dto.request;

import java.util.List;

public class UpdateRolePermissionsRequest {
    private Long roleId;
    private List<Long> permissionIds;

    public UpdateRolePermissionsRequest() {}

    public UpdateRolePermissionsRequest(Long roleId, List<Long> permissionIds) {
        this.roleId = roleId;
        this.permissionIds = permissionIds;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public List<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Long> permissionIds) {
        this.permissionIds = permissionIds;
    }
}
