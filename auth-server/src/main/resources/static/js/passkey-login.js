// Passkey Login Script
document.addEventListener('DOMContentLoaded', function() {
    const passkeyLoginForm = document.getElementById('passkeyLoginForm');
    const passkeyUsername = document.getElementById('passkeyUsername');
    const passkeyLoginBtn = document.getElementById('passkeyLoginBtn');
    const statusBox = document.getElementById('passkeyStatus');

    // Utility functions
    function showStatus(message, type = 'info') {
        statusBox.textContent = message;
        statusBox.className = type;
        statusBox.style.display = 'block';
    }

    function hideStatus() {
        statusBox.style.display = 'none';
    }

    function base64urlToUint8Array(base64url) {
        const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i);
        }
        return bytes;
    }

    function uint8ArrayToBase64url(uint8Array) {
        let binary = '';
        for (let i = 0; i < uint8Array.length; i++) {
            binary += String.fromCharCode(uint8Array[i]);
        }
        const base64 = btoa(binary);
        return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
    }

    // Passkey login flow
    async function loginWithPasskey(event) {
        event.preventDefault();

        const userId = passkeyUsername.value.trim();
        if (!userId) {
            showStatus('Please enter your username', 'error');
            return;
        }

        try {
            passkeyLoginBtn.disabled = true;
            showStatus('Initializing passkey login...', 'info');

            // Step 1: Initialize login
            const initResponse = await fetch('/api/passkey/login/initialize', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ userId: userId })
            });

            if (!initResponse.ok) {
                throw new Error('Failed to initialize login');
            }

            const options = await initResponse.json();
            console.log('Login options:', options);

            // Check if user has any passkeys registered
            if (!options.publicKey || !options.publicKey.allowCredentials ||
                options.publicKey.allowCredentials.length === 0) {
                showStatus('No passkey registered for this user. Please register a passkey first or login with password.', 'error');
                passkeyLoginBtn.disabled = false;
                return;
            }

            // Convert base64url strings to Uint8Array
            const publicKeyOptions = {
                ...options.publicKey,
                challenge: base64urlToUint8Array(options.publicKey.challenge),
                allowCredentials: options.publicKey.allowCredentials.map(cred => ({
                    ...cred,
                    id: base64urlToUint8Array(cred.id)
                }))
            };

            showStatus('Please authenticate with your passkey...', 'info');

            // Step 2: Get credential
            const credential = await navigator.credentials.get({
                publicKey: publicKeyOptions
            });

            if (!credential) {
                throw new Error('Failed to get credential');
            }

            console.log('Credential retrieved:', credential);

            // Step 3: Prepare credential for server
            const credentialForServer = {
                id: credential.id,
                rawId: uint8ArrayToBase64url(new Uint8Array(credential.rawId)),
                type: credential.type,
                response: {
                    clientDataJSON: uint8ArrayToBase64url(new Uint8Array(credential.response.clientDataJSON)),
                    authenticatorData: uint8ArrayToBase64url(new Uint8Array(credential.response.authenticatorData)),
                    signature: uint8ArrayToBase64url(new Uint8Array(credential.response.signature)),
                    userHandle: credential.response.userHandle ?
                        uint8ArrayToBase64url(new Uint8Array(credential.response.userHandle)) : null
                }
            };

            showStatus('Finalizing login...', 'info');

            // Step 4: Finalize login
            const finalizeResponse = await fetch('/api/passkey/login/finalize', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(credentialForServer)
            });

            if (!finalizeResponse.ok) {
                throw new Error('Failed to finalize login');
            }

            const result = await finalizeResponse.json();
            console.log('Login result:', result);

            if (result.redirectUrl) {
                showStatus('Login successful! Redirecting...', 'info');
                // Redirect to the passkey management page
                setTimeout(() => {
                    window.location.href = result.redirectUrl;
                }, 1000);
            } else {
                showStatus('Login successful!', 'info');
                setTimeout(() => {
                    window.location.href = '/home';
                }, 1000);
            }

        } catch (error) {
            console.error('Error during passkey login:', error);
            let errorMessage = 'Failed to login with passkey';

            if (error.name === 'NotAllowedError') {
                errorMessage = 'Authentication cancelled or timeout';
            } else if (error.name === 'InvalidStateError') {
                errorMessage = 'Passkey authentication failed';
            } else if (error.message) {
                errorMessage = error.message;
            }

            showStatus(errorMessage, 'error');
            passkeyLoginBtn.disabled = false;
        }
    }

    // Event listener
    if (passkeyLoginForm) {
        passkeyLoginForm.addEventListener('submit', loginWithPasskey);
    }
});
