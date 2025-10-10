# Passkey Authentication Setup Guide

This guide explains how to set up and use passkey authentication with the OAuth2 Authorization Server.

## Overview

Passkey authentication has been integrated into the auth-server as an alternative login method. Users can authenticate using:
1. **Traditional password login** - Username and password
2. **Passkey authentication** - Biometric (fingerprint, face ID) or device PIN

Passkeys provide stronger security and a better user experience compared to passwords.

## Prerequisites

1. **Hanko Passkey Backend** must be running
   - Follow the setup guide: https://github.com/teamhanko/passkeys/blob/main/server/README.md
   - Default URL: `http://localhost:8000`

2. **Tenant ID and API Key** from Hanko Passkey backend
   - Create a tenant through the Hanko admin API
   - Generate an API key for authentication

## Configuration

### 1. Set Environment Variables

Set the following environment variables before starting the auth-server:

```bash
export HANKO_TENANT_ID=your-tenant-id
export HANKO_API_KEY=your-api-key
```

Or configure them in `auth-server/src/main/resources/application.yml`:

```yaml
hanko:
  passkey:
    api-url: http://localhost:8000
    tenant-id: your-tenant-id
    api-key: your-api-key
```

### 2. Start the Hanko Passkey Backend

```bash
# Navigate to your Hanko passkeys server directory
cd /path/to/passkeys/server

# Start the backend (follow Hanko's documentation)
# This typically runs on port 8000
```

### 3. Start the Auth Server

```bash
cd auth-server
mvn spring-boot:run
```

The auth-server will be available at `http://localhost:9000`.

## Usage

### Registering a Passkey

1. **Login with password first**:
   - Go to `http://localhost:9000/login`
   - Enter username: `user`, password: `password`

2. **Navigate to passkey registration**:
   - After login, you'll be redirected to the home page
   - Click "Register a Passkey"
   - Or visit `http://localhost:9000/register-passkey` directly

3. **Complete passkey registration**:
   - Click "Register Passkey" button
   - Follow your browser's prompts to create a passkey
   - This will use your device's biometric authentication or PIN

4. **Success**:
   - You'll see a success message
   - You can now use this passkey to login

### Logging in with a Passkey

1. **Go to the login page**:
   - Visit `http://localhost:9000/login`

2. **Use passkey authentication**:
   - Enter your username in the "Username for Passkey" field
   - Click "Sign in with Passkey"
   - Follow your browser's prompts to authenticate
   - This will use your registered passkey (fingerprint, face ID, or device PIN)

3. **Success**:
   - You'll be authenticated and redirected to the home page
   - Or continue with the OAuth2 authorization flow if coming from a client app

## Architecture

### Components Added

1. **Configuration**:
   - `HankoPasskeyProperties.java` - Configuration properties for Hanko backend connection

2. **DTOs**:
   - `PasskeyRegistrationInitRequest.java` - Initialize passkey registration
   - `PasskeyLoginInitRequest.java` - Initialize passkey login
   - `PasskeyCredentialResponse.java` - WebAuthn credential response
   - `PasskeyFinalizeRequest.java` - Finalize registration/login

3. **Services**:
   - `HankoPasskeyService.java` - REST client for Hanko Passkey API
     - `initializeRegistration()` - Start registration process
     - `finalizeRegistration()` - Complete registration
     - `initializeLogin()` - Start login process
     - `finalizeLogin()` - Complete login and authenticate user

4. **Controllers**:
   - `PasskeyController.java` - REST API endpoints for passkey operations
     - `POST /api/passkey/register/initialize` - Initialize registration (requires authentication)
     - `POST /api/passkey/register/finalize` - Complete registration
     - `POST /api/passkey/login/initialize` - Initialize login (public)
     - `POST /api/passkey/login/finalize` - Complete login and create session
   - `LoginController.java` - Serves HTML pages
     - `GET /login` - Login page with passkey option
     - `GET /register-passkey` - Passkey registration page

