package com.example.authserver.service;

import com.example.authserver.config.HankoPasskeyProperties;
import com.example.authserver.dto.PasskeyFinalizeRequest;
import com.example.authserver.dto.PasskeyLoginInitRequest;
import com.example.authserver.dto.PasskeyRegistrationInitRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class HankoPasskeyService {

    private static final Logger logger = LoggerFactory.getLogger(HankoPasskeyService.class);

    private final WebClient webClient;
    private final HankoPasskeyProperties properties;

    public HankoPasskeyService(HankoPasskeyProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder
                .baseUrl(properties.getApiUrl())
                .defaultHeader("apiKey", properties.getApiKey() != null ? properties.getApiKey() : "")
                .build();
    }

    /**
     * Initialize passkey registration
     */
    public Mono<JsonNode> initializeRegistration(PasskeyRegistrationInitRequest request) {
        String url = String.format("/%s/registration/initialize", properties.getTenantId());

        logger.info("Initializing passkey registration for user: {}", request.getUsername());

        return webClient.post()
                .uri(url)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(error -> logger.error("Failed to initialize registration", error));
    }

    /**
     * Finalize passkey registration
     */
    public Mono<JsonNode> finalizeRegistration(PasskeyFinalizeRequest request) {
        String url = String.format("/%s/registration/finalize", properties.getTenantId());

        logger.info("Finalizing passkey registration");

        // Send the credential directly (unwrapped) to Hanko API
        return webClient.post()
                .uri(url)
                .bodyValue(request.getCredential())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(error -> logger.error("Failed to finalize registration", error));
    }

    /**
     * Initialize passkey login
     */
    public Mono<JsonNode> initializeLogin(PasskeyLoginInitRequest request) {
        String url = String.format("/%s/login/initialize", properties.getTenantId());

        logger.info("Initializing passkey login for user: {}", request.getUserId());

        return webClient.post()
                .uri(url)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(error -> logger.error("Failed to initialize login", error));
    }

    /**
     * Finalize passkey login
     */
    public Mono<JsonNode> finalizeLogin(PasskeyFinalizeRequest request) {
        String url = String.format("/%s/login/finalize", properties.getTenantId());

        logger.info("Finalizing passkey login");

        // Send the credential directly (unwrapped) to Hanko API
        return webClient.post()
                .uri(url)
                .bodyValue(request.getCredential())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(response -> logger.info("Passkey login finalized successfully"))
                .doOnError(error -> logger.error("Failed to finalize login", error));
    }
}
