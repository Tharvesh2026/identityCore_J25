package dev.tharbytes.identityCore.security;

import dev.tharbytes.identityCore.entity.UserEntity;
import dev.tharbytes.identityCore.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    private static final Logger log =
            LoggerFactory.getLogger(AppUserDetailsService.class);

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String mailId) throws UsernameNotFoundException {
        log.info("Authentication initiated for username [{}].", mailId);

        UserEntity user = userRepository.findByMailId(mailId)
                .orElseThrow(() -> {
                    log.warn("Authentication failed for username [{}]: user not found.", mailId);
                    return new UsernameNotFoundException("User not found: " + mailId);
                });

        List<SimpleGrantedAuthority> authorities = user.getRole().getPermissions().stream()
                .map(p -> new SimpleGrantedAuthority(p.getPermissionKey()))
                .collect(Collectors.toList());

        // Also add ROLE_ prefix authority for Spring Security hasRole checks
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRoleName()));

        if ("SUSPENDED".equalsIgnoreCase(user.getStatus())) {
            log.warn("Authentication attempted for locked account [{}].", mailId);
        } else if ("INACTIVE".equalsIgnoreCase(user.getStatus())) {
            log.warn("Authentication attempted for inactive account [{}].", mailId);
        } else {
            log.info("User details loaded successfully for username [{}].", mailId);
        }

        return User.builder()
                .username(mailId)   // use email as username for Spring Security
                .password(user.getPassword())
                .authorities(authorities)
                .disabled("INACTIVE".equalsIgnoreCase(user.getStatus()))
                .accountLocked("SUSPENDED".equalsIgnoreCase(user.getStatus()))
                .build();
    }
}