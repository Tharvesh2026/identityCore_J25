package dev.tharbytes.identityCore.dto;

import java.time.Instant;
import java.util.Set;

public class OAuth2ClientResponseDto {

    private String id;
    private String clientId;
    private String clientSecret; // Plaintext, set only on create / secret-rotation
    private String clientName;
    private Set<String> clientAuthenticationMethods;
    private Set<String> authorizationGrantTypes;
    private Set<String> redirectUris;
    private Set<String> postLogoutRedirectUris;
    private Set<String> scopes;
    private Instant clientIdIssuedAt;
    private boolean requirePkce;

    public OAuth2ClientResponseDto() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Set<String> getClientAuthenticationMethods() {
        return clientAuthenticationMethods;
    }

    public void setClientAuthenticationMethods(Set<String> clientAuthenticationMethods) {
        this.clientAuthenticationMethods = clientAuthenticationMethods;
    }

    public Set<String> getAuthorizationGrantTypes() {
        return authorizationGrantTypes;
    }

    public void setAuthorizationGrantTypes(Set<String> authorizationGrantTypes) {
        this.authorizationGrantTypes = authorizationGrantTypes;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public Set<String> getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    public void setPostLogoutRedirectUris(Set<String> postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public Instant getClientIdIssuedAt() {
        return clientIdIssuedAt;
    }

    public void setClientIdIssuedAt(Instant clientIdIssuedAt) {
        this.clientIdIssuedAt = clientIdIssuedAt;
    }

    public boolean isRequirePkce() {
        return requirePkce;
    }

    public void setRequirePkce(boolean requirePkce) {
        this.requirePkce = requirePkce;
    }
}
