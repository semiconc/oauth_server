# Hanko Passkeys Quick Start Guide

This guide will help you set up the Hanko Passkeys backend and integrate it with your OAuth2 Authorization Server.

## Prerequisites

- Docker (for running Hanko Passkeys backend)
- Or Hanko Passkeys backend running from source

## Step 1: Start Hanko Passkeys Backend

### Using Docker

```bash
# Pull the Hanko Passkeys image
docker pull ghcr.io/teamhanko/passkeys-server:latest

# Run the backend
docker run -d \
  --name hanko-passkeys \
  -p 8000:8000 \
  -p 8001:8001 \
  ghcr.io/teamhanko/passkeys-server:latest
```

### Using Source

If you're running from source (as per the Hanko README):

```bash
cd /path/to/passkeys/server
# Follow the instructions in https://github.com/teamhanko/passkeys/blob/main/server/README.md
# Ensure ports 8000 (public API) and 8001 (admin API) are accessible
```

### Verify Hanko is Running

```bash
# Check public API (port 8000)
curl http://localhost:8000/health/alive

# Check admin API (port 8001)
curl http://localhost:8001/health/alive

# Both should return: {"status":"alive"}
```

## Step 2: Create Tenant and API Key

We've provided automated scripts to set up your tenant and generate an API key.

### On Linux/Mac:

```bash
cd /mnt/wsl/data/workspaces/oauth_server
./scripts/setup-hanko-tenant.sh
```

### On Windows (PowerShell):

```powershell
cd C:\path\to\oauth_server
.\scripts\setup-hanko-tenant.ps1
```

### Manual Setup (if scripts don't work)

If the automated scripts fail, you can set up manually using curl:

#### 1. Create a Tenant

```bash
curl -X POST http://localhost:8001/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "OAuth Server",
    "config": {
      "cors": {
        "allowed_origins": ["http://localhost:9000"],
        "unsafe_wildcard_origin_allowed": false
      },
      "webauthn": {
        "relying_party": {
          "id": "localhost",
          "display_name": "OAuth2 Authorization Server",
          "origins": ["http://localhost:9000"]
        },
        "timeout": 60000,
        "user_verification": "preferred"
      }
    }
  }'
```

Save the `id` from the response - this is your `TENANT_ID`.

#### 2. Generate API Key

Replace `<TENANT_ID>` with your tenant ID from step 1:

```bash
curl -X POST http://localhost:8001/tenants/<TENANT_ID>/secrets/api \
  -H "Content-Type: application/json"
```

Save the `secret` from the response - this is your `API_KEY`.

## Step 3: Configure Auth-Server

Set the environment variables with the values from Step 2:

### Linux/Mac:

```bash
export HANKO_TENANT_ID="your-tenant-id-here"
export HANKO_API_KEY="your-api-key-here"
```

### Windows (PowerShell):

```powershell
$env:HANKO_TENANT_ID = "your-tenant-id-here"
$env:HANKO_API_KEY = "your-api-key-here"
```

### Or Update application.yml

Alternatively, edit `auth-server/src/main/resources/application.yml`:

```yaml
hanko:
  passkey:
    api-url: http://localhost:8000
    tenant-id: your-tenant-id-here
    api-key: your-api-key-here
```

## Step 4: Start Auth-Server

```bash
cd auth-server
mvn spring-boot:run
```

The auth-server will start on port 9000.

## Step 5: Test Passkey Authentication

### 1. Access the Login Page

Open your browser to: http://localhost:9000/login

### 2. Login with Password First

- Username: `user`
- Password: `password`

### 3. Register a Passkey

After logging in, you'll be redirected to the home page. Click "Register a Passkey" or go to:
http://localhost:9000/register-passkey

Follow your browser's prompts to:
- Use Touch ID / Face ID (Mac)
- Use Windows Hello (Windows)
- Use fingerprint sensor (Android/iOS)
- Use a security key

### 4. Test Passkey Login

1. Logout from the home page
2. Go back to http://localhost:9000/login
3. Enter your username in the "Username for Passkey" field
4. Click "Sign in with Passkey"
5. Authenticate using your registered passkey

