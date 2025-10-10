package com.example.authserver.dto;

public class PasskeyFinalizeRequest {

    private PasskeyCredentialResponse credential;

    public PasskeyFinalizeRequest() {
    }

    public PasskeyFinalizeRequest(PasskeyCredentialResponse credential) {
        this.credential = credential;
    }

    public PasskeyCredentialResponse getCredential() {
        return credential;
    }

    public void setCredential(PasskeyCredentialResponse credential) {
        this.credential = credential;
    }
}
