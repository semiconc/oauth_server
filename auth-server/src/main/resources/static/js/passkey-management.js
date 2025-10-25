// Passkey Management Script
document.addEventListener('DOMContentLoaded', async function() {
    const statusBox = document.getElementById('statusBox');
    const passkeySection = document.getElementById('passkeySection');
    const registrationSection = document.getElementById('registrationSection');
    const deleteSection = document.getElementById('deleteSection');
    const registerBtn = document.getElementById('registerBtn');
    const deleteBtn = document.getElementById('deleteBtn');
    const credentialInfo = document.getElementById('credentialInfo');

    let currentCredentialId = null;

    // Utility functions
    function showStatus(message, type = 'info') {
        statusBox.textContent = message;
        statusBox.className = `status-box ${type}`;
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

    // Check for existing passkeys
    async function checkPasskeyStatus() {
        try {
            const response = await fetch('/api/passkey/check', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('Failed to check passkey status');
            }

            const data = await response.json();
            console.log('Credential options:', data);

            passkeySection.classList.add('hidden');

            // Check if there are any credentials in the allowCredentials array
            if (data.publicKey && data.publicKey.allowCredentials && data.publicKey.allowCredentials.length > 0) {
                // User has passkeys registered
                const credential = data.publicKey.allowCredentials[0];
                currentCredentialId = credential.id;

                credentialInfo.innerHTML = `
                    <strong>Credential ID:</strong> ${credential.id.substring(0, 20)}...<br>
                    <strong>Type:</strong> ${credential.type}<br>
                    <strong>Transports:</strong> ${credential.transports ? credential.transports.join(', ') : 'N/A'}
                `;

                deleteSection.classList.remove('hidden');
                showStatus('You have a passkey registered.', 'success');
            } else {
                // No passkeys registered
                registrationSection.classList.remove('hidden');
                showStatus('No passkey found. You can register one now.', 'info');
            }
        } catch (error) {
            console.error('Error checking passkey status:', error);
            passkeySection.classList.add('hidden');
            showStatus('Unable to check passkey status. Please try again later.', 'error');
        }
    }

    // Register passkey
    async function registerPasskey() {
        try {
            registerBtn.disabled = true;
            showStatus('Initializing passkey registration...', 'info');

            // Step 1: Initialize registration
            const initResponse = await fetch('/api/passkey/register/initialize', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!initResponse.ok) {
                throw new Error('Failed to initialize registration');
            }

            const options = await initResponse.json();
            console.log('Registration options:', options);

            // Convert base64url strings to Uint8Array
            const publicKeyOptions = {
                ...options.publicKey,
                challenge: base64urlToUint8Array(options.publicKey.challenge),
                user: {
                    ...options.publicKey.user,
                    id: base64urlToUint8Array(options.publicKey.user.id)
                }
            };

            if (options.publicKey.excludeCredentials) {
                publicKeyOptions.excludeCredentials = options.publicKey.excludeCredentials.map(cred => ({
                    ...cred,
                    id: base64urlToUint8Array(cred.id)
                }));
            }

            showStatus('Please follow the passkey prompt on your device...', 'info');

            // Step 2: Create credential
            const credential = await navigator.credentials.create({
                publicKey: publicKeyOptions
            });

            if (!credential) {
                throw new Error('Failed to create credential');
            }

            console.log('Credential created:', credential);

            // Step 3: Prepare credential for server
            const credentialForServer = {
                id: credential.id,
                rawId: uint8ArrayToBase64url(new Uint8Array(credential.rawId)),
                type: credential.type,
                response: {
                    clientDataJSON: uint8ArrayToBase64url(new Uint8Array(credential.response.clientDataJSON)),
                    attestationObject: uint8ArrayToBase64url(new Uint8Array(credential.response.attestationObject))
                }
            };

            showStatus('Finalizing registration...', 'info');

            // Step 4: Finalize registration
            const finalizeResponse = await fetch('/api/passkey/register/finalize', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(credentialForServer)
            });

            if (!finalizeResponse.ok) {
                throw new Error('Failed to finalize registration');
            }

            const result = await finalizeResponse.json();
            console.log('Registration result:', result);

            showStatus('Passkey registered successfully! Refreshing...', 'success');

            // Refresh the page to check status again
            setTimeout(() => {
                location.reload();
            }, 1500);

        } catch (error) {
            console.error('Error registering passkey:', error);
            showStatus(`Failed to register passkey: ${error.message}`, 'error');
            registerBtn.disabled = false;
        }
    }

    // Delete passkey
    async function deletePasskey() {
        if (!currentCredentialId) {
            showStatus('No credential ID found.', 'error');
            return;
        }

        if (!confirm('Are you sure you want to delete your passkey?')) {
            return;
        }

        try {
            deleteBtn.disabled = true;
            showStatus('Deleting passkey...', 'info');

            const response = await fetch(`/api/passkey/credential/${encodeURIComponent(currentCredentialId)}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('Failed to delete passkey');
            }

            const result = await response.json();
            console.log('Delete result:', result);

            showStatus('Passkey deleted successfully! Refreshing...', 'success');

            // Refresh the page to check status again
            setTimeout(() => {
                location.reload();
            }, 1500);

        } catch (error) {
            console.error('Error deleting passkey:', error);
            showStatus(`Failed to delete passkey: ${error.message}`, 'error');
            deleteBtn.disabled = false;
        }
    }

    // Event listeners
    registerBtn.addEventListener('click', registerPasskey);
    deleteBtn.addEventListener('click', deletePasskey);

    // Initialize
    checkPasskeyStatus();
});
