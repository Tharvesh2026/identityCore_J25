package dev.tharbytes.identityCore.dto.request;

public class UpdateRoleRequest {
    private Long userId;
    private Long roleId;

    public UpdateRoleRequest() {}

    public UpdateRoleRequest(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
