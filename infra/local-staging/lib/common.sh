#!/usr/bin/env bash
set -euo pipefail

LOCAL_STAGING_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
COMPOSE_FILE="$LOCAL_STAGING_DIR/docker-compose.yml"
ENV_FILE="$LOCAL_STAGING_DIR/.env"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

require_env_file() {
  [[ -f "$ENV_FILE" ]] || fail "Missing $ENV_FILE. Copy .env.example to .env first."
  if rg -qi 'appapi\.squarefootstory\.com|13\.235\.101\.204|SPRING_PROFILES_ACTIVE=(prod|production)(,|$)' "$ENV_FILE"; then
    fail "Production target/profile detected in local-staging .env"
  fi
  local active_profile
  active_profile=$(awk -F= '$1 == "SPRING_PROFILES_ACTIVE" {print $2}' "$ENV_FILE" | tr -d '[:space:]')
  [[ "$active_profile" == "local-staging" ]] || fail "SPRING_PROFILES_ACTIVE must equal local-staging"
}

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

validate_compose() {
  require_env_file
  compose config --quiet
}
