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
    const statusDiv = document.getElementById('passkeyStatus');
    statusDiv.textContent = message;
    statusDiv.style.display = 'block';
    statusDiv.style.background = isError ? '#fee' : '#efe';
    statusDiv.style.color = isError ? '#c33' : '#3c3';
    statusDiv.style.borderLeft = isError ? '4px solid #c33' : '4px solid #3c3';

    setTimeout(() => {
        statusDiv.style.display = 'none';
    }, 5000);
}

// Passkey Login
document.getElementById('passkeyLoginForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const username = document.getElementById('passkeyUsername').value;
    const loginBtn = document.getElementById('passkeyLoginBtn');

    if (!username) {
        showStatus('Please enter a username', true);
        return;
    }

    try {
        loginBtn.disabled = true;
        loginBtn.innerHTML = 'Authenticating...<span class="loader"></span>';

        // Step 1: Initialize login
        const initResponse = await fetch('/api/passkey/login/initialize', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ userId: username })
        });

        if (!initResponse.ok) {
            throw new Error('Failed to initialize passkey login');
        }

        const response = await initResponse.json();

        // Extract publicKey from response (Hanko wraps it in a publicKey object)
        const options = response.publicKey || response;

        // Convert challenge from base64url to buffer
        const publicKeyCredentialRequestOptions = {
            challenge: base64urlToBuffer(options.challenge),
            timeout: options.timeout || 60000,
            rpId: options.rpId || window.location.hostname,
            userVerification: options.userVerification || 'preferred'
        };

        if (options.allowCredentials && options.allowCredentials.length > 0) {
            publicKeyCredentialRequestOptions.allowCredentials = options.allowCredentials.map(cred => ({
                id: base64urlToBuffer(cred.id),
                type: cred.type,
                transports: cred.transports
            }));
        }

        // Step 2: Get credential from authenticator
        const credential = await navigator.credentials.get({
            publicKey: publicKeyCredentialRequestOptions
        });

        if (!credential) {
            throw new Error('No credential received');
        }

        // Step 3: Prepare credential response
        const credentialResponse = {
            id: credential.id,
            rawId: bufferToBase64url(credential.rawId),
            type: credential.type,
            response: {
                authenticatorData: bufferToBase64url(credential.response.authenticatorData),
                clientDataJSON: bufferToBase64url(credential.response.clientDataJSON),
                signature: bufferToBase64url(credential.response.signature),
                userHandle: credential.response.userHandle ? bufferToBase64url(credential.response.userHandle) : null
            }
        };

        // Step 4: Finalize login (send credential directly, not wrapped)
        const finalizeResponse = await fetch('/api/passkey/login/finalize', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(credentialResponse)
        });

        if (!finalizeResponse.ok) {
            throw new Error('Failed to finalize passkey login');
        }

        const result = await finalizeResponse.json();

        showStatus('Login successful! Redirecting...');

        // Redirect to continue the OAuth flow or dashboard
        setTimeout(() => {
            window.location.href = '/';
        }, 1000);

    } catch (error) {
        console.error('Passkey login error:', error);
        showStatus('Passkey login failed: ' + error.message, true);
        loginBtn.disabled = false;
        loginBtn.innerHTML = 'Sign in with Passkey';
    }
});

// Passkey Registration
document.getElementById('registerPasskeyLink').addEventListener('click', async (e) => {
    e.preventDefault();

    const registerBtn = e.target;
    registerBtn.textContent = 'Registering...';

    try {
        // Check if user is already authenticated
        const username = document.getElementById('username').value;

        if (!username) {
            showStatus('Please login with password first to register a passkey', true);
            registerBtn.textContent = 'Register a new passkey';
            return;
        }

        // For registration, user must be authenticated first
        showStatus('Please login with password first to register a passkey', true);
        registerBtn.textContent = 'Register a new passkey';

    } catch (error) {
        console.error('Passkey registration error:', error);
        showStatus('Passkey registration failed: ' + error.message, true);
        registerBtn.textContent = 'Register a new passkey';
    }
});

// Check if WebAuthn is supported
if (!window.PublicKeyCredential) {
    showStatus('Passkeys are not supported in this browser', true);
    document.getElementById('passkeyLoginBtn').disabled = true;
    document.getElementById('registerPasskeyLink').style.display = 'none';
}
