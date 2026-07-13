package dev.tharbytes.identityCore.dto.request;

public class CreateRoleRequest {
    private String roleName;

    public CreateRoleRequest() {}

    public CreateRoleRequest(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
