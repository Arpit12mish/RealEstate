#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=lib/safe-target.sh
source "$SCRIPT_DIR/lib/safe-target.sh"
require_safe_base_url

interval=${INTERVAL_SECONDS:-5}
[[ "$interval" =~ ^[1-9][0-9]*$ ]] || { echo "ERROR: INTERVAL_SECONDS must be positive" >&2; exit 1; }

metrics=(
  hikaricp.connections.active
  hikaricp.connections.pending
  hikaricp.connections.timeout
  tomcat.threads.busy
  jvm.memory.used
  process.cpu.usage
  system.cpu.usage
  sfs.rate_limit.cache.size
  sfs.logging.queue.size
  sfs.logging.events.discarded
)

while true; do
  timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  for metric in "${metrics[@]}"; do
    value=$(actuator_curl "$BASE_URL/actuator/metrics/$metric" 2>/dev/null || printf '{"unavailable":true}')
    printf '%s\t%s\t%s\n' "$timestamp" "$metric" "$value"
  done
  sleep "$interval"
done
