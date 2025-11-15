package com.example.authserver.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class PasskeyAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private final String credentialId;

    /**
     * Creates a token with the supplied array of authorities.
     *
     * @param authorities the collection of <tt>GrantedAuthority</tt>s for the principal
     *                    represented by this authentication object.
     */
    public PasskeyAuthenticationToken(Object principal, String credentialId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentialId = credentialId;
        setAuthenticated(true);
    }

    public String getCredentialId() {
        return credentialId;
    }

    @Override
    public Object getCredentials() {
        return null; // We don't consider the credentialId a "credential" in the password sense.
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }
}
