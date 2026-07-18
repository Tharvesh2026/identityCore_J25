package dev.tharbytes.identityCore.config;

import dev.tharbytes.identityCore.entity.UserEntity;
import dev.tharbytes.identityCore.security.AuthHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class VerificationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(VerificationInterceptor.class);
    private final AuthHelper authHelper;

    public VerificationInterceptor(AuthHelper authHelper) {
        this.authHelper = authHelper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserEntity currentUser = authHelper.getCurrentUser();

        if (currentUser != null && "PENDING_VERIFICATION".equalsIgnoreCase(currentUser.getStatus())) {
            String uri = request.getRequestURI();

            // Allow access to verification endpoints, logout, and static assets
            if (uri.equals("/verify-otp") ||
                uri.equals("/verify-otp/resend") ||
                uri.equals("/logout") ||
                uri.startsWith("/css/") ||
                uri.startsWith("/js/") ||
                uri.startsWith("/images/") ||
                uri.startsWith("/assets/") ||
                uri.equals("/favicon.ico")) {
                return true;
            }

            log.debug("Redirecting unverified user [{}] from [{}] to verification page.", currentUser.getUsername(), uri);

            // If it is a REST API request, return a JSON error instead of redirection
            String acceptHeader = request.getHeader("Accept");
            if (uri.startsWith("/user/") || uri.startsWith("/auth/") || (acceptHeader != null && acceptHeader.contains("application/json"))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"Email verification required\",\"status\":\"PENDING_VERIFICATION\"}");
                return false;
            }

            response.sendRedirect("/verify-otp");
            return false;
        }

        return true;
    }
}
