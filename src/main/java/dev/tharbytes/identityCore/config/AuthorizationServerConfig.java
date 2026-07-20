package dev.tharbytes.identityCore.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import dev.tharbytes.identityCore.entity.UserEntity;
import dev.tharbytes.identityCore.repository.UserRepository;
import dev.tharbytes.identityCore.security.OAuth2UserProvisioner;
import dev.tharbytes.identityCore.util.PasswordUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.http.MediaType;

/**
 * Turns identityCore into an OIDC/OAuth2 Authorization Server: other
 * Tharvbytes apps register here as OAuth2 clients and redirect users through
 * /oauth2/authorize the same way they'd redirect to Google.
 *
 * This sits ALONGSIDE the existing SecurityConfig (dashboard login,
 * remember-me,
 * federated Google/GitHub login) — it does not replace it. Order(1) here means
 * this chain is checked first, but only its own requestMatchers (/oauth2/**,
 * /.well-known/**) actually match; everything else falls through to
 * SecurityConfig's Order(2) chain, unchanged.
 *
 * v3.0 TODO (deferred per current scope): persist the JWK signing key instead
 * of generating it at boot, and add idle-sleep-aware token lifetimes.
 */
@Configuration
public class AuthorizationServerConfig {

    private final PasswordEncoder passwordEncoder;

    // Set to your real public origin once you have one. Every registered
    // client's issuer-uri config must match this exactly.
    @Value("${icore.sso.issuer-uri:http://localhost:8080}")
    private String issuer;

    private final UserRepository userRepository;
    private final OAuth2UserProvisioner provisioner;

    public AuthorizationServerConfig(UserRepository userRepository, OAuth2UserProvisioner provisioner, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.provisioner = provisioner;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults()); // enables OIDC discovery, userinfo, jwks on top of plain OAuth2

        // Unauthenticated hits to /oauth2/authorize fall through to your
        // existing /login page (form + Google/GitHub), not a 401 JSON error.
        http.exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

        return http.build();
    }


    /**
     * Registered clients = every Tharvbytes app allowed to use identityCore as SSO.
     * Uses plain JDBC (not JPA) against dedicated oauth2_registered_client /
     * oauth2_authorization / oauth2_authorization_consent tables — see
     * src/main/resources/authorization-server-schema.sql, run once against
     * your Supabase database.
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(DataSource dataSource) {
        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(new JdbcTemplate(dataSource));
        // Seed one test client so you can demo end-to-end immediately.
        // Replace with real Tharvbytes app values, then remove this block —
        // long-term this belongs behind a small admin screen (RoleController
        // style: hasAuthority("CLIENT_MANAGE")), not hardcoded here.
        RegisteredClient existing = repository.findByClientId("tharvbytes-app");
        String clientInternalId = existing != null ? existing.getId() : UUID.randomUUID().toString();
        RegisteredClient demoApp = RegisteredClient.withId(clientInternalId)
                .clientId("tharvbytes-demo-app")
                .clientSecret(PasswordUtil.hash("Tharvesh@2005."))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:9000/login/oauth2/code/icore")
                .postLogoutRedirectUri("http://127.0.0.1:9000/")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL)
                .clientSettings(ClientSettings.builder().requireProofKey(true).build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(15))
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .reuseRefreshTokens(false)
                        .build())
                .build();
        repository.save(demoApp);
        return repository;
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }

    /**
     * RSA key used to sign every access/ID token, exposed at /oauth2/jwks.
     * Generated fresh on every boot for now — fine while the app runs
     * continuously; revisit in v3.0 once idle-sleep is back on the table,
     * since a restart would otherwise invalidate every token issued before it.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    /**
     * The part that connects your existing permission model to the tokens
     * other apps receive: pulls the authenticated user's real permissions
     * (same authoritiesFor() used for the dashboard) into the JWT/ID token as
     * a "permissions" claim, and the role name as "role".
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            Authentication principal = context.getPrincipal();
            String email = extractEmail(principal.getPrincipal());
            if (email == null)
                return;

            userRepository.findByMailId(email).ifPresent(user -> {
                List<String> permissions = provisioner.authoritiesFor(user).stream()
                        .map(Object::toString)
                        .filter(a -> !a.startsWith("ROLE_"))
                        .collect(Collectors.toList());

                context.getClaims().claim("permissions", permissions);
                context.getClaims().claim("role", user.getRoleName());
            });
        };
    }

    private String extractEmail(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername(); // AppUserDetailsService uses email as username
        }
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        if (principal instanceof OAuth2User oAuth2User) {
            Object email = oAuth2User.getAttribute("email");
            return email != null ? email.toString() : null;
        }
        return null;
    }
}
