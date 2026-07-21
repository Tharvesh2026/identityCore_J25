package dev.tharbytes.identityCore.service;

import dev.tharbytes.identityCore.dto.CreateOAuth2ClientRequest;
import dev.tharbytes.identityCore.dto.OAuth2ClientResponseDto;
import dev.tharbytes.identityCore.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OAuth2ClientService {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ClientService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final RegisteredClientRepository registeredClientRepository;
    private final JdbcTemplate jdbcTemplate;

    public OAuth2ClientService(RegisteredClientRepository registeredClientRepository, JdbcTemplate jdbcTemplate) {
        this.registeredClientRepository = registeredClientRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public OAuth2ClientResponseDto createClient(CreateOAuth2ClientRequest request) {
        String clientName = request.getClientName().trim();
        
        String clientId = StringUtils.hasText(request.getClientId())
                ? request.getClientId().trim()
                : generateClientId(clientName);

        if (registeredClientRepository.findByClientId(clientId) != null) {
            throw new IllegalArgumentException("Client ID already exists: " + clientId);
        }

        String rawClientSecret = generateSecureSecret();
        String hashedSecret = PasswordUtil.hash(rawClientSecret);

        String internalId = UUID.randomUUID().toString();
        RegisteredClient.Builder builder = RegisteredClient.withId(internalId)
                .clientId(clientId)
                .clientName(clientName)
                .clientSecret(hashedSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);

        // Parse and add redirect URIs
        Set<String> redirectUris = parseUris(request.getRedirectUris());
        if (redirectUris.isEmpty()) {
            throw new IllegalArgumentException("At least one valid redirect URI is required.");
        }
        redirectUris.forEach(builder::redirectUri);

        // Parse and add post logout redirect URIs
        Set<String> postLogoutUris = parseUris(request.getPostLogoutRedirectUris());
        postLogoutUris.forEach(builder::postLogoutRedirectUri);

        // Set scopes
        List<String> scopes = request.getScopes();
        if (scopes == null || scopes.isEmpty()) {
            builder.scope(OidcScopes.OPENID)
                   .scope(OidcScopes.PROFILE)
                   .scope(OidcScopes.EMAIL);
        } else {
            scopes.forEach(builder::scope);
        }

        builder.clientSettings(ClientSettings.builder()
                .requireProofKey(request.isRequirePkce())
                .build());

        builder.tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(request.getAccessTokenTimeToLiveMinutes() > 0 ? request.getAccessTokenTimeToLiveMinutes() : 15))
                .refreshTokenTimeToLive(Duration.ofDays(request.getRefreshTokenTimeToLiveDays() > 0 ? request.getRefreshTokenTimeToLiveDays() : 30))
                .reuseRefreshTokens(false)
                .build());

        RegisteredClient registeredClient = builder.build();
        registeredClientRepository.save(registeredClient);

        log.info("Successfully registered dynamic OAuth2 client [{}] for application [{}]", clientId, clientName);

        OAuth2ClientResponseDto response = mapToDto(registeredClient);
        response.setClientSecret(rawClientSecret); // Return plaintext secret ONLY once on creation
        return response;
    }

    public List<OAuth2ClientResponseDto> listClients() {
        String sql = "SELECT id, client_id, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, post_logout_redirect_uris, scopes, client_settings, client_id_issued_at FROM oauth2_registered_client ORDER BY client_id_issued_at DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            OAuth2ClientResponseDto dto = new OAuth2ClientResponseDto();
            dto.setId(rs.getString("id"));
            dto.setClientId(rs.getString("client_id"));
            dto.setClientName(rs.getString("client_name"));
            dto.setClientAuthenticationMethods(StringUtils.commaDelimitedListToSet(rs.getString("client_authentication_methods")));
            dto.setAuthorizationGrantTypes(StringUtils.commaDelimitedListToSet(rs.getString("authorization_grant_types")));
            dto.setRedirectUris(StringUtils.commaDelimitedListToSet(rs.getString("redirect_uris")));
            dto.setPostLogoutRedirectUris(StringUtils.commaDelimitedListToSet(rs.getString("post_logout_redirect_uris")));
            dto.setScopes(StringUtils.commaDelimitedListToSet(rs.getString("scopes")));
            
            java.sql.Timestamp timestamp = rs.getTimestamp("client_id_issued_at");
            if (timestamp != null) {
                dto.setClientIdIssuedAt(timestamp.toInstant());
            }

            String settingsStr = rs.getString("client_settings");
            dto.setRequirePkce(settingsStr != null && settingsStr.contains("require-proof-key") && settingsStr.contains("true"));

            return dto;
        });
    }

    public OAuth2ClientResponseDto findByClientId(String clientId) {
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            throw new NoSuchElementException("OAuth2 client not found with clientId: " + clientId);
        }
        return mapToDto(client);
    }

    public OAuth2ClientResponseDto regenerateSecret(String clientId) {
        RegisteredClient existing = registeredClientRepository.findByClientId(clientId);
        if (existing == null) {
            throw new NoSuchElementException("OAuth2 client not found with clientId: " + clientId);
        }

        String rawClientSecret = generateSecureSecret();
        String hashedSecret = PasswordUtil.hash(rawClientSecret);

        RegisteredClient updated = RegisteredClient.from(existing)
                .clientSecret(hashedSecret)
                .build();

        registeredClientRepository.save(updated);
        log.info("Regenerated client secret for OAuth2 client [{}]", clientId);

        OAuth2ClientResponseDto dto = mapToDto(updated);
        dto.setClientSecret(rawClientSecret); // Return new plaintext secret
        return dto;
    }

    public void deleteClient(String clientId) {
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client == null) {
            throw new NoSuchElementException("OAuth2 client not found with clientId: " + clientId);
        }
        jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE client_id = ?", clientId);
        log.info("Deleted OAuth2 client [{}]", clientId);
    }

    private OAuth2ClientResponseDto mapToDto(RegisteredClient client) {
        OAuth2ClientResponseDto dto = new OAuth2ClientResponseDto();
        dto.setId(client.getId());
        dto.setClientId(client.getClientId());
        dto.setClientName(client.getClientName());
        dto.setClientAuthenticationMethods(client.getClientAuthenticationMethods().stream()
                .map(ClientAuthenticationMethod::getValue).collect(Collectors.toSet()));
        dto.setAuthorizationGrantTypes(client.getAuthorizationGrantTypes().stream()
                .map(AuthorizationGrantType::getValue).collect(Collectors.toSet()));
        dto.setRedirectUris(client.getRedirectUris());
        dto.setPostLogoutRedirectUris(client.getPostLogoutRedirectUris());
        dto.setScopes(client.getScopes());
        dto.setClientIdIssuedAt(client.getClientIdIssuedAt());
        dto.setRequirePkce(client.getClientSettings().isRequireProofKey());
        return dto;
    }

    private String generateClientId(String clientName) {
        String slug = clientName.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (slug.isBlank()) slug = "client";
        return slug + "-" + randomAlphanumeric(6);
    }

    private String generateSecureSecret() {
        return "ic_sec_" + randomAlphanumeric(32);
    }

    private String randomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(SECURE_RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    private Set<String> parseUris(String uris) {
        if (!StringUtils.hasText(uris)) return Collections.emptySet();
        return Arrays.stream(uris.split("[,\\r\\n]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }
}
