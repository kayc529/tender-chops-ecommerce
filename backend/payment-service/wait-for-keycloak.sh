#!/usr/bin/env sh
set -e

KEYCLOAK_URL="http://keycloak:8080/realms/tender-chops-test/.well-known/openid-configuration"

echo "Waiting for Keycloak at ${KEYCLOAK_URL}"

# Loop until Keycloak responds successfully
until wget -qO- "${KEYCLOAK_URL}" > /dev/null 2>&1; do
  echo "Keycloak not ready yet, retrying in 5s..."
  sleep 5
done

echo "Keycloak is up, starting payment-service..."

exec java -jar app.jar