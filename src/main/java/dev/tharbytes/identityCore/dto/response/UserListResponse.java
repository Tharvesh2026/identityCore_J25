package dev.tharbytes.identityCore.dto.response;

import java.util.List;

public class UserListResponse {
    private List<UserResponse> users;
    private long activeUsers;
    private long adminUsers;
    private boolean canManageUsers;

    public UserListResponse() {}

    public UserListResponse(List<UserResponse> users, long activeUsers, long adminUsers, boolean canManageUsers) {
        this.users = users;
        this.activeUsers = activeUsers;
        this.adminUsers = adminUsers;
        this.canManageUsers = canManageUsers;
    }

    public List<UserResponse> getUsers() {
        return users;
    }

    public void setUsers(List<UserResponse> users) {
        this.users = users;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getAdminUsers() {
        return adminUsers;
    }

    public void setAdminUsers(long adminUsers) {
        this.adminUsers = adminUsers;
    }

    public boolean isCanManageUsers() {
        return canManageUsers;
    }

    public void setCanManageUsers(boolean canManageUsers) {
        this.canManageUsers = canManageUsers;
    }
}
