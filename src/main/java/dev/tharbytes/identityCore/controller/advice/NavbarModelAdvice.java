package dev.tharbytes.identityCore.controller.advice;

import dev.tharbytes.identityCore.entity.UserEntity;
import dev.tharbytes.identityCore.security.AuthHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavbarModelAdvice {

    @Autowired
    private AuthHelper authHelper;

    public NavbarModelAdvice(AuthHelper authHelper) {
        this.authHelper = authHelper;
    }

    @ModelAttribute("user")
    public UserEntity currentUser() {
        try {
            return authHelper.requireCurrentUser();
        } catch (Exception e) {
            return null; // let navbar fragment handle null gracefully if unauthenticated page
        }
    }
}