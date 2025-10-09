package com.example.authserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PasskeyLoginInitRequest {

    @JsonProperty("user_id")
    private String userId;

    public PasskeyLoginInitRequest() {
    }

    public PasskeyLoginInitRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
