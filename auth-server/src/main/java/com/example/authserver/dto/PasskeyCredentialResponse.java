package com.example.authserver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PasskeyCredentialResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("rawId")
    private String rawId;

    @JsonProperty("type")
    private String type;

    @JsonProperty("response")
    private AuthenticatorResponse response;

    public static class AuthenticatorResponse {
        @JsonProperty("clientDataJSON")
        private String clientDataJSON;

        @JsonProperty("authenticatorData")
        private String authenticatorData;

        @JsonProperty("signature")
        private String signature;

        @JsonProperty("userHandle")
        private String userHandle;

        @JsonProperty("attestationObject")
        private String attestationObject;

        // Getters and setters
        public String getClientDataJSON() {
            return clientDataJSON;
        }

        public void setClientDataJSON(String clientDataJSON) {
            this.clientDataJSON = clientDataJSON;
        }

        public String getAuthenticatorData() {
            return authenticatorData;
        }

        public void setAuthenticatorData(String authenticatorData) {
            this.authenticatorData = authenticatorData;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getUserHandle() {
            return userHandle;
        }

        public void setUserHandle(String userHandle) {
            this.userHandle = userHandle;
        }

        public String getAttestationObject() {
            return attestationObject;
        }

        public void setAttestationObject(String attestationObject) {
            this.attestationObject = attestationObject;
        }
    }

    // Getters and setters
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public AuthenticatorResponse getResponse() {
        return response;
    }

    public void setResponse(AuthenticatorResponse response) {
        this.response = response;
    }
}
