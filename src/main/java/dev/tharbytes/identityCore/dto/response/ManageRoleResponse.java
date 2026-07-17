package dev.tharbytes.identityCore.dto.response;

import java.util.List;

public class ManageRoleResponse {
    private RoleResponse role;
    private List<PermissionResponse> permissions;
    private List<Long> assignedPermissionIds;

    public ManageRoleResponse() {}

    public ManageRoleResponse(RoleResponse role, List<PermissionResponse> permissions, List<Long> assignedPermissionIds) {
        this.role = role;
        this.permissions = permissions;
        this.assignedPermissionIds = assignedPermissionIds;
    }

    public RoleResponse getRole() {
        return role;
    }

    public void setRole(RoleResponse role) {
        this.role = role;
    }

    public List<PermissionResponse> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionResponse> permissions) {
        this.permissions = permissions;
    }

    public List<Long> getAssignedPermissionIds() {
        return assignedPermissionIds;
    }

    public void setAssignedPermissionIds(List<Long> assignedPermissionIds) {
        this.assignedPermissionIds = assignedPermissionIds;
    }
}
