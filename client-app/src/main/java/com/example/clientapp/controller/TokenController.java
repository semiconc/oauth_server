package com.example.clientapp.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TokenController {

    @GetMapping("/tokens")
    public Map<String, Object> getTokens(
            @RegisteredOAuth2AuthorizedClient("client-app") OAuth2AuthorizedClient authorizedClient,
            @AuthenticationPrincipal OidcUser oidcUser) {

        Map<String, Object> response = new HashMap<>();

        // Access Token
        Map<String, Object> accessTokenInfo = new HashMap<>();
        accessTokenInfo.put("token_value", authorizedClient.getAccessToken().getTokenValue());
        accessTokenInfo.put("token_type", authorizedClient.getAccessToken().getTokenType().getValue());
        accessTokenInfo.put("scopes", authorizedClient.getAccessToken().getScopes());
        accessTokenInfo.put("issued_at", authorizedClient.getAccessToken().getIssuedAt());
        accessTokenInfo.put("expires_at", authorizedClient.getAccessToken().getExpiresAt());
        response.put("access_token", accessTokenInfo);

        // ID Token
        if (oidcUser != null && oidcUser.getIdToken() != null) {
            Map<String, Object> idTokenInfo = new HashMap<>();
            idTokenInfo.put("token_value", oidcUser.getIdToken().getTokenValue());
            idTokenInfo.put("claims", oidcUser.getIdToken().getClaims());
            idTokenInfo.put("issued_at", oidcUser.getIdToken().getIssuedAt());
            idTokenInfo.put("expires_at", oidcUser.getIdToken().getExpiresAt());
            response.put("id_token", idTokenInfo);
        }

        // Refresh Token (if available)
        if (authorizedClient.getRefreshToken() != null) {
            Map<String, Object> refreshTokenInfo = new HashMap<>();
            refreshTokenInfo.put("token_value", authorizedClient.getRefreshToken().getTokenValue());
            refreshTokenInfo.put("issued_at", authorizedClient.getRefreshToken().getIssuedAt());
            response.put("refresh_token", refreshTokenInfo);
        }

        return response;
    }
}
