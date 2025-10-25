# Passkey Integration Guide

## Overview

This OAuth2 server integrates with Hanko Passkey Server for passwordless authentication. Users can login with either:
- **Password authentication** (traditional username/password)
- **Passkey authentication** (WebAuthn/FIDO2)

Both methods lead to the same passkey management page before OAuth2 authorization.

## Architecture

### Flow Option 1: Password Login
1. User logs in with username/password at auth-server (port 9000)
2. After successful authentication → Redirected to **Passkey Management Page** (`/home`)
3. System automatically checks Hanko server for existing passkeys
4. User can:
   - **Register a new passkey** (if none exists)
   - **Delete existing passkey** (if one exists)
5. User clicks **"Continue to Client Application"** button
6. OAuth2 authorization flow continues → User redirected to client-app (port 9001)

### Flow Option 2: Passkey Login
1. User enters username and clicks **"Sign In with Passkey"** at auth-server (port 9000)
2. Browser prompts for biometric/passkey authentication
3. After successful passkey authentication → Redirected to **Passkey Management Page** (`/home`)
4. System automatically checks Hanko server for existing passkeys
5. User can manage passkeys (register additional or delete existing)
6. User clicks **"Continue to Client Application"** button
7. OAuth2 authorization flow continues → User redirected to client-app (port 9001)

### Hanko Passkey Server

The passkey server is running in Docker:
- **Container**: `docker-compose-passkey-server-1`
- **Ports**: 8000-8001
- **API Base URL**: `http://localhost:8000`

```bash
$ docker ps
CONTAINER ID   IMAGE                           PORTS                                                           NAMES
82ca8a70cc4c   docker-compose-passkey-server   0.0.0.0:8000-8001->8000-8001/tcp                                docker-compose-passkey-server-1
125129a25180   postgres:12-alpine              0.0.0.0:5432->5432/tcp                                          docker-compose-postgresd-1
```

## Configuration

### Environment Variables

The `.env` file in the project root contains Hanko credentials:

```bash
HANKO_TENANT_ID=3a84883a-44a7-4145-81ed-f6eb41ba51de
HANKO_API_KEY=Y5N-IEfokEmxSpx8h4Q5eyqMkCINkqiDe9UUkFVcyOBZ6FUDL8ylZBnqs_-rIwFho6NfMbLOvVrtv6qtybpmqQ==
```

### Application Configuration

[auth-server/src/main/resources/application.yml](auth-server/src/main/resources/application.yml):

```yaml
hanko:
  passkey:
    api-url: http://localhost:8000
    tenant-id: ${HANKO_TENANT_ID:}
    api-key: ${HANKO_API_KEY:}
```

## Running the Application

### Option 1: Using the Convenience Script (Recommended)

```bash
# From project root
./run-auth-server.sh
```

This script automatically:
- Loads environment variables from `.env`
- Starts the auth-server with correct configuration

### Option 2: Manual Export + Run

```bash
# Export environment variables
export HANKO_TENANT_ID=3a84883a-44a7-4145-81ed-f6eb41ba51de
export HANKO_API_KEY=Y5N-IEfokEmxSpx8h4Q5eyqMkCINkqiDe9UUkFVcyOBZ6FUDL8ylZBnqs_-rIwFho6NfMbLOvVrtv6qtybpmqQ==

# Start auth-server
cd auth-server
mvn spring-boot:run

# In another terminal, start client-app
cd client-app
mvn spring-boot:run
```

### Option 3: Using Maven with Environment Variables

```bash
# From auth-server directory
HANKO_TENANT_ID=3a84883a-44a7-4145-81ed-f6eb41ba51de \
HANKO_API_KEY=Y5N-IEfokEmxSpx8h4Q5eyqMkCINkqiDe9UUkFVcyOBZ6FUDL8ylZBnqs_-rIwFho6NfMbLOvVrtv6qtybpmqQ== \
mvn spring-boot:run
```

