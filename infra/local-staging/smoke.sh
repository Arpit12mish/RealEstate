#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"

validate_compose
host_port=$(awk -F= '$1 == "NGINX_HOST_PORT" {print $2}' "$ENV_FILE" | tr -d '[:space:]')
base_url="http://127.0.0.1:${host_port:-8088}"

for attempt in $(seq 1 60); do
  if curl --fail --silent "$base_url/actuator/health/readiness" >/dev/null; then
    break
  fi
  [[ "$attempt" -lt 60 ]] || fail "Readiness did not become healthy"
  sleep 2
done

curl --fail --silent "$base_url/local-health" >/dev/null
curl --fail --silent "$base_url/actuator/health/liveness" >/dev/null
curl --fail --silent -H 'X-Request-Id: local-staging-smoke' "$base_url/api/public/cities/trending" >/dev/null

metrics_status=$(curl --silent --output /dev/null --write-out '%{http_code}' \
  "$base_url/actuator/metrics/hikaricp.connections.active")
[[ "$metrics_status" == "401" || "$metrics_status" == "403" ]] \
  || fail "Actuator metrics unexpectedly returned HTTP $metrics_status without authentication"

has_published_port() {
  local service="$1"
  local container_id
  local bindings

  container_id=$(compose ps -q "$service")
  [[ -n "$container_id" ]] || fail "Service $service is not running"
  bindings=$(docker inspect --format '{{json .HostConfig.PortBindings}}' "$container_id")
  [[ "$bindings" != "{}" && "$bindings" != "null" ]]
}

if has_published_port backend; then
  fail "Backend port is unexpectedly published"
fi
if has_published_port postgres; then
  fail "PostgreSQL port is unexpectedly published"
fi

echo "Local-staging smoke checks passed through NGINX at $base_url"