## Step 6: Test with OAuth2 Flow

### 1. Start the Client App

```bash
cd client-app
mvn spring-boot:run
```

### 2. Test OAuth2 Flow with Passkey

1. Visit http://localhost:9001
2. Click login - you'll be redirected to auth-server
3. Use passkey to authenticate (instead of password)
4. Approve OAuth2 consent
5. You'll be redirected back to client-app with user info

## Troubleshooting

### "Hanko Admin API is not accessible"

**Solution:** Ensure Hanko backend is running and port 8001 is accessible:
```bash
curl http://localhost:8001/health/alive
```

### "Failed to create tenant"

**Solution:** Check Hanko backend logs for errors. Ensure it's running properly.

### "Passkey registration failed"

**Common causes:**
1. **Browser not supported**: Use Chrome 67+, Firefox 60+, Safari 13+
2. **HTTPS required**: Passkeys only work on localhost (HTTP) or HTTPS domains
3. **No authenticator available**: Ensure your device has biometric or security key

### "Passkey login failed: Authentication failed"

**Common causes:**
1. **No passkey registered**: Register a passkey first
2. **Wrong username**: Ensure you're using the username you registered with
3. **Hanko backend not running**: Check if Hanko is accessible
4. **Tenant/API key mismatch**: Verify environment variables are correct

### View Detailed Logs

Enable debug logging in `auth-server/src/main/resources/application.yml`:

```yaml
logging:
  level:
    com.example.authserver: DEBUG
    com.example.authserver.service.HankoPasskeyService: TRACE
```

## Port Summary

- **8000**: Hanko Public API (used by browser for WebAuthn)
- **8001**: Hanko Admin API (used for tenant/key management)
- **9000**: Auth-Server (OAuth2 Authorization Server)
- **9001**: Client-App (OAuth2 Client Application)

## Security Notes

### Development vs Production

The current setup is for **development only**. For production:

1. **Use HTTPS**: WebAuthn requires HTTPS in production
2. **Secure API Keys**: Use secrets management (not environment variables)
3. **Update Relying Party ID**: Change from `localhost` to your domain
4. **Update CORS Origins**: Restrict to your production domains
5. **Database Backend**: Configure Hanko with PostgreSQL/MySQL instead of in-memory

### Example Production Config

```yaml
hanko:
  passkey:
    api-url: https://passkeys.yourdomain.com
    tenant-id: ${HANKO_TENANT_ID}  # From secrets manager
    api-key: ${HANKO_API_KEY}       # From secrets manager
```

Update tenant config for production:

```json
{
  "config": {
    "webauthn": {
      "relying_party": {
        "id": "yourdomain.com",
        "display_name": "Your App Name",
        "origins": ["https://auth.yourdomain.com"]
      }
    }
  }
}
```

## Useful Admin API Endpoints

### List All Tenants
```bash
curl http://localhost:8001/tenants
```

### Get Tenant Details
```bash
curl http://localhost:8001/tenants/<TENANT_ID>
```

### Update Tenant Config
```bash
curl -X PUT http://localhost:8001/tenants/<TENANT_ID>/config \
  -H "Content-Type: application/json" \
  -d '{ "webauthn": { ... } }'
```

### List Users with Passkeys
```bash
curl http://localhost:8001/tenants/<TENANT_ID>/users
```

### List API Keys
```bash
curl http://localhost:8001/tenants/<TENANT_ID>/secrets/api
```

### Delete Tenant
```bash
curl -X DELETE http://localhost:8001/tenants/<TENANT_ID>
```

## Need Help?

- Hanko Passkeys Documentation: https://github.com/teamhanko/passkeys
- Hanko Passkeys Admin API Spec: https://github.com/teamhanko/passkeys/blob/main/spec/passkey-server-admin.yaml
- Hanko Passkeys Public API Spec: https://github.com/teamhanko/passkeys/blob/main/spec/passkey-server.yaml
- WebAuthn Guide: https://webauthn.guide/

## What's Next?

- Customize the login/registration UI
- Add user management to view/revoke passkeys
- Implement passkey as 2FA alongside passwords
- Add support for multiple passkeys per user
- Integrate with your user database
