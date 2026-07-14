package dev.tharbytes.identityCore.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Populates SLF4J's MDC with a per-request correlation ID and the authenticated
 * user's identifier, so every log line emitted during a request automatically
 * carries [CorrId=...] and [UserId=...] — no more regex-guessing user identity
 * out of free-text log messages.
 *
 * Order(1) is intentional: Spring Security's filter chain runs earlier
 * (default order -100), so by the time this filter executes, the
 * SecurityContext is already populated for authenticated requests.
 */
@Component
@Order(1)
public class CorrelationIdFilter implements Filter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_KEY = "correlationId";
    public static final String MDC_USER_KEY = "userId";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        try {
            MDC.put(MDC_CORRELATION_KEY, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            populateAuthenticatedUserId();

            chain.doFilter(servletRequest, servletResponse);
        } finally {
            // Always clear — threads are pooled and reused across requests.
            MDC.clear();
        }
    }

    private void populateAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            MDC.put(MDC_USER_KEY, auth.getName());
        }
        // If unauthenticated, MDC_USER_KEY stays unset; the logging pattern's
        // %X{userId:-anonymous} default handles that case.
    }
}