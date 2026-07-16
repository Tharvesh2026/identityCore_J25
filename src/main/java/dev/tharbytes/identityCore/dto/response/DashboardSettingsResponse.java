package dev.tharbytes.identityCore.dto.response;

public class DashboardSettingsResponse {
    private UserResponse user;
    private String sessionId;
    private long sessionCreation;
    private long sessionLastAccess;
    private int sessionTimeout;

    public DashboardSettingsResponse() {}

    public DashboardSettingsResponse(UserResponse user, String sessionId, long sessionCreation, long sessionLastAccess, int sessionTimeout) {
        this.user = user;
        this.sessionId = sessionId;
        this.sessionCreation = sessionCreation;
        this.sessionLastAccess = sessionLastAccess;
        this.sessionTimeout = sessionTimeout;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getSessionCreation() {
        return sessionCreation;
    }

    public void setSessionCreation(long sessionCreation) {
        this.sessionCreation = sessionCreation;
    }

    public long getSessionLastAccess() {
        return sessionLastAccess;
    }

    public void setSessionLastAccess(long sessionLastAccess) {
        this.sessionLastAccess = sessionLastAccess;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
}
