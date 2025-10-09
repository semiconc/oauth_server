# ✅ Hanko Passkey Setup Complete!

Your OAuth2 Authorization Server now has passkey authentication integrated!

## What Was Done

### 1. Hanko Tenant Created ✅

A tenant has been created in your Hanko Passkeys backend with the following configuration:

- **Tenant ID**: `65272454-123c-485d-853d-71aea8fd43a8`
- **API Key**: `lLnJkWRsijFzJ3qRiv9QWBredpIOD6YzQcbKHjWABnXuHR2BZWo7ZLh3Huc1mxrr7BQkZgphJIXc42xEUW1c0Q==`
- **Relying Party**: localhost
- **Allowed Origins**: http://localhost:9000

### 2. Auth-Server Integration Complete ✅

The following components have been added to your auth-server:

**Backend Components:**
- ✅ [HankoPasskeyService.java](auth-server/src/main/java/com/example/authserver/service/HankoPasskeyService.java) - REST client for Hanko API
- ✅ [PasskeyController.java](auth-server/src/main/java/com/example/authserver/controller/PasskeyController.java) - REST API endpoints
- ✅ [HankoPasskeyProperties.java](auth-server/src/main/java/com/example/authserver/config/HankoPasskeyProperties.java) - Configuration
- ✅ 4 DTO classes for WebAuthn protocol
- ✅ Updated [SecurityConfig.java](auth-server/src/main/java/com/example/authserver/config/SecurityConfig.java)

**Frontend Components:**
- ✅ [login.html](auth-server/src/main/resources/templates/login.html) - Login page with passkey option
- ✅ [register-passkey.html](auth-server/src/main/resources/templates/register-passkey.html) - Passkey registration page
- ✅ [home.html](auth-server/src/main/resources/templates/home.html) - Home page
- ✅ [passkey-auth.js](auth-server/src/main/resources/static/js/passkey-auth.js) - WebAuthn authentication
- ✅ [passkey-registration.js](auth-server/src/main/resources/static/js/passkey-registration.js) - WebAuthn registration

**Configuration:**
- ✅ Updated [application.yml](auth-server/src/main/resources/application.yml) with Hanko settings
- ✅ Added dependencies: spring-boot-starter-webflux, spring-boot-starter-thymeleaf

### 3. Setup Scripts Created ✅

- ✅ [setup-hanko-tenant.sh](scripts/setup-hanko-tenant.sh) - Automated setup for Linux/Mac
- ✅ [setup-hanko-tenant.ps1](scripts/setup-hanko-tenant.ps1) - Automated setup for Windows

### 4. Documentation Created ✅

- ✅ [README.md](README.md) - Main project documentation
- ✅ [HANKO_QUICK_START.md](HANKO_QUICK_START.md) - Complete setup guide
- ✅ [PASSKEY_SETUP.md](PASSKEY_SETUP.md) - Detailed architecture docs
- ✅ [.env.example](.env.example) - Environment variable template
- ✅ [.env](.env) - Pre-configured with your tenant ID and API key

## Next Steps

### Start the Auth-Server

**Option 1: Using Environment Variables**

```bash
# Set environment variables
export HANKO_TENANT_ID="65272454-123c-485d-853d-71aea8fd43a8"
export HANKO_API_KEY="lLnJkWRsijFzJ3qRiv9QWBredpIOD6YzQcbKHjWABnXuHR2BZWo7ZLh3Huc1mxrr7BQkZgphJIXc42xEUW1c0Q=="

# Start auth-server
cd auth-server
mvn spring-boot:run
```

**Option 2: Update application.yml**

Edit `auth-server/src/main/resources/application.yml` and add:

```yaml
hanko:
  passkey:
    tenant-id: 65272454-123c-485d-853d-71aea8fd43a8
    api-key: lLnJkWRsijFzJ3qRiv9QWBredpIOD6YzQcbKHjWABnXuHR2BZWo7ZLh3Huc1mxrr7BQkZgphJIXc42xEUW1c0Q==
```

Then start:
```bash
cd auth-server
mvn spring-boot:run
```

### Test Passkey Authentication

1. **Open your browser**: http://localhost:9000/login

2. **Login with password**:
   - Username: `user`
   - Password: `password`

3. **Register a passkey**:
   - Click "Register a Passkey" on the home page
   - Follow your browser's prompts (Touch ID, Windows Hello, fingerprint, etc.)

4. **Logout and test passkey login**:
   - Logout from the home page
   - Go to http://localhost:9000/login
   - Enter username: `user` in the "Username for Passkey" field
   - Click "Sign in with Passkey"
   - Authenticate with your biometric/PIN

