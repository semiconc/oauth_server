package com.example.authserver.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class PasskeyCredentialResponse {

    private String id;
    private String rawId;
    private JsonNode response;
    private String type;
    private JsonNode clientExtensionResults;
    private JsonNode authenticatorAttachment;

    public PasskeyCredentialResponse() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRawId() {
        return rawId;
    }

    public void setRawId(String rawId) {
        this.rawId = rawId;
    }

    public JsonNode getResponse() {
        return response;
    }

    public void setResponse(JsonNode response) {
        this.response = response;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JsonNode getClientExtensionResults() {
        return clientExtensionResults;
    }

    public void setClientExtensionResults(JsonNode clientExtensionResults) {
        this.clientExtensionResults = clientExtensionResults;
    }

    public JsonNode getAuthenticatorAttachment() {
        return authenticatorAttachment;
    }

    public void setAuthenticatorAttachment(JsonNode authenticatorAttachment) {
        this.authenticatorAttachment = authenticatorAttachment;
    }
}
