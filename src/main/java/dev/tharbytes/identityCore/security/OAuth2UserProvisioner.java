package dev.tharbytes.identityCore.security;

import dev.tharbytes.identityCore.entity.RoleEntity;
import dev.tharbytes.identityCore.entity.UserEntity;
import dev.tharbytes.identityCore.exception.ResourceNotFoundException;
import dev.tharbytes.identityCore.repository.RoleRepository;
import dev.tharbytes.identityCore.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OAuth2UserProvisioner {

    private static final Long GUEST_ROLE_ID = 6L;
    private static final String GUEST_PASSWORD_HASH =
            "$2y$12$/BEKBrgkGL97uw9Xs.hkNet2SxkmNuitSk6e6R/3I2Yy8Gxig6Ysy";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private static final Logger log =
            LoggerFactory.getLogger(AppUserDetailsService.class);

    public OAuth2UserProvisioner(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public UserEntity resolveOrProvisionUser(String email, String displayName, String registrationId) {
        return userRepository.findByMailId(email)
                .orElseGet(() -> provisionGuestUser(email, displayName, registrationId));
    }

    public List<SimpleGrantedAuthority> authoritiesFor(UserEntity user) {
        List<SimpleGrantedAuthority> authorities = user.getRole().getPermissions().stream()
                .map(p -> new SimpleGrantedAuthority(p.getPermissionKey()))
                .collect(Collectors.toList());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRoleName()));
        return authorities;
    }

    private UserEntity provisionGuestUser(String email, String displayName, String registrationId) {
        RoleEntity guestRole = roleRepository.findById(GUEST_ROLE_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Guest role not found"));

        UserEntity user = new UserEntity();
        user.setName(displayName);
        user.setUsername(generateUniqueUsername(email));
        user.setMailId(email);
        user.setPassword(GUEST_PASSWORD_HASH);
        user.setRole(guestRole);
        user.setStatus("ACTIVE");

        UserEntity saved = userRepository.save(user);
        log.info("User [{}] auto-provisioned via provider [{}] with GUEST role.", email, registrationId);
        return saved;
    }

    private String generateUniqueUsername(String email) {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9]", "");
        if (base.isBlank()) {
            base = "user";
        }
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }
}