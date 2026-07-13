package dev.tharbytes.identityCore.security;

import dev.tharbytes.identityCore.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuth2UserProvisioner provisioner;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    public CustomOAuth2UserService(OAuth2UserProvisioner provisioner) {
        this.provisioner = provisioner;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        log.info("OAuth2 login initiated via provider [{}].", registrationId);

        String email = extractEmail(oAuth2User, userRequest, registrationId);
        if (email == null || email.isBlank()) {
            log.warn("OAuth2 login failed via provider [{}]: no verified email available.", registrationId);
            throw new OAuth2AuthenticationException("Email not available from " + registrationId);
        }
        email = email.toLowerCase();

        UserEntity user = provisioner.resolveOrProvisionUser(email, resolveName(oAuth2User), registrationId);

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            log.warn("OAuth2 login denied via provider [{}] for user [{}]: account status is [{}].",
                    registrationId, email, user.getStatus());
            throw new OAuth2AuthenticationException("Account is not active");
        }

        List<SimpleGrantedAuthority> authorities = provisioner.authoritiesFor(user);

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("email", email);

        log.info("OAuth2 login successful via provider [{}] for user [{}].", registrationId, email);

        return new DefaultOAuth2User(authorities, attributes, "email");
    }

    private String resolveName(OAuth2User oAuth2User) {
        String name = oAuth2User.getAttribute("name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        String login = oAuth2User.getAttribute("login");
        return login != null ? login : "New User";
    }

    private String extractEmail(OAuth2User oAuth2User, OAuth2UserRequest userRequest, String registrationId) {
        String email = oAuth2User.getAttribute("email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        if ("github".equalsIgnoreCase(registrationId)) {
            return fetchGithubPrimaryEmail(userRequest.getAccessToken().getTokenValue());
        }
        return null;
    }

    /**
     * GitHub only returns "email" in the base profile if the user has made it public.
     * Otherwise we fetch it via the /user/emails endpoint, which requires the
     * "user:email" scope to be included in the GitHub client registration.
     */
    private String fetchGithubPrimaryEmail(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/vnd.github+json");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<GithubEmail[]> response = restTemplate.exchange(
                    "https://api.github.com/user/emails", HttpMethod.GET, entity, GithubEmail[].class);
            GithubEmail[] emails = response.getBody();
            if (emails == null) {
                return null;
            }
            for (GithubEmail e : emails) {
                if (e.primary && e.verified) {
                    return e.email;
                }
            }
            for (GithubEmail e : emails) {
                if (e.verified) {
                    return e.email;
                }
            }
        } catch (RestClientException ex) {
            log.error("Failed to fetch GitHub email addresses for OAuth2 login.", ex);
        }
        return null;
    }

    private static class GithubEmail {
        public String email;
        public boolean primary;
        public boolean verified;
    }
}