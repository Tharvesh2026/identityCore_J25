package dev.tharbytes.identityCore.security;

import dev.tharbytes.identityCore.entity.UserEntity;
import dev.tharbytes.identityCore.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthHelper {

    private final UserRepository userRepository;

    public AuthHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the currently logged-in UserEntity, or null if not authenticated.
     */
    public UserEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;
        String email = auth.getName();
        return userRepository.findByMailId(email).orElse(null);
    }

    /**
     * Returns the currently logged-in user, throwing if not found.
     */
    public UserEntity requireCurrentUser() {
        UserEntity user = getCurrentUser();
        if (user == null)
            throw new IllegalStateException("Not authenticated");
        return user;
    }
}
