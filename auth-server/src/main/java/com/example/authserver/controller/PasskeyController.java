package com.example.authserver.controller;

import com.example.authserver.service.HankoPasskeyService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.authserver.security.PasskeyAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/passkey")
public class PasskeyController {

    private static final Logger logger = LoggerFactory.getLogger(PasskeyController.class);

    private final HankoPasskeyService passkeyService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
    private final JwtDecoder jwtDecoder;

    public PasskeyController(HankoPasskeyService passkeyService, JwtDecoder jwtDecoder) {
        this.passkeyService = passkeyService;
        this.jwtDecoder = jwtDecoder;
    }

    /**
     * Check if user has registered passkeys
     */
    @GetMapping("/check")
    public Mono<ResponseEntity<JsonNode>> checkCredentials() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String username = authentication.getName();

        return passkeyService.checkCredentials(username)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    logger.error("Error checking credentials", error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
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

        return passkeyService.initializeRegistration(username, username, username)
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
            @RequestBody JsonNode credential) {

        return passkeyService.finalizeRegistration(credential)
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
     * Delete a passkey credential
     */
    @DeleteMapping("/credential/{credentialId}")
    public Mono<ResponseEntity<Map<String, String>>> deleteCredential(@PathVariable String credentialId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return passkeyService.deleteCredential(credentialId)
                .then(Mono.just(ResponseEntity.ok(Map.of("message", "Passkey deleted successfully"))))
                .onErrorResume(error -> {
                    logger.error("Error deleting credential", error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Failed to delete passkey")));
                });
    }

    /**
     * Initialize passkey login (for non-authenticated users)
     */
    @PostMapping("/login/initialize")
    public Mono<ResponseEntity<JsonNode>> initializeLogin(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");

        if (userId == null || userId.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        logger.info("Initializing passkey login for user: {}", userId);

        return passkeyService.getCredentialOptions(userId)
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
            @RequestBody JsonNode credential,
            HttpServletRequest request,
            HttpServletResponse response) {

        logger.info("Finalizing passkey login");

        return passkeyService.finalizeLogin(credential)
                .map(jsonResponse -> {
                    // Extract user information from response
                    // Extract user information from response
                    Map<String, String> userInfo = extractUserInfo(jsonResponse);

                    if (userInfo != null && userInfo.get("userId") != null) {
                        String userId = userInfo.get("userId");
                        String credentialId = userInfo.get("credentialId");

                        // Create authentication token with USER role
                        PasskeyAuthenticationToken authToken =
                                new PasskeyAuthenticationToken(
                                        userId,
                                        credentialId,
                                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                                );

                        // Create security context and set authentication
                        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                        securityContext.setAuthentication(authToken);
                        SecurityContextHolder.setContext(securityContext);

                        // Save security context to session (same as form login)
                        securityContextRepository.saveContext(securityContext, request, response);

                        logger.info("User {} authenticated successfully with passkey", userId);

                        // Check if there's a saved request (e.g., OAuth2 authorize request)
                        SavedRequest savedRequest = requestCache.getRequest(request, response);
                        String redirectUrl = "/home";  // Default fallback

                        if (savedRequest != null) {
                            redirectUrl = savedRequest.getRedirectUrl();
                            logger.info("Redirecting to saved request: {}", redirectUrl);
                            requestCache.removeRequest(request, response);  // Clear saved request
                        }

                        return ResponseEntity.ok(Map.of(
                                "message", "Login successful",
                                "userId", userId,
                                "redirectUrl", redirectUrl
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

    private Map<String, String> extractUserInfo(JsonNode response) {
        try {
            if (response.has("token")) {
                String token = response.get("token").asText();
                return parseAndValidateJwt(token);
            }

            // Fallback for non-JWT responses (if any)
            if (response.has("credential") && response.get("credential").has("user_id")) {
                return Map.of("userId", response.get("credential").get("user_id").asText());
            }
            if (response.has("user_id")) {
                return Map.of("userId", response.get("user_id").asText());
            }

        } catch (Exception e) {
            logger.error("Error extracting user info from response", e);
        }
        return null;
    }

    private Map<String, String> parseAndValidateJwt(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String userId = jwt.getSubject();
            String credentialId = jwt.getClaimAsString("credential_id");

            logger.info("Successfully validated JWT and extracted user ID '{}' and credential ID '{}'", userId, credentialId);
            return Map.of("userId", userId, "credentialId", credentialId);
        } catch (Exception e) {
            logger.error("Failed to validate JWT token", e);
        }
        return null;
    }
}
