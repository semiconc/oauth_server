# Setup script for Hanko Passkeys tenant and API key (PowerShell version)
# This script creates a tenant and generates an API key for the auth-server

$ErrorActionPreference = "Stop"

$HANKO_ADMIN_URL = "http://localhost:8001"
$HANKO_PUBLIC_URL = "http://localhost:8000"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Hanko Passkeys Tenant Setup" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Check if admin API is accessible
Write-Host "Checking Hanko Admin API availability..."
try {
    $healthCheck = Invoke-WebRequest -Uri "$HANKO_ADMIN_URL/health/alive" -UseBasicParsing -TimeoutSec 5
    Write-Host "✅ Hanko Admin API is accessible" -ForegroundColor Green
} catch {
    Write-Host "❌ Error: Hanko Admin API is not accessible at $HANKO_ADMIN_URL" -ForegroundColor Red
    Write-Host "Please ensure the Hanko Passkeys backend is running." -ForegroundColor Red
    Write-Host "Admin API should be on port 8001" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Create a tenant
Write-Host "Creating new tenant..."
$tenantBody = @{
    display_name = "OAuth Server"
    create_api_key = $true
    config = @{
        cors = @{
            allowed_origins = @("http://localhost:9000")
            allow_unsafe_wildcard = $false
        }
        webauthn = @{
            timeout = 60000
            user_verification = "preferred"
            relying_party = @{
                id = "localhost"
                display_name = "OAuth2 Authorization Server"
                origins = @("http://localhost:9000")
            }
        }
    }
} | ConvertTo-Json -Depth 10

try {
    $tenantResponse = Invoke-RestMethod -Uri "$HANKO_ADMIN_URL/tenants" -Method Post -Body $tenantBody -ContentType "application/json"
    $TENANT_ID = $tenantResponse.id
    Write-Host "✅ Tenant created successfully" -ForegroundColor Green
    Write-Host "   Tenant ID: $TENANT_ID"
} catch {
    Write-Host "❌ Failed to create tenant" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
Write-Host ""

# Get API key (automatically created with create_api_key: true)
Write-Host "Retrieving API key for tenant..."
try {
    $apiKeyResponse = Invoke-RestMethod -Uri "$HANKO_ADMIN_URL/tenants/$TENANT_ID/secrets/api" -Method Get
    $API_KEY = $apiKeyResponse[0].secret
    Write-Host "✅ API key retrieved successfully" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to retrieve API key" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
Write-Host ""

# Display configuration
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Setup Complete! 🎉" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Use the following configuration for your auth-server:" -ForegroundColor Yellow
Write-Host ""
Write-Host "Environment Variables (PowerShell):" -ForegroundColor White
Write-Host "-----------------------------------" -ForegroundColor White
Write-Host "`$env:HANKO_TENANT_ID = `"$TENANT_ID`""
Write-Host "`$env:HANKO_API_KEY = `"$API_KEY`""
Write-Host ""
Write-Host "Or add to application.yml:" -ForegroundColor White
Write-Host "-------------------------" -ForegroundColor White
Write-Host "hanko:"
Write-Host "  passkey:"
Write-Host "    api-url: $HANKO_PUBLIC_URL"
Write-Host "    tenant-id: $TENANT_ID"
Write-Host "    api-key: $API_KEY"
Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Set the environment variables above"
Write-Host "2. Start the auth-server:"
Write-Host "   cd auth-server"
Write-Host "   mvn spring-boot:run"
Write-Host "3. Access http://localhost:9000/login"
Write-Host "4. Login with user/password"
Write-Host "5. Register a passkey at http://localhost:9000/register-passkey"
Write-Host ""
