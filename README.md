# OAuth2 Authorization Server with Passkey Authentication

A Spring Boot OAuth2 Authorization Server with **passkey authentication** integration using Hanko Passkeys backend.

## Features

- ✅ OAuth2 Authorization Server with authorization code + refresh token flow
- ✅ **Passkey Authentication** (WebAuthn) as an alternative to passwords
- ✅ Traditional password-based authentication
- ✅ OIDC support
- ✅ JWT token signing
- ✅ OAuth2 Client application for testing
- ✅ Modern UI with Thymeleaf templates

## Quick Start

### 1. Prerequisites

- Java 17+
- Maven 3.6+
- Hanko Passkeys backend running (ports 8000 and 8001)

### 2. Start Hanko Passkeys Backend

See [HANKO_QUICK_START.md](HANKO_QUICK_START.md) for detailed instructions.

Quick version using Docker:

```bash
docker run -d --name hanko-passkeys -p 8000:8000 -p 8001:8001 ghcr.io/teamhanko/passkeys-server:latest
```

### 3. Set Up Hanko Tenant

Run the automated setup script:

```bash
./scripts/setup-hanko-tenant.sh
```

This will:
- Create a new tenant in Hanko
- Generate an API key
- Display configuration values

Copy the environment variables from the output:

```bash
export HANKO_TENANT_ID="your-tenant-id"
export HANKO_API_KEY="your-api-key"
```

### 4. Start Auth Server

```bash
cd auth-server
mvn spring-boot:run
```

The auth-server will start on **http://localhost:9000**

### 5. Start Client App (Optional)

```bash
cd client-app
mvn spring-boot:run
```

The client-app will start on **http://localhost:9001**

## Using Passkey Authentication

### Register a Passkey

1. Login with password first:
   - Go to http://localhost:9000/login
   - Username: `user`, Password: `password`

2. Register your passkey:
   - Click "Register a Passkey" on the home page
   - Or visit http://localhost:9000/register-passkey
   - Follow your browser's prompts (Touch ID, Windows Hello, etc.)

### Login with Passkey

1. Go to http://localhost:9000/login
2. Enter your username in the "Username for Passkey" field
3. Click "Sign in with Passkey"
4. Authenticate using your registered passkey

## Project Structure

```
oauth_server/
├── auth-server/              # OAuth2 Authorization Server
│   ├── src/main/java/
│   │   └── com/example/authserver/
│   │       ├── config/
│   │       │   ├── SecurityConfig.java          # Security configuration
│   │       │   └── HankoPasskeyProperties.java  # Hanko config
│   │       ├── controller/
│   │       │   ├── PasskeyController.java       # Passkey REST API
│   │       │   └── LoginController.java         # Login pages
│   │       ├── service/
│   │       │   └── HankoPasskeyService.java     # Hanko API client
│   │       └── dto/                             # Passkey DTOs
│   └── src/main/resources/
│       ├── templates/                           # Thymeleaf templates
│       │   ├── login.html                       # Login with passkey option
│       │   ├── register-passkey.html            # Passkey registration
│       │   └── home.html                        # Home page
│       ├── static/js/                           # WebAuthn JavaScript
│       │   ├── passkey-auth.js
│       │   └── passkey-registration.js
│       └── application.yml
│
├── client-app/               # OAuth2 Client Application
│   └── [OAuth2 client implementation]
│
├── scripts/                  # Setup scripts
│   ├── setup-hanko-tenant.sh      # Linux/Mac setup
│   └── setup-hanko-tenant.ps1     # Windows setup
│
├── HANKO_QUICK_START.md      # Quick start guide
├── PASSKEY_SETUP.md          # Detailed passkey setup
└── README.md                 # This file
```

## API Endpoints

### Auth Server (Port 9000)

**OAuth2 Endpoints:**
- `GET /oauth2/authorize` - Authorization endpoint
- `POST /oauth2/token` - Token endpoint
- `GET /.well-known/openid-configuration` - OIDC discovery
- `GET /.well-known/jwks.json` - JWK Set

**Passkey Endpoints:**
- `POST /api/passkey/register/initialize` - Start passkey registration (authenticated)
- `POST /api/passkey/register/finalize` - Complete passkey registration
- `POST /api/passkey/login/initialize` - Start passkey login (public)
- `POST /api/passkey/login/finalize` - Complete passkey login

