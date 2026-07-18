package dev.tharbytes.identityCore.security;

import dev.tharbytes.identityCore.entity.UserEntity;
import dev.tharbytes.identityCore.service.EmailService;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Handles OIDC login (e.g. Google, which issues an ID token). Spring Security routes
 * OIDC providers through OidcUserService rather than the plain OAuth2UserService used
 * for non-OIDC providers like GitHub, so this is kept as a separate handler.
 */
@Service
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OAuth2UserProvisioner provisioner;
    private final OidcUserService delegate = new OidcUserService();
    private final EmailService emailService;

    private static final Logger log =
            LoggerFactory.getLogger(CustomOidcUserService.class);

    public CustomOidcUserService(OAuth2UserProvisioner provisioner, EmailService emailService) {
        this.provisioner = provisioner;
        this.emailService = emailService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        log.info("OIDC login initiated via provider [{}].", registrationId);

        String email = oidcUser.getEmail();
        if (email == null || email.isBlank()) {
            log.warn("OIDC login failed via provider [{}]: no verified email available.", registrationId);
            throw new OAuth2AuthenticationException("Email not available from " + registrationId);
        }
        email = email.toLowerCase();

        String displayName = oidcUser.getFullName() != null ? oidcUser.getFullName() : email;

        UserEntity user = provisioner.resolveOrProvisionUser(email, displayName, registrationId);

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            log.warn("OIDC login denied via provider [{}] for user [{}]: account status is [{}].",
                    registrationId, email, user.getStatus());
            throw new OAuth2AuthenticationException("Account is not active");
        }

        List<SimpleGrantedAuthority> authorities = provisioner.authoritiesFor(user);

        log.info("OIDC login successful via provider [{}] for user [{}].", registrationId, email);

        try {
            emailService.inviteMail(email);
        } catch (IOException e) {
            log.error("Invite Mail not send, {}",e.getMessage());
        } catch (MessagingException e) {
            log.error("Invite Mail not send, {}",e.getMessage());
        }

        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), "email");
    }
}