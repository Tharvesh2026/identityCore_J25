package dev.tharbytes.identityCore.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreateOAuth2ClientRequest {

    @NotBlank(message = "Client name is required")
    private String clientName;

    private String clientId; // Optional; auto-generated if blank

    private String redirectUris; // Comma-separated or newline-separated

    private String postLogoutRedirectUris; // Comma-separated or newline-separated

    private List<String> scopes;

    private boolean requirePkce = true;

    private long accessTokenTimeToLiveMinutes = 15;

    private long refreshTokenTimeToLiveDays = 30;

    public CreateOAuth2ClientRequest() {}

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(String redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    public void setPostLogoutRedirectUris(String postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public boolean isRequirePkce() {
        return requirePkce;
    }

    public void setRequirePkce(boolean requirePkce) {
        this.requirePkce = requirePkce;
    }

    public long getAccessTokenTimeToLiveMinutes() {
        return accessTokenTimeToLiveMinutes;
    }

    public void setAccessTokenTimeToLiveMinutes(long accessTokenTimeToLiveMinutes) {
        this.accessTokenTimeToLiveMinutes = accessTokenTimeToLiveMinutes;
    }

    public long getRefreshTokenTimeToLiveDays() {
        return refreshTokenTimeToLiveDays;
    }

    public void setRefreshTokenTimeToLiveDays(long refreshTokenTimeToLiveDays) {
        this.refreshTokenTimeToLiveDays = refreshTokenTimeToLiveDays;
    }
}
