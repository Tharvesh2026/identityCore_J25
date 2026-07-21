package dev.tharbytes.identityCore.controller;

import dev.tharbytes.identityCore.dto.CreateOAuth2ClientRequest;
import dev.tharbytes.identityCore.dto.OAuth2ClientResponseDto;
import dev.tharbytes.identityCore.repository.UserRepository;
import dev.tharbytes.identityCore.service.OAuth2ClientService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/clients")
public class OAuth2ClientController {

    private final OAuth2ClientService clientService;
    private final UserRepository userRepository;

    public OAuth2ClientController(OAuth2ClientService clientService, UserRepository userRepository) {
        this.clientService = clientService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listClients(Model model, Authentication authentication) {
        addUserToModel(model, authentication);
        List<OAuth2ClientResponseDto> clients = clientService.listClients();
        model.addAttribute("clients", clients);
        if (!model.containsAttribute("createRequest")) {
            model.addAttribute("createRequest", new CreateOAuth2ClientRequest());
        }
        return "clients";
    }

    @PostMapping
    public String createClient(@ModelAttribute("createRequest") CreateOAuth2ClientRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            OAuth2ClientResponseDto created = clientService.createClient(request);
            redirectAttributes.addFlashAttribute("newClient", created);
            redirectAttributes.addFlashAttribute("successMessage", "OAuth2 Client created successfully! Make sure to copy the Client Secret now — it will not be shown again.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create OAuth2 Client: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("createRequest", request);
        }
        return "redirect:/clients";
    }

    @PostMapping("/{clientId}/delete")
    public String deleteClient(@PathVariable String clientId, RedirectAttributes redirectAttributes) {
        try {
            clientService.deleteClient(clientId);
            redirectAttributes.addFlashAttribute("successMessage", "OAuth2 Client '" + clientId + "' deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete client: " + ex.getMessage());
        }
        return "redirect:/clients";
    }

    @PostMapping("/{clientId}/regenerate-secret")
    public String regenerateSecret(@PathVariable String clientId, RedirectAttributes redirectAttributes) {
        try {
            OAuth2ClientResponseDto updated = clientService.regenerateSecret(clientId);
            redirectAttributes.addFlashAttribute("newClient", updated);
            redirectAttributes.addFlashAttribute("successMessage", "Client Secret regenerated successfully for '" + clientId + "'. Copy the new secret now.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to regenerate secret: " + ex.getMessage());
        }
        return "redirect:/clients";
    }

    private void addUserToModel(Model model, Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            userRepository.findByMailId(authentication.getName())
                    .ifPresent(user -> model.addAttribute("user", user));
        }
    }
}
