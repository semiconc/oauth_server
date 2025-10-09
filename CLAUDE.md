# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.3.4 OAuth2 implementation with two separate Maven applications:
- **auth-server** (port 9000): OAuth2 Authorization Server using Spring Security OAuth2 Authorization Server
- **client-app** (port 9001): OAuth2 Client Application using Spring Security OAuth2 Client with Thymeleaf

Both applications use Java 17 and SpringDoc OpenAPI for API documentation.

## Building and Running

### Build Applications
```bash
# Build auth-server
cd auth-server && mvn clean install

# Build client-app
cd client-app && mvn clean install
```

### Run Applications
Both applications must be running simultaneously for OAuth2 flow to work:

```bash
# Terminal 1: Start auth-server (must start first)
cd auth-server && mvn spring-boot:run

# Terminal 2: Start client-app
cd client-app && mvn spring-boot:run
```

### Run Tests
```bash
# Run tests for auth-server
cd auth-server && mvn test

# Run tests for client-app
cd client-app && mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName
```

## Architecture

### Auth Server (Port 9000)

**Security Configuration** ([auth-server/src/main/java/com/example/authserver/config/SecurityConfig.java](auth-server/src/main/java/com/example/authserver/config/SecurityConfig.java)):
- Uses **two separate SecurityFilterChains** with `@Order` annotations to avoid conflicts:
  1. `@Order(1)`: OAuth2 Authorization Server endpoints with JWT resource server
  2. `@Order(2)`: Default security for form login with custom failure handler
- This dual-chain pattern was critical to resolve 500 errors on login failure (see git history)

**Key Components**:
- `LoggingAuthenticationFailureHandler`: Custom failure handler that logs authentication failures before delegating to Spring's default handler
- In-memory user store with BCrypt password encoding (default user: "user"/"password")
- In-memory registered client repository with client-id "client-app" and client-secret "secret" (using `{noop}` prefix)
- RSA key pair generation for JWT signing
- Authorization code + refresh token grant types
- OIDC and custom "read" scope with required authorization consent

**OAuth2 Endpoints**:
- Issuer: `http://localhost:9000`
- Authorization endpoint: `/oauth2/authorize`
- Token endpoint: `/oauth2/token`
- JWK Set endpoint: `/.well-known/jwks.json`
- OIDC Configuration: `/.well-known/openid-configuration`

### Client App (Port 9001)

**Security Configuration** ([client-app/src/main/java/com/example/clientapp/config/SecurityConfig.java](client-app/src/main/java/com/example/clientapp/config/SecurityConfig.java)):
- Single SecurityFilterChain with OAuth2 login
- Public access to Swagger UI endpoints
- Uses `issuer-uri` in application.yml for automatic OAuth2 provider configuration

**OAuth2 Client Configuration** ([client-app/src/main/resources/application.yml](client-app/src/main/resources/application.yml)):
- Registration ID: "client-app"
- Provider: Custom "spring" provider using auth-server's issuer-uri
- Redirect URI: `http://localhost:9001/login/oauth2/code/client-app`
- Scopes: openid, read

**Thymeleaf Templates** ([client-app/src/main/resources/templates/](client-app/src/main/resources/templates/)):
- `index.html`: Landing page
- `authorized.html`: Post-authentication page displaying OAuth2User attributes

**Controllers**:
- `WebController`: Serves Thymeleaf templates; `/authorized` endpoint receives OAuth2User principal

## Critical Implementation Details

### SecurityFilterChain Ordering
The auth-server uses multiple SecurityFilterChains with explicit `@Order` annotations. This pattern is essential:
- Without proper ordering, form login and OAuth2 authorization endpoints conflict
- The OAuth2 server chain (`@Order(1)`) must be processed before the default security chain (`@Order(2)`)
- Both chains share the same `PasswordEncoder` bean to ensure consistent password validation

### Password Encoding
- User passwords are encoded with BCrypt (`BCryptPasswordEncoder`)
- Client secrets use `{noop}` prefix for plain-text storage (acceptable for development)
- The same `PasswordEncoder` bean must be injected into `UserDetailsService` to avoid 500 errors on authentication

### Authentication Failure Handling
- `LoggingAuthenticationFailureHandler` provides visibility into login failures
- Delegates to `SimpleUrlAuthenticationFailureHandler` with redirect to `/login?error`
- Must be configured in the default SecurityFilterChain's `formLogin` customizer

## API Documentation

Both applications include SpringDoc OpenAPI (Swagger UI):
- Auth Server: `http://localhost:9000/swagger-ui.html`
- Client App: `http://localhost:9001/swagger-ui.html`

## Testing the OAuth2 Flow

1. Start auth-server on port 9000
2. Start client-app on port 9001
3. Navigate to `http://localhost:9001`
4. Click login → redirected to auth-server login
5. Enter credentials (user/password)
6. Approve consent for requested scopes
7. Redirected back to client-app at `/authorized` with user attributes displayed
