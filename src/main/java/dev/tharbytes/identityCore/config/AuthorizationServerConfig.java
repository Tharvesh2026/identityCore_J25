package dev.tharbytes.identityCore.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import dev.tharbytes.identityCore.repository.UserRepository;
import dev.tharbytes.identityCore.security.OAuth2UserProvisioner;
import dev.tharbytes.identityCore.util.PasswordUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Turns identityCore into an OIDC/OAuth2 Authorization Server: other
 * Tharvbytes apps register here as OAuth2 clients and redirect users through
 * /oauth2/authorize the same way they'd redirect to Google.
 */
@Configuration
public class AuthorizationServerConfig {

    private final PasswordEncoder passwordEncoder;

    @Value("${icore.sso.issuer-uri:http://localhost:8080}")
    private String issuer;

    private final UserRepository userRepository;
    private final OAuth2UserProvisioner provisioner;

    public AuthorizationServerConfig(UserRepository userRepository, OAuth2UserProvisioner provisioner,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.provisioner = provisioner;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        Function<OidcUserInfoAuthenticationContext, OidcUserInfo> userInfoMapper = context -> {
            String principalName = context.getAuthorization().getPrincipalName();
            OidcUserInfo.Builder builder = OidcUserInfo.builder();

            userRepository.findByMailId(principalName).ifPresentOrElse(user -> {
                List<String> permissions = provisioner.authoritiesFor(user).stream()
                        .map(Object::toString)
                        .filter(a -> !a.startsWith("ROLE_"))
                        .collect(Collectors.toList());

                builder.subject(user.getId().toString())
                        .email(user.getMailId())
                        .name(user.getName())
                        .preferredUsername(user.getUsername() != null ? user.getUsername() : user.getMailId())
                        .claim("permissions", permissions)
                        .claim("role", user.getRoleName());
            }, () -> {
                builder.subject(principalName)
                        .email(principalName)
                        .preferredUsername(principalName);
            });

            return builder.build();
        };

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(oidc -> oidc
                        .userInfoEndpoint(userInfo -> userInfo
                                .userInfoMapper(userInfoMapper)));

        MediaTypeRequestMatcher htmlMatcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
        htmlMatcher.setUseEquals(true);

        http
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                htmlMatcher))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(DataSource dataSource) {
        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(new JdbcTemplate(dataSource));
        String clientId = "tharvbytes-task-app";
        RegisteredClient existing = repository.findByClientId(clientId);
        String clientInternalId = existing != null ? existing.getId() : UUID.randomUUID().toString();
        RegisteredClient demoApp = RegisteredClient.withId(clientInternalId)
                .clientId(clientId)
                .clientSecret(PasswordUtil.hash("Tharvesh@2005."))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:9000/login/oauth2/code/icore")
                .redirectUri("http://127.0.0.1:9000/login/oauth2/code/icore")
                .postLogoutRedirectUri("http://localhost:9000/")
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

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

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

                context.getClaims().subject(user.getId().toString());
                context.getClaims().claim("permissions", permissions);
                context.getClaims().claim("role", user.getRoleName());
                context.getClaims().claim("email", user.getMailId());
                context.getClaims().claim("name", user.getName());
                context.getClaims().claim("preferred_username",
                        user.getUsername() != null ? user.getUsername() : user.getMailId());
            });
        };
    }

    private String extractEmail(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
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
