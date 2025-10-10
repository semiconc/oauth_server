// Utility functions for base64url encoding/decoding
function base64urlToBuffer(base64url) {
    const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + (4 - base64.length % 4) % 4, '=');
    const binary = atob(padded);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes.buffer;
}

function bufferToBase64url(buffer) {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (let i = 0; i < bytes.length; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    const base64 = btoa(binary);
    return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

function showStatus(message, isError = false) {
    const statusDiv = document.getElementById('status');
    statusDiv.textContent = message;
    statusDiv.style.display = 'block';
    statusDiv.style.background = isError ? '#fee' : '#efe';
    statusDiv.style.color = isError ? '#c33' : '#3c3';
    statusDiv.style.borderLeft = isError ? '4px solid #c33' : '4px solid #3c3';
}

// Check if WebAuthn is supported
if (!window.PublicKeyCredential) {
    showStatus('Passkeys are not supported in this browser', true);
    document.getElementById('registerBtn').disabled = true;
}

// Passkey Registration
document.getElementById('registerBtn').addEventListener('click', async () => {
    const registerBtn = document.getElementById('registerBtn');

    try {
        registerBtn.disabled = true;
        registerBtn.innerHTML = 'Registering...<span class="loader"></span>';

        // Step 1: Initialize registration
        const initResponse = await fetch('/api/passkey/register/initialize', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (!initResponse.ok) {
            if (initResponse.status === 401) {
                throw new Error('You must be logged in to register a passkey');
            }
            throw new Error('Failed to initialize passkey registration');
        }

        const response = await initResponse.json();

        // Extract publicKey from response (Hanko wraps it in a publicKey object)
        const options = response.publicKey || response;

        // Convert challenge and user.id from base64url to buffer
        const publicKeyCredentialCreationOptions = {
            challenge: base64urlToBuffer(options.challenge),
            rp: {
                name: options.rp.name,
                id: options.rp.id || window.location.hostname
            },
            user: {
                id: base64urlToBuffer(options.user.id),
                name: options.user.name,
                displayName: options.user.displayName || options.user.name
            },
            pubKeyCredParams: options.pubKeyCredParams,
            timeout: options.timeout || 60000,
            attestation: options.attestation || 'none',
            authenticatorSelection: options.authenticatorSelection || {
                authenticatorAttachment: 'platform',
                requireResidentKey: false,
                userVerification: 'preferred'
            }
        };

        if (options.excludeCredentials && options.excludeCredentials.length > 0) {
            publicKeyCredentialCreationOptions.excludeCredentials = options.excludeCredentials.map(cred => ({
                id: base64urlToBuffer(cred.id),
                type: cred.type,
                transports: cred.transports
            }));
        }

        // Step 2: Create credential
        const credential = await navigator.credentials.create({
            publicKey: publicKeyCredentialCreationOptions
        });

        if (!credential) {
            throw new Error('No credential created');
        }

        // Step 3: Prepare credential response (send directly without wrapping in credential object)
        const credentialResponse = {
            id: credential.id,
            rawId: bufferToBase64url(credential.rawId),
            type: credential.type,
            response: {
                attestationObject: bufferToBase64url(credential.response.attestationObject),
                clientDataJSON: bufferToBase64url(credential.response.clientDataJSON)
            }
        };

        if (credential.response.getTransports) {
            credentialResponse.transports = credential.response.getTransports();
        }

        // Step 4: Finalize registration (send credential directly, not wrapped)
        const finalizeResponse = await fetch('/api/passkey/register/finalize', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(credentialResponse)
        });

        if (!finalizeResponse.ok) {
            throw new Error('Failed to finalize passkey registration');
        }

        const result = await finalizeResponse.json();

        showStatus('Passkey registered successfully! You can now use it to sign in.');

        setTimeout(() => {
            window.location.href = '/';
        }, 2000);

    } catch (error) {
        console.error('Passkey registration error:', error);
        showStatus('Passkey registration failed: ' + error.message, true);
        registerBtn.disabled = false;
        registerBtn.innerHTML = 'Register Passkey';
    }
});
