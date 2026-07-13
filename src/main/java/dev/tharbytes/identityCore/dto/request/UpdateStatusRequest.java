package dev.tharbytes.identityCore.dto.request;

public class UpdateStatusRequest {
    private Long userId;
    private String status;

    public UpdateStatusRequest() {}

    public UpdateStatusRequest(Long userId, String status) {
        this.userId = userId;
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
