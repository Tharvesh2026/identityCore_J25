package dev.tharbytes.identityCore.dto.response;

public class PasswordResetVerifyResponse {
    private String token;
    private boolean valid;

    public PasswordResetVerifyResponse() {}

    public PasswordResetVerifyResponse(String token, boolean valid) {
        this.token = token;
        this.valid = valid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
