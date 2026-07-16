package dev.tharbytes.identityCore.dto.response;

import java.util.List;

public class ManageUserResponse {
    private UserResponse selectedUser;
    private List<RoleResponse> roles;

    public ManageUserResponse() {}

    public ManageUserResponse(UserResponse selectedUser, List<RoleResponse> roles) {
        this.selectedUser = selectedUser;
        this.roles = roles;
    }

    public UserResponse getSelectedUser() {
        return selectedUser;
    }

    public void setSelectedUser(UserResponse selectedUser) {
        this.selectedUser = selectedUser;
    }

    public List<RoleResponse> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleResponse> roles) {
        this.roles = roles;
    }
}
