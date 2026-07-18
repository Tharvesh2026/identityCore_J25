package dev.tharbytes.identityCore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final VerificationInterceptor verificationInterceptor;

    public WebConfig(VerificationInterceptor verificationInterceptor) {
        this.verificationInterceptor = verificationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Disabled redirection block to allow unverified users to see settings/profile and trigger verification manually.
        // registry.addInterceptor(verificationInterceptor);
    }
}
