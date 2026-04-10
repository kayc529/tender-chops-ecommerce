#!/usr/bin/env sh
set -e

# ===== Required env vars =====
if [ -z "${KEYCLOAK_HOST}" ]; then
  echo "ERROR: KEYCLOAK_HOST is not set"
  exit 1
fi

if [ -z "${KEYCLOAK_REALM}" ]; then
  echo "ERROR: KEYCLOAK_REALM is not set"
  exit 1
fi

KEYCLOAK_URL="http://${KEYCLOAK_HOST}/realms/${KEYCLOAK_REALM}/.well-known/openid-configuration"

# ===== Retry config =====
MAX_RETRIES=${MAX_RETRIES:-30}
INTERVAL=${RETRY_INTERVAL:-5}
ATTEMPT=1

# ===== Ensure curl exists =====
if ! command -v curl >/dev/null 2>&1; then
  echo "ERROR: curl not found. Install curl in the Docker image."
  exit 1
fi

log() {
  echo "$(date -Iseconds) $1"
}

log "INFO: Waiting for Keycloak at ${KEYCLOAK_URL} (max ${MAX_RETRIES} retries, interval ${INTERVAL}s)"

# ===== Retry loop =====
while [ "$ATTEMPT" -le "$MAX_RETRIES" ]; do
  if curl -fsS "${KEYCLOAK_URL}" > /dev/null 2>&1; then
    log "INFO: Keycloak is ready. Starting application..."
    exec java -jar app.jar
  fi

  log "WARN: Keycloak not ready (attempt ${ATTEMPT}/${MAX_RETRIES}). Retrying in ${INTERVAL}s..."
  ATTEMPT=$((ATTEMPT + 1))
  sleep "$INTERVAL"
done

log "ERROR: Keycloak did not become ready after ${MAX_RETRIES} attempts."
exit 1