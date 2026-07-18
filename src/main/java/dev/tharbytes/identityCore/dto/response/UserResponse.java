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
    private String provider;
    private boolean verified;
    private String businessName;
    private String location;
    private String awsAccountId;
    private String gcpProjectId;
    private String azureSubscriptionId;
    private String avatarUrl;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    public UserResponse() {}

    public UserResponse(Long id, String name, String email, String username, String role, String status, List<String> permissions,
                        String provider, boolean verified, String businessName, String location,
                        String awsAccountId, String gcpProjectId, String azureSubscriptionId, String avatarUrl,
                        java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.username = username;
        this.role = role;
        this.status = status;
        this.permissions = permissions;
        this.provider = provider;
        this.verified = verified;
        this.businessName = businessName;
        this.location = location;
        this.awsAccountId = awsAccountId;
        this.gcpProjectId = gcpProjectId;
        this.azureSubscriptionId = azureSubscriptionId;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
            perms,
            u.getProvider(),
            u.isVerified(),
            u.getBusinessName(),
            u.getLocation(),
            u.getAwsAccountId(),
            u.getGcpProjectId(),
            u.getAzureSubscriptionId(),
            u.getAvatarUrl(),
            u.getCreatedAt(),
            u.getUpdatedAt()
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAwsAccountId() {
        return awsAccountId;
    }

    public void setAwsAccountId(String awsAccountId) {
        this.awsAccountId = awsAccountId;
    }

    public String getGcpProjectId() {
        return gcpProjectId;
    }

    public void setGcpProjectId(String gcpProjectId) {
        this.gcpProjectId = gcpProjectId;
    }

    public String getAzureSubscriptionId() {
        return azureSubscriptionId;
    }

    public void setAzureSubscriptionId(String azureSubscriptionId) {
        this.azureSubscriptionId = azureSubscriptionId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
