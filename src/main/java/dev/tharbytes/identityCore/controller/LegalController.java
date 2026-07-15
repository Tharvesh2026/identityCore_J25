package dev.tharbytes.identityCore.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Public, unauthenticated legal pages. Both routes are permitted in
 * SecurityConfig without login, since users need to be able to read these
 * before creating an account (and search engines/regulators need to reach them).
 */
@Controller
public class LegalController {

    @Value("${app.legal.terms-updated}")
    private String termsUpdated;

    @Value("${app.legal.privacy-updated}")
    private String privacyUpdated;

    @Value("${app.legal.contact-email}")
    private String contactEmail;

    @Value("${app.legal.company-name}")
    private String companyName;

    @Value("${app.legal.cookie-policy-updated:July 15, 2026}")
    private String cookiePolicyUpdated;

    @GetMapping("/terms")

    public String terms(Model model) {
        model.addAttribute("lastUpdated", termsUpdated);
        model.addAttribute("contactEmail", contactEmail);
        model.addAttribute("companyName", companyName);
        return "legal/terms";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("lastUpdated", privacyUpdated);
        model.addAttribute("contactEmail", contactEmail);
        model.addAttribute("companyName", companyName);
        return "legal/privacy";
    }

    @GetMapping("/cookie-policy")
    public String cookiePolicy(Model model) {
        model.addAttribute("cookiePolicyUpdated", cookiePolicyUpdated);
        model.addAttribute("contactEmail", contactEmail);
        return "legal/cookie-policy";
    }
}