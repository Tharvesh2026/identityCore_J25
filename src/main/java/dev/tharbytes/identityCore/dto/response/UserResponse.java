package dev.tharbytes.identityCore.dto.response;

import dev.tharbytes.identityCore.entity.UserEntity;
import java.util.List;
import java.util.stream.Collectors;

public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String username;
    private String role;
    private String status;
    private List<String> permissions;

    public UserResponse() {}

    public UserResponse(Long id, String name, String email, String username, String role, String status, List<String> permissions) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.username = username;
        this.role = role;
        this.status = status;
        this.permissions = permissions;
    }

    public static UserResponse from(UserEntity u) {
        List<String> perms = null;
        if (u.getRole() != null && u.getRole().getPermissions() != null) {
            perms = u.getRole().getPermissions().stream()
                    .map(p -> p.getPermissionKey())
                    .sorted()
                    .collect(Collectors.toList());
        }
        return new UserResponse(
            u.getId(),
            u.getName(),
            u.getMailId(),
            u.getUsername(),
            u.getRoleName(),
            u.getStatus(),
            perms
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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