## Testing the Complete Flow

### Prerequisites
1. Ensure Hanko passkey server is running (check with `docker ps`)
2. Both auth-server and client-app are running
3. Environment variables are set (use `./run-auth-server.sh` or export manually)

### Test Steps

#### Test Case 1: First Time Password Login + Passkey Registration

1. **Navigate to client application**
   ```
   http://localhost:9001
   ```

2. **Initiate OAuth2 login**
   - Click login button
   - Redirected to `http://localhost:9000/login`

3. **Login with password**
   - Username: `user`
   - Password: `password`
   - Click **"Sign In with Password"**

4. **Passkey Management Page** (`/home`)
   - Automatically checks Hanko server
   - Shows "Register a Passkey" button (no passkey exists yet)
   - Click **"Register Passkey"**
   - Browser prompts for passkey creation
   - Follow device prompts (fingerprint, Face ID, PIN, security key, etc.)
   - Passkey registered successfully
   - Page refreshes and shows credential info

5. **Continue to Client Application**
   - Click **"Continue to Client Application"** button
   - Complete OAuth2 consent screen
   - Redirected to `http://localhost:9001/authorized`

#### Test Case 2: Passkey Login (Subsequent Login)

1. **Navigate to client application**
   ```
   http://localhost:9001
   ```

2. **Initiate OAuth2 login**
   - Click login button
   - Redirected to `http://localhost:9000/login`

3. **Login with passkey**
   - Enter username: `user`
   - Click **"Sign In with Passkey"**
   - Browser prompts for passkey authentication
   - Authenticate with fingerprint/Face ID/PIN
   - Automatically logged in ✅

4. **Passkey Management Page** (`/home`)
   - Shows credential information (ID, type, transports)
   - Shows **"Delete Passkey"** button
   - User can:
     - Delete existing passkey
     - Register additional passkeys

5. **Continue to Client Application**
   - Click **"Continue to Client Application"** button
   - Complete OAuth2 consent screen
   - Redirected to `http://localhost:9001/authorized`

#### Test Case 3: Passkey Management (Delete)

1. Login with password (as above)
2. On Passkey Management Page → Click **"Delete Passkey"**
3. Confirm deletion
4. Page refreshes → Shows "Register a Passkey" button again
5. User can now register a new passkey or continue with password-only auth

## API Endpoints

### Passkey Login APIs (Public - No Auth Required)

#### Initialize Passkey Login
```http
POST /api/passkey/login/initialize
Content-Type: application/json

{
  "userId": "user"
}
```
Returns WebAuthn credential request options from Hanko.

#### Finalize Passkey Login
```http
POST /api/passkey/login/finalize
Content-Type: application/json

{
  "id": "credential-id",
  "rawId": "base64url-encoded",
  "type": "public-key",
  "response": {
    "clientDataJSON": "base64url-encoded",
    "authenticatorData": "base64url-encoded",
    "signature": "base64url-encoded",
    "userHandle": "base64url-encoded"
  }
}
```
Returns success/failure and creates authenticated session.

### Passkey Management APIs (Authenticated - Session Required)

#### Check Passkey Status
```http
GET /api/passkey/check
```
Returns credential options from Hanko, including list of registered passkeys.

#### Initialize Passkey Registration
```http
POST /api/passkey/register/initialize
```
Initiates WebAuthn registration ceremony.

#### Finalize Passkey Registration
```http
POST /api/passkey/register/finalize
Content-Type: application/json

{
  "id": "credential-id",
  "rawId": "base64url-encoded",
  "type": "public-key",
  "response": {
    "clientDataJSON": "base64url-encoded",
    "attestationObject": "base64url-encoded"
  }
}
```

#### Delete Passkey
```http
DELETE /api/passkey/credential/{credentialId}
```

## Key Files

