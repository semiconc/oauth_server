package com.example.authserver.service;

import com.example.authserver.config.HankoPasskeyProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class HankoPasskeyService {

    private static final Logger logger = LoggerFactory.getLogger(HankoPasskeyService.class);

    private final WebClient webClient;
    private final HankoPasskeyProperties properties;

    public HankoPasskeyService(HankoPasskeyProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        String apiUrl = properties.getApiUrl();
        logger.info("Initializing HankoPasskeyService with API URL: {}", apiUrl);
        logger.info("Tenant ID: {}", properties.getTenantId());

        this.webClient = webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeader("apiKey", properties.getApiKey() != null ? properties.getApiKey() : "")
                .build();
    }

    /**
     * Get credential options for a user to check if they have registered passkeys
     */
    public Mono<JsonNode> getCredentialOptions(String userId) {
        String url = String.format("/%s/login/initialize", properties.getTenantId());
        String fullUrl = properties.getApiUrl() + url;

        logger.info("Getting credential options for user: {}", userId);
        logger.info("Request URL: {}", fullUrl);

        return webClient.post()
                .uri(url)
                .bodyValue(Map.of("user_id", userId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(response -> logger.info("Successfully got credential options"))
                .doOnError(error -> logger.error("Failed to get credential options from URL: {}", fullUrl, error));
    }

    /**
     * Initialize passkey registration
     */
    public Mono<JsonNode> initializeRegistration(String userId, String username, String displayName) {
        String url = String.format("/%s/registration/initialize", properties.getTenantId());

        logger.info("Initializing passkey registration for user: {}", username);

        return webClient.post()
                .uri(url)
                .bodyValue(Map.of(
                        "user_id", userId,
                        "username", username,
                        "display_name", displayName
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(error -> logger.error("Failed to initialize registration", error));
    }

    /**
     * Finalize passkey registration
     */
    public Mono<JsonNode> finalizeRegistration(JsonNode credential) {
        String url = String.format("/%s/registration/finalize", properties.getTenantId());

        logger.info("Finalizing passkey registration");

        return webClient.post()
                .uri(url)
                .bodyValue(credential)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(error -> logger.error("Failed to finalize registration", error));
    }

    /**
     * Finalize passkey login
     */
    public Mono<JsonNode> finalizeLogin(JsonNode credential) {
        String url = String.format("/%s/login/finalize", properties.getTenantId());
        String fullUrl = properties.getApiUrl() + url;

        logger.info("Finalizing passkey login");
        logger.info("Request URL: {}", fullUrl);
        logger.info("Credential payload: {}", credential.toString());

        return webClient.post()
                .uri(url)
                .bodyValue(credential)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> {
                            logger.error("Hanko server returned error status: {}", clientResponse.statusCode());
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        logger.error("Error response body: {}", errorBody);
                                        return Mono.error(new RuntimeException(
                                                String.format("Hanko server error: %s - %s",
                                                        clientResponse.statusCode(), errorBody)
                                        ));
                                    });
                        })
                .bodyToMono(JsonNode.class)
                .doOnSuccess(response -> {
                    logger.info("Passkey login finalized successfully");
                    logger.info("Response: {}", response.toString());
                })
                .doOnError(error -> {
                    logger.error("Failed to finalize login to URL: {}", fullUrl);
                    logger.error("Error details: ", error);
                });
    }

    /**
     * Check if user has registered credentials by attempting login initialization
     * Returns credential options if passkeys exist, or handles errors gracefully
     */
    public Mono<JsonNode> checkCredentials(String userId) {
        String url = String.format("/%s/login/initialize", properties.getTenantId());
        String fullUrl = properties.getApiUrl() + url;

        logger.info("Checking credentials for user: {}", userId);
        logger.info("Request URL: {}", fullUrl);

        return webClient.post()
                .uri(url)
                .bodyValue(Map.of("user_id", userId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(response -> logger.info("Successfully retrieved credential options"))
                .onErrorResume(error -> {
                    logger.info("No credentials found for user or error occurred: {}", error.getMessage());
                    // Return empty credentials list when error occurs (likely no passkeys registered)
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.node.ObjectNode emptyResponse = mapper.createObjectNode();
                    com.fasterxml.jackson.databind.node.ObjectNode publicKey = mapper.createObjectNode();
                    publicKey.set("allowCredentials", mapper.createArrayNode());
                    emptyResponse.set("publicKey", publicKey);
                    return Mono.just(emptyResponse);
                });
    }

    /**
     * Delete a passkey credential
     */
    public Mono<Void> deleteCredential(String credentialId) {
        String url = String.format("/%s/credentials/%s", properties.getTenantId(), credentialId);

        logger.info("Deleting credential: {}", credentialId);

        return webClient.delete()
                .uri(url)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error -> logger.error("Failed to delete credential", error));
    }
}
