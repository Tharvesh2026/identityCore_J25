package dev.tharbytes.identityCore.controller;

import dev.tharbytes.identityCore.entity.UserEntity;
import dev.tharbytes.identityCore.security.AuthHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.servlet.http.HttpServletRequest;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final AuthHelper authHelper;

    public DashboardController(AuthHelper authHelper) {
        this.authHelper = authHelper;
    }

    @GetMapping("/welcome")
    public String welcome(Model model) {
        UserEntity user = authHelper.requireCurrentUser();
        model.addAttribute("user", user);
        return "welcome";
    }

    @GetMapping("/settings")
    public String settings(Model model, HttpServletRequest request) {
        UserEntity user = authHelper.requireCurrentUser();
        model.addAttribute("user", user);
        model.addAttribute("sessionId", request.getSession().getId());
        model.addAttribute("sessionCreation", request.getSession().getCreationTime());
        model.addAttribute("sessionLastAccess", request.getSession().getLastAccessedTime());
        model.addAttribute("sessionTimeout", request.getSession().getMaxInactiveInterval());
        return "settings";
    }

    @GetMapping("/logs")
    public void logs(HttpServletRequest request,
                     jakarta.servlet.http.HttpServletResponse response) throws IOException {
        UserEntity user = authHelper.requireCurrentUser();

        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.println("===== APPLICATION LOGS =====");
        out.println("Requested By : " + user.getUsername());
        out.println("Mail Id      : " + user.getMailId());
        out.println("Endpoint     : /logs");
        out.println("Session ID   : " + request.getSession().getId());
        out.println("Timestamp    : " + new Date());
        out.println("----------------------------------------");
        out.println();

        Path logPath = Paths.get("logs/application.log");
        if (!Files.exists(logPath)) {
            out.println("Log file not found: " + logPath.toAbsolutePath());
            return;
        }

        List<String> lines = Files.readAllLines(logPath);
        int start = Math.max(0, lines.size() - 100);
        for (int i = start; i < lines.size(); i++) {
            out.println(lines.get(i));
        }
    }

    /** Session refresh endpoint for AJAX calls from welcome/settings pages */
    @PostMapping("/refresh-session")
    public org.springframework.http.ResponseEntity<Void> refreshSession(HttpServletRequest request) {
        if (request.getSession(false) == null) {
            return org.springframework.http.ResponseEntity.status(401).build();
        }
        request.getSession().setAttribute("refreshed", System.currentTimeMillis());
        return org.springframework.http.ResponseEntity.ok().build();
    }
}
