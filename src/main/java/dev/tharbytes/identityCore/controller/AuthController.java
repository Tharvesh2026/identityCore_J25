package dev.tharbytes.identityCore.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    /** Root → redirect to login */
    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    // Show login page 
    @GetMapping("/login")
    public String loginPage() {
        return "index";
    }

    /** Show register tab — redirect to login page with tab hint */
    @GetMapping("/register")
    public String registerPage() {
        return "redirect:/login?tab=register";
    }
}
