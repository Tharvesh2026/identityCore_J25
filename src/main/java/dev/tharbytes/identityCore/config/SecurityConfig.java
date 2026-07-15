package dev.tharbytes.identityCore.config;

import dev.tharbytes.identityCore.security.AppUserDetailsService;
import dev.tharbytes.identityCore.security.CustomOAuth2UserService;
import dev.tharbytes.identityCore.security.CustomOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final AppUserDetailsService userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;

    public SecurityConfig(AppUserDetailsService userDetailsService,
                          CustomOAuth2UserService customOAuth2UserService,
                          CustomOidcUserService customOidcUserService) {
        this.userDetailsService = userDetailsService;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ── CSRF ──────────────────────────────────────────────────────────
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                "/h2-console/**",
                                "/user/**",
                                "/auth/**"
                        )
                )

                .headers(h -> h.frameOptions(fo -> fo.sameOrigin()))

                .authorizeHttpRequests(auth -> auth
                        // Public
                        .requestMatchers(
                                "/", "/login", "/register",
                                "/user/login", "/user/register",
                                "/oauth2/**", "/login/oauth2/**",
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
                        .defaultSuccessUrl("/welcome", true)
                        .failureUrl("/login?error=Invalid+email+or+password")
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

                // ── Logout ────────────────────────────────────────────────────────
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=SUCCESS")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                // ── Session management ────────────────────────────────────────────
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .expiredUrl("/login?error=Session+expired.+Please+login+again.")
                );

        return http.build();
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
}