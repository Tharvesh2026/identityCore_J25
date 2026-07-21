package dev.tharbytes.identityCore.controller.api;

import dev.tharbytes.identityCore.dto.CreateOAuth2ClientRequest;
import dev.tharbytes.identityCore.dto.OAuth2ClientResponseDto;
import dev.tharbytes.identityCore.service.OAuth2ClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/oauth2/clients")
public class OAuth2ClientApiController {

    private final OAuth2ClientService clientService;

    public OAuth2ClientApiController(OAuth2ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<OAuth2ClientResponseDto> createClient(@Valid @RequestBody CreateOAuth2ClientRequest request) {
        OAuth2ClientResponseDto created = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<OAuth2ClientResponseDto>> listClients() {
        return ResponseEntity.ok(clientService.listClients());
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<OAuth2ClientResponseDto> getClient(@PathVariable String clientId) {
        return ResponseEntity.ok(clientService.findByClientId(clientId));
    }

    @PostMapping("/{clientId}/regenerate-secret")
    public ResponseEntity<OAuth2ClientResponseDto> regenerateSecret(@PathVariable String clientId) {
        return ResponseEntity.ok(clientService.regenerateSecret(clientId));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Map<String, Object>> deleteClient(@PathVariable String clientId) {
        clientService.deleteClient(clientId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OAuth2 Client deleted successfully: " + clientId
        ));
    }
}
