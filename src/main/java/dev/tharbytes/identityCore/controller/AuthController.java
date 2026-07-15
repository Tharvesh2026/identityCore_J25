package dev.tharbytes.identityCore.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/")
    public String root(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/welcome";
        }
        return "landing";
    }

    @GetMapping("/login")
    public String loginPage(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/welcome";
        }
        return "index";
    }
    /** Show register tab — redirect to login page with tab hint */
    @GetMapping("/register")
    public String registerPage() {
        return "redirect:/login?tab=register";
    }
}