5. **Frontend**:
   - `templates/login.html` - Custom login page with passkey option
   - `templates/register-passkey.html` - Passkey registration page
   - `templates/home.html` - Home page with link to register passkeys
   - `static/js/passkey-auth.js` - WebAuthn authentication logic
   - `static/js/passkey-registration.js` - WebAuthn registration logic

6. **Security Configuration Updates**:
   - Public access to passkey login endpoints
   - Authenticated access required for passkey registration
   - CSRF exemption for passkey API endpoints

## API Endpoints

### Passkey Registration (Requires Authentication)

#### Initialize Registration
```
POST /api/passkey/register/initialize
```
Response: WebAuthn PublicKeyCredentialCreationOptions

#### Finalize Registration
```
POST /api/passkey/register/finalize
Content-Type: application/json

{
  "credential": {
    "id": "...",
    "rawId": "...",
    "type": "public-key",
    "response": {
      "attestationObject": "...",
      "clientDataJSON": "..."
    }
  }
}
```

### Passkey Login (Public)

#### Initialize Login
```
POST /api/passkey/login/initialize
Content-Type: application/json

{
  "userId": "username"
}
```
Response: WebAuthn PublicKeyCredentialRequestOptions

#### Finalize Login
```
POST /api/passkey/login/finalize
Content-Type: application/json

{
  "credential": {
    "id": "...",
    "rawId": "...",
    "type": "public-key",
    "response": {
      "authenticatorData": "...",
      "clientDataJSON": "...",
      "signature": "...",
      "userHandle": "..."
    }
  }
}
```

## Browser Compatibility

Passkeys (WebAuthn) are supported in:
- Chrome/Edge 67+
- Firefox 60+
- Safari 13+
- Mobile browsers (iOS Safari 14+, Chrome Android 70+)

The login page automatically detects WebAuthn support and disables passkey options if not available.

## Security Considerations

1. **HTTPS Required in Production**: WebAuthn requires HTTPS in production environments. Only localhost is allowed to use HTTP.

2. **Session Management**: After successful passkey authentication, a server-side session is created using Spring Security's session management.

3. **CSRF Protection**: Passkey API endpoints have CSRF protection disabled to allow cross-origin authentication flows. This is safe because WebAuthn has built-in origin validation.

4. **User Verification**: The implementation uses "preferred" user verification, which means biometric/PIN verification is preferred but not required.

5. **Credential Storage**: Credentials are stored in the Hanko Passkey backend, not in the auth-server. The auth-server only validates credentials through the Hanko API.

## Troubleshooting

### Passkey registration fails
- Ensure you're logged in with password first
- Check browser compatibility
- Verify Hanko backend is running and accessible
- Check browser console for detailed error messages

### Passkey login fails
- Ensure you've registered a passkey for that username
- Verify the Hanko backend is running
- Check that the tenant ID and API key are correctly configured
- Try using password login as a fallback

### Can't connect to Hanko backend
- Verify `HANKO_TENANT_ID` and `HANKO_API_KEY` environment variables are set
- Check that Hanko backend is running on `http://localhost:8000`
- Review auth-server logs for connection errors

## Testing the Complete OAuth2 Flow with Passkeys

1. Start all services:
   ```bash
   # Terminal 1: Hanko Passkey Backend
   cd /path/to/hanko/passkeys/server
   # Start according to Hanko documentation

   # Terminal 2: Auth Server
   cd auth-server
   mvn spring-boot:run

   # Terminal 3: Client App
   cd client-app
   mvn spring-boot:run
   ```

2. Test passkey authentication in OAuth2 flow:
   - Visit client app: `http://localhost:9001`
   - Click login → redirected to auth-server
   - Use passkey to authenticate (instead of password)
   - Complete OAuth2 consent
   - Redirected back to client app with user info

## Next Steps

- Consider implementing passkey as a second factor (2FA)
- Add user management interface to view/remove registered passkeys
- Implement passkey backup and recovery mechanisms
- Add support for platform authenticators vs. roaming authenticators