**Pages:**
- `GET /login` - Login page with passkey option
- `GET /register-passkey` - Passkey registration page
- `GET /` - Home page

### Client App (Port 9001)

- `GET /` - Landing page
- `GET /authorized` - Post-authentication page with user info

## Configuration

### Environment Variables

```bash
# Hanko Passkeys
export HANKO_TENANT_ID="your-tenant-id"
export HANKO_API_KEY="your-api-key"
```

### Application Properties

Edit `auth-server/src/main/resources/application.yml`:

```yaml
hanko:
  passkey:
    api-url: http://localhost:8000
    tenant-id: ${HANKO_TENANT_ID}
    api-key: ${HANKO_API_KEY}
```

## Testing OAuth2 Flow with Passkeys

1. Visit client app: http://localhost:9001
2. Click "Login with OAuth2"
3. You'll be redirected to auth-server login
4. **Use passkey** instead of password to authenticate
5. Approve OAuth2 consent
6. Redirected back to client-app with user info

## Default Credentials

**Password Login:**
- Username: `user`
- Password: `password`

**OAuth2 Client:**
- Client ID: `client-app`
- Client Secret: `secret`

## Browser Support

Passkeys (WebAuthn) require modern browsers:
- Chrome/Edge 67+
- Firefox 60+
- Safari 13+
- Mobile browsers (iOS Safari 14+, Chrome Android 70+)

## Security Notes

⚠️ **This configuration is for DEVELOPMENT only**

For production:
- Use HTTPS (WebAuthn requires HTTPS)
- Use a proper database instead of in-memory stores
- Secure API keys with secrets management
- Update CORS and allowed origins
- Change relying party ID from `localhost` to your domain

## Troubleshooting

### "Hanko Admin API is not accessible"
- Ensure Hanko backend is running: `curl http://localhost:8001/health/alive`

### "Passkey registration failed"
- Check browser compatibility
- Ensure device has biometric or security key
- View browser console for errors

### "Passkey login failed"
- Verify you've registered a passkey for that username
- Check that Hanko backend is running
- Verify HANKO_TENANT_ID and HANKO_API_KEY are set correctly

### Enable Debug Logging

Add to `application.yml`:

```yaml
logging:
  level:
    com.example.authserver: DEBUG
```

## Documentation

- **[HANKO_QUICK_START.md](HANKO_QUICK_START.md)** - Complete setup guide with troubleshooting
- **[PASSKEY_SETUP.md](PASSKEY_SETUP.md)** - Detailed passkey architecture and API docs
- **[CLAUDE.md](CLAUDE.md)** - Project overview and build instructions

## Architecture Highlights

### Dual Authentication System

The system supports both password and passkey authentication:

1. **Password Flow**: Traditional username/password with BCrypt hashing
2. **Passkey Flow**: WebAuthn-based biometric authentication via Hanko

### Security Configuration

Two separate SecurityFilterChain beans with explicit ordering:
- `@Order(1)`: OAuth2 authorization server endpoints
- `@Order(2)`: Default security for form login and passkey endpoints

### Passkey Registration Flow

1. User logs in with password
2. Initiates passkey registration (`POST /api/passkey/register/initialize`)
3. Browser's WebAuthn API creates credential
4. Credential sent to Hanko for storage (`POST /api/passkey/register/finalize`)

### Passkey Authentication Flow

1. User initiates passkey login (`POST /api/passkey/login/initialize`)
2. Browser's WebAuthn API signs challenge
3. Signature verified by Hanko (`POST /api/passkey/login/finalize`)
4. Spring Security session created

## Tech Stack

- **Spring Boot 3.3.4**
- **Spring Security OAuth2 Authorization Server**
- **Spring Security OAuth2 Client**
- **Thymeleaf** (templating)
- **WebAuthn** (browser API)
- **Hanko Passkeys** (backend)
- **WebFlux** (HTTP client)

## Port Summary

- **8000**: Hanko Public API (WebAuthn)
- **8001**: Hanko Admin API (management)
- **9000**: Auth-Server (OAuth2 + Passkeys)
- **9001**: Client-App (OAuth2 client)

## Contributing

Feel free to submit issues and pull requests!

## License

This project is for demonstration purposes.

## Links

- [Hanko Passkeys](https://github.com/teamhanko/passkeys)
- [WebAuthn Guide](https://webauthn.guide/)
- [Spring Authorization Server](https://spring.io/projects/spring-authorization-server)
