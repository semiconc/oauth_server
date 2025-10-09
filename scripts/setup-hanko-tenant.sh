#!/bin/bash

# Setup script for Hanko Passkeys tenant and API key
# This script creates a tenant and generates an API key for the auth-server

set -e

HANKO_ADMIN_URL="http://localhost:8001"
HANKO_PUBLIC_URL="http://localhost:8000"

echo "=========================================="
echo "Hanko Passkeys Tenant Setup"
echo "=========================================="
echo ""

# Check if admin API is accessible
echo "Checking Hanko Admin API availability..."
if ! curl -s -f "${HANKO_ADMIN_URL}/health/alive" > /dev/null 2>&1; then
    echo "❌ Error: Hanko Admin API is not accessible at ${HANKO_ADMIN_URL}"
    echo "Please ensure the Hanko Passkeys backend is running."
    echo "Admin API should be on port 8001"
    exit 1
fi
echo "✅ Hanko Admin API is accessible"
echo ""

# Create a tenant
echo "Creating new tenant..."
TENANT_RESPONSE=$(curl -s -X POST "${HANKO_ADMIN_URL}/tenants" \
    -H "Content-Type: application/json" \
    -d '{
        "display_name": "OAuth Server",
        "create_api_key": true,
        "config": {
            "cors": {
                "allowed_origins": ["http://localhost:9000"],
                "allow_unsafe_wildcard": false
            },
            "webauthn": {
                "timeout": 60000,
                "user_verification": "preferred",
                "relying_party": {
                    "id": "localhost",
                    "display_name": "OAuth2 Authorization Server",
                    "origins": ["http://localhost:9000"]
                }
            }
        }
    }')

if [ $? -ne 0 ]; then
    echo "❌ Failed to create tenant"
    exit 1
fi

TENANT_ID=$(echo "$TENANT_RESPONSE" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$TENANT_ID" ]; then
    echo "❌ Failed to extract tenant ID from response"
    echo "Response: $TENANT_RESPONSE"
    exit 1
fi

echo "✅ Tenant created successfully"
echo "   Tenant ID: $TENANT_ID"
echo ""

# Get API key (automatically created with create_api_key: true)
echo "Retrieving API key for tenant..."
API_KEY_RESPONSE=$(curl -s -X GET "${HANKO_ADMIN_URL}/tenants/${TENANT_ID}/secrets/api")

if [ $? -ne 0 ]; then
    echo "❌ Failed to retrieve API key"
    exit 1
fi

# Extract the secret from the first API key in the array
API_KEY=$(echo "$API_KEY_RESPONSE" | grep -o '"secret":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$API_KEY" ]; then
    echo "❌ Failed to extract API key from response"
    echo "Response: $API_KEY_RESPONSE"
    exit 1
fi

echo "✅ API key retrieved successfully"
echo ""

# Display configuration
echo "=========================================="
echo "Setup Complete! 🎉"
echo "=========================================="
echo ""
echo "Use the following configuration for your auth-server:"
echo ""
echo "Environment Variables:"
echo "----------------------"
echo "export HANKO_TENANT_ID=\"${TENANT_ID}\""
echo "export HANKO_API_KEY=\"${API_KEY}\""
echo ""
echo "Or add to application.yml:"
echo "-------------------------"
echo "hanko:"
echo "  passkey:"
echo "    api-url: ${HANKO_PUBLIC_URL}"
echo "    tenant-id: ${TENANT_ID}"
echo "    api-key: ${API_KEY}"
echo ""
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Set the environment variables above"
echo "2. Start the auth-server:"
echo "   cd auth-server && mvn spring-boot:run"
echo "3. Access http://localhost:9000/login"
echo "4. Login with user/password"
echo "5. Register a passkey at http://localhost:9000/register-passkey"
echo ""
