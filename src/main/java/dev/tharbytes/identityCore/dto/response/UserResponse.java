package dev.tharbytes.identityCore.dto.response;

import dev.tharbytes.identityCore.entity.UserEntity;

public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String username;
    private String role;
    private String status;

    public UserResponse() {}

    public UserResponse(Long id, String name, String email, String username, String role, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.username = username;
        this.role = role;
        this.status = status;
    }

    public static UserResponse from(UserEntity u) {
        return new UserResponse(
            u.getId(),
            u.getName(),
            u.getMailId(),
            u.getUsername(),
            u.getRoleName(),
            u.getStatus()
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
}