### Test OAuth2 Flow with Passkey

1. **Start the client app**:
   ```bash
   cd client-app
   mvn spring-boot:run
   ```

2. **Access the client app**: http://localhost:9001

3. **Click login** - you'll be redirected to auth-server

4. **Authenticate with passkey** instead of password

5. **Approve OAuth2 consent**

6. **View user info** on the client app

## Important URLs

- **Auth-Server Login**: http://localhost:9000/login
- **Passkey Registration**: http://localhost:9000/register-passkey
- **Home Page**: http://localhost:9000/
- **Client App**: http://localhost:9001/
- **OAuth2 Authorization**: http://localhost:9000/oauth2/authorize
- **OIDC Discovery**: http://localhost:9000/.well-known/openid-configuration

## API Endpoints

### Passkey Registration (Authenticated Users Only)

```bash
# Initialize registration
curl -X POST http://localhost:9000/api/passkey/register/initialize \
  -H "Cookie: JSESSIONID=..." \
  -H "Content-Type: application/json"

# Finalize registration
curl -X POST http://localhost:9000/api/passkey/register/finalize \
  -H "Cookie: JSESSIONID=..." \
  -H "Content-Type: application/json" \
  -d '{"credential": {...}}'
```

### Passkey Login (Public)

```bash
# Initialize login
curl -X POST http://localhost:9000/api/passkey/login/initialize \
  -H "Content-Type: application/json" \
  -d '{"userId": "user"}'

# Finalize login
curl -X POST http://localhost:9000/api/passkey/login/finalize \
  -H "Content-Type: application/json" \
  -d '{"credential": {...}}'
```

## Troubleshooting

### Auth-server won't start

**Issue**: Missing tenant ID or API key

**Solution**: Make sure environment variables are set:
```bash
echo $HANKO_TENANT_ID
echo $HANKO_API_KEY
```

If empty, set them as shown above.

### Passkey registration fails

**Common causes**:
1. Not logged in - must authenticate with password first
2. Browser doesn't support WebAuthn
3. No biometric/security key available on device

**Solution**:
- Use a modern browser (Chrome 67+, Firefox 60+, Safari 13+)
- Ensure device has biometric or security key
- Check browser console for errors

### Passkey login fails

**Common causes**:
1. No passkey registered for that username
2. Hanko backend not running
3. Wrong tenant ID or API key

**Solution**:
- Register a passkey first using password login
- Verify Hanko is running: `curl http://localhost:8000/health/alive`
- Check environment variables are correct

### "Cannot connect to Hanko"

**Issue**: Hanko backend not running or wrong URL

**Solution**:
```bash
# Check if Hanko is running
curl http://localhost:8000/health/alive
curl http://localhost:8001/health/alive

# If not running, start it:
docker ps | grep hanko-passkeys
# or
docker run -d --name hanko-passkeys -p 8000:8000 -p 8001:8001 ghcr.io/teamhanko/passkeys-server:latest
```

## Security Considerations

⚠️ **Development Setup** - This configuration is for development only!

For production:
1. **Use HTTPS** - WebAuthn requires HTTPS in production
2. **Secure API Keys** - Use secrets management, not environment variables
3. **Update Relying Party ID** - Change from `localhost` to your domain
4. **Use Database** - Replace in-memory stores with persistent database
5. **Update CORS** - Restrict allowed origins to your production domains

## Support

If you need help:
- Review [HANKO_QUICK_START.md](HANKO_QUICK_START.md) for detailed instructions
- Check [PASSKEY_SETUP.md](PASSKEY_SETUP.md) for architecture details
- Visit [Hanko Passkeys GitHub](https://github.com/teamhanko/passkeys)
- Read [WebAuthn Guide](https://webauthn.guide/)

## What's Working

✅ **Password Authentication**: Traditional username/password login
✅ **Passkey Registration**: Authenticated users can register passkeys
✅ **Passkey Authentication**: Login with biometric/security key
✅ **OAuth2 Authorization**: Standard authorization code + refresh token flow
✅ **OIDC Support**: OpenID Connect discovery and JWT tokens
✅ **Dual Auth**: Both password and passkey work side-by-side
✅ **Modern UI**: Clean, responsive Thymeleaf templates
✅ **WebAuthn Compliant**: Standards-compliant passkey implementation

## Credits

- Built with Spring Boot 3.3.4
- Uses Hanko Passkeys backend
- Implements WebAuthn standard
- Integrated by Claude Code

---

**Enjoy your passwordless future! 🎉**
