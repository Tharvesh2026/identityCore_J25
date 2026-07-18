package dev.tharbytes.identityCore.config;

import dev.tharbytes.identityCore.security.AppUserDetailsService;
import dev.tharbytes.identityCore.security.CustomOAuth2UserService;
import dev.tharbytes.identityCore.security.CustomOidcUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import javax.sql.DataSource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final AppUserDetailsService userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;

    // Loaded from application.properties / env var — NEVER hardcode this.
    // Used to sign the persistent remember-me token; rotating it invalidates
    // all existing "remember me" sessions app-wide.
    @Value("${app.remember-me.key}")
    private String rememberMeKey;

    // 14 days, in seconds. Overridable via application.properties.
    @Value("${app.remember-me.validity-seconds:1209600}")
    private int rememberMeValiditySeconds;

    // false for local HTTP dev, true in production (HTTPS via tunnel/proxy).
    // A "true" value here on a plain-HTTP origin makes the browser silently
    // refuse to store/send the cookie — remember-me will look completely
    // broken with no error anywhere.
    @Value("${app.remember-me.secure-cookie:false}")
    private boolean rememberMeSecureCookie;

    public SecurityConfig(AppUserDetailsService userDetailsService,
                          CustomOAuth2UserService customOAuth2UserService,
                          CustomOidcUserService customOidcUserService) {
        this.userDetailsService = userDetailsService;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, PersistentTokenRepository tokenRepository) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // ── CSRF ──────────────────────────────────────────────────────────
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                "/h2-console/**",
                                "/user/**",
                                "/auth/**",
                                "/login",
                                "/register",
                                "/verify-otp/resend"
                        )
                )
                .addFilterAfter(new org.springframework.web.filter.OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, 
                                                    jakarta.servlet.http.HttpServletResponse response, 
                                                    jakarta.servlet.FilterChain filterChain)
                            throws jakarta.servlet.ServletException, java.io.IOException {
                        org.springframework.security.web.csrf.CsrfToken csrfToken = 
                            (org.springframework.security.web.csrf.CsrfToken) request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
                        if (csrfToken != null) {
                            csrfToken.getToken();
                        }
                        filterChain.doFilter(request, response);
                    }
                }, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)

                .headers(h -> h.frameOptions(fo -> fo.sameOrigin()))

                .authorizeHttpRequests(auth -> auth
                        // Public
                        .requestMatchers(
                                "/", "/login", "/register",
                                "/user/login", "/user/register",
                                "/oauth2/**", "/login/oauth2/**",
                                "/forgot-password", "/reset-password",
                                "/verify-otp", "/verify-otp/resend",
                                "/user/verify-otp", "/user/verify-otp/resend",
                                "/terms", "/privacy", "/cookie-policy",
                                "/user/terms", "/user/privacy", "/user/cookie-policy",
                                "/user/forgot-password", "/user/reset-password", "/user/reset-password/verify",
                                "/css/**", "/js/**", "/images/**", "/assets/**",
                                "/h2-console/**", "/error"
                        ).permitAll()

                        // Permission-gated pages
                        .requestMatchers("/users", "/manage-user").hasAuthority("USER_READ")
                        .requestMatchers("/roles", "/manage-role").hasAuthority("ROLE_READ")
                        .requestMatchers("/logs", "/log").hasAuthority("LOG_VIEW")

                        // API endpoints — checked in controller via UserService.hasPermission()
                        .requestMatchers("/auth/**").authenticated()
                        .requestMatchers("/user/**").authenticated()

                        // Everything else needs authentication
                        .anyRequest().authenticated()
                )

                // ── Form login ────────────────────────────────────────────────────
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            if (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json")) {
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"success\":true,\"message\":\"Login successful\"}");
                            } else {
                                response.sendRedirect("/welcome");
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            String errorMsg = "Invalid email or password";
                            if (exception != null && exception.getMessage() != null && exception.getMessage().contains("connected with")) {
                                errorMsg = exception.getMessage();
                            }
                            if (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json")) {
                                response.setStatus(401);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"success\":false,\"message\":\"" + errorMsg + "\"}");
                            } else {
                                String redirectUrl = "/?error=" + java.net.URLEncoder.encode(errorMsg, java.nio.charset.StandardCharsets.UTF_8);
                                response.sendRedirect(redirectUrl);
                            }
                        })
                        .permitAll()
                )

                // ── OAuth2 login (Google / GitHub) ───────────────────────────────
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService)
                        )
                        .defaultSuccessUrl("/welcome", true)
                        .failureUrl("/login?error=OAuth+authentication+failed")
                        .permitAll()
                )

                // ── Remember Me (persistent token — revocable per device) ────────
                .rememberMe(rememberMe -> rememberMe
                        .key(rememberMeKey)
                        .tokenRepository(tokenRepository)
                        .tokenValiditySeconds(rememberMeValiditySeconds)
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("remember-me")     // matches the login form checkbox name
                        .rememberMeCookieName("ICORE_REMEMBER_ME")
                        .useSecureCookie(rememberMeSecureCookie) // true in prod (HTTPS), false for local HTTP dev
                )

                // ── Logout ────────────────────────────────────────────────────────
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=SUCCESS")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "ICORE_REMEMBER_ME")
                        .clearAuthentication(true)
                        .permitAll()
                )

                // ── Session management ────────────────────────────────────────────
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .expiredUrl("/login?error=Session+expired.+Please+login+again.")
                );

        return http.build();
    }

    /**
     * Persistent (DB-backed) remember-me token store. Unlike the simpler
     * hash-based token, this lets you revoke a single device's "remember me"
     * session without invalidating everyone else's — the standard enterprise
     * approach. Requires the `persistent_logins` table (see persistent_logins.sql).
     */
    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        // Table is created manually via SQL — do NOT set createTableOnStartup(true)
        // here: it re-runs CREATE TABLE (no "IF NOT EXISTS") on every restart and
        // will throw once the table exists.
        return tokenRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Bypasses the entire Spring Security filter chain (including the
     * remember-me filter) for static assets. This matters beyond performance:
     * without it, parallel asset requests race the main page request for the
     * remember-me cookie during token rotation, triggering false-positive
     * "cookie theft" detection and deleting the persistent_logins row.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/css/**", "/js/**", "/images/**", "/assets/**", "/favicon.ico"
        );
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}