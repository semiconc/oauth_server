package com.example.authserver.controller;

import com.example.authserver.dto.PasskeyCredentialResponse;
import com.example.authserver.dto.PasskeyFinalizeRequest;
import com.example.authserver.dto.PasskeyLoginInitRequest;
import com.example.authserver.dto.PasskeyRegistrationInitRequest;
import com.example.authserver.service.HankoPasskeyService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/passkey")
public class PasskeyController {

    private static final Logger logger = LoggerFactory.getLogger(PasskeyController.class);

    private final HankoPasskeyService passkeyService;

    public PasskeyController(HankoPasskeyService passkeyService) {
        this.passkeyService = passkeyService;
    }

    /**
     * Initialize passkey registration for authenticated user
     */
    @PostMapping("/register/initialize")
    public Mono<ResponseEntity<JsonNode>> initializeRegistration() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String username = authentication.getName();

        PasskeyRegistrationInitRequest request = new PasskeyRegistrationInitRequest(
                username,
                username,
                username
        );

        return passkeyService.initializeRegistration(request)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    logger.error("Error initializing registration", error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Finalize passkey registration
     */
    @PostMapping("/register/finalize")
    public Mono<ResponseEntity<Map<String, String>>> finalizeRegistration(
            @RequestBody PasskeyCredentialResponse credential) {

        // Wrap the credential in the expected format for the service
        PasskeyFinalizeRequest request = new PasskeyFinalizeRequest(credential);

        return passkeyService.finalizeRegistration(request)
                .map(response -> {
                    logger.info("Passkey registration finalized successfully");
                    return ResponseEntity.ok(Map.of("message", "Passkey registered successfully"));
                })
                .onErrorResume(error -> {
                    logger.error("Error finalizing registration", error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Failed to register passkey")));
                });
    }

    /**
     * Initialize passkey login
     */
    @PostMapping("/login/initialize")
    public Mono<ResponseEntity<JsonNode>> initializeLogin(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");

        if (userId == null || userId.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        PasskeyLoginInitRequest request = new PasskeyLoginInitRequest(userId);

        return passkeyService.initializeLogin(request)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    logger.error("Error initializing login", error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Finalize passkey login and authenticate user
     */
    @PostMapping("/login/finalize")
    public Mono<ResponseEntity<Map<String, String>>> finalizeLogin(
            @RequestBody PasskeyCredentialResponse credential) {

        // Wrap the credential in the expected format for the service
        PasskeyFinalizeRequest request = new PasskeyFinalizeRequest(credential);

        return passkeyService.finalizeLogin(request)
                .map(response -> {
                    // Extract user information from response
                    String userId = extractUserId(response);

                    if (userId != null) {
                        // Create authentication token
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userId,
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                                );

                        // Set authentication in security context
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        logger.info("User {} authenticated successfully with passkey", userId);
                        return ResponseEntity.ok(Map.of(
                                "message", "Login successful",
                                "userId", userId
                        ));
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", "Authentication failed"));
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error finalizing login", error);
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "Authentication failed")));
                });
    }

    private String extractUserId(JsonNode response) {
        try {
            if (response.has("user_id")) {
                return response.get("user_id").asText();
            }
            if (response.has("credential") && response.get("credential").has("user_id")) {
                return response.get("credential").get("user_id").asText();
            }
        } catch (Exception e) {
            logger.error("Error extracting user ID from response", e);
        }
        return null;
    }
}
