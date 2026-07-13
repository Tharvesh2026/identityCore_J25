package dev.tharbytes.identityCore.dto.request;

public class ResetPasswordRequest {
    private Long userId;
    private String newPassword;

    public ResetPasswordRequest() {}

    public ResetPasswordRequest(Long userId, String newPassword) {
        this.userId = userId;
        this.newPassword = newPassword;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