### Backend
- [HankoPasskeyService.java](auth-server/src/main/java/com/example/authserver/service/HankoPasskeyService.java) - Hanko API integration
- [PasskeyController.java](auth-server/src/main/java/com/example/authserver/controller/PasskeyController.java) - REST endpoints
- [HankoPasskeyProperties.java](auth-server/src/main/java/com/example/authserver/config/HankoPasskeyProperties.java) - Configuration
- [SecurityConfig.java](auth-server/src/main/java/com/example/authserver/config/SecurityConfig.java) - Security settings

### Frontend
- [login.html](auth-server/src/main/resources/templates/login.html) - Login page with password and passkey options
- [passkey-login.js](auth-server/src/main/resources/static/js/passkey-login.js) - WebAuthn login logic
- [passkey-management.html](auth-server/src/main/resources/templates/passkey-management.html) - Post-login passkey management UI
- [passkey-management.js](auth-server/src/main/resources/static/js/passkey-management.js) - WebAuthn registration/deletion logic

### Configuration
- [application.yml](auth-server/src/main/resources/application.yml) - Application config
- [.env](.env) - Hanko credentials (not committed to git)
- [run-auth-server.sh](run-auth-server.sh) - Convenience script

## Security Considerations

### CSRF Protection
- Passkey API endpoints (`/api/passkey/**`) are excluded from CSRF protection
- This is necessary for WebAuthn API integration
- All endpoints still require authentication

### Session Management
- Password login creates a session
- Session required to access passkey management page
- Session persists through OAuth2 flow

### Hanko API Authentication
- API key sent in `apiKey` header
- Tenant ID included in URL path
- Both loaded from environment variables

## Troubleshooting

### Error: "연결이 거부됨" (Connection refused)

**Symptom:**
```
org.springframework.web.reactive.function.client.WebClientRequestException:
finishConnect(..) failed: 연결이 거부됨: /[0:0:0:0:0:0:0:1]:80
```

**Cause:** Hanko passkey server not running or wrong URL

**Solution:**
1. Check if Hanko server is running:
   ```bash
   docker ps | grep passkey
   ```

2. Verify environment variables are set:
   ```bash
   echo $HANKO_TENANT_ID
   echo $HANKO_API_KEY
   ```

3. Check logs for correct URL:
   ```
   Initializing HankoPasskeyService with API URL: http://localhost:8000
   Request URL: http://localhost:8000/3a84883a-44a7-4145-81ed-f6eb41ba51de/login/initialize
   ```

### Error: "HANKO_TENANT_ID not set"

**Solution:** Use the run script or export environment variables manually.

### WebAuthn Not Working

**Possible causes:**
1. HTTPS required for production (localhost works over HTTP)
2. Browser doesn't support WebAuthn
3. No biometric/security key available

**Check:**
```javascript
if (window.PublicKeyCredential) {
  console.log("WebAuthn supported!");
} else {
  console.log("WebAuthn not supported");
}
```

## Development Notes

### Adding More Logging

To debug Hanko API calls, check these log messages:
```
c.e.a.service.HankoPasskeyService : Initializing HankoPasskeyService with API URL: http://localhost:8000
c.e.a.service.HankoPasskeyService : Tenant ID: 3a84883a-44a7-4145-81ed-f6eb41ba51de
c.e.a.service.HankoPasskeyService : Getting credential options for user: user
c.e.a.service.HankoPasskeyService : Request URL: http://localhost:8000/3a84883a-44a7-4145-81ed-f6eb41ba51de/login/initialize
```

### Testing Without Passkey Server

To skip passkey integration during development, comment out the passkey check in the frontend:
```javascript
// checkPasskeyStatus();  // Comment this out
```

## References

- [Hanko Passkey Server Documentation](https://github.com/teamhanko/passkey-server)
- [WebAuthn Specification](https://www.w3.org/TR/webauthn-2/)
- [Spring Security OAuth2 Authorization Server](https://docs.spring.io/spring-authorization-server/reference/)
