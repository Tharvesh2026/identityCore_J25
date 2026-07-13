package dev.tharbytes.identityCore.security;

import dev.tharbytes.identityCore.entity.UserEntity;
import dev.tharbytes.identityCore.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String mailId) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByMailId(mailId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + mailId));

        List<SimpleGrantedAuthority> authorities = user.getRole().getPermissions().stream()
                .map(p -> new SimpleGrantedAuthority(p.getPermissionKey()))
                .collect(Collectors.toList());

        // Also add ROLE_ prefix authority for Spring Security hasRole checks
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRoleName()));

        return User.builder()
                .username(mailId)   // use email as username for Spring Security
                .password(user.getPassword())
                .authorities(authorities)
                .disabled("INACTIVE".equalsIgnoreCase(user.getStatus()))
                .accountLocked("SUSPENDED".equalsIgnoreCase(user.getStatus()))
                .build();
    }
}
