package com.example.authserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PasskeyRegistrationInitRequest {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("display_name")
    private String displayName;

    public PasskeyRegistrationInitRequest() {
    }

    public PasskeyRegistrationInitRequest(String userId, String username, String displayName) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
