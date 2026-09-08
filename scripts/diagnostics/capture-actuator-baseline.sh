#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=lib/safe-target.sh
source "$SCRIPT_DIR/lib/safe-target.sh"
require_safe_base_url

OUTPUT_DIR=${OUTPUT_DIR:-$SCRIPT_DIR/output/$(date -u +%Y%m%dT%H%M%SZ)}
mkdir -p "$OUTPUT_DIR"

metrics=(
  hikaricp.connections.active
  hikaricp.connections.idle
  hikaricp.connections.pending
  hikaricp.connections.max
  hikaricp.connections.timeout
  tomcat.threads.current
  tomcat.threads.busy
  tomcat.threads.config.max
  jvm.memory.used
  jvm.gc.pause
  process.cpu.usage
  system.cpu.usage
  sfs.rate_limit.cache.size
  sfs.rate_limit.cache.hits
  sfs.rate_limit.cache.misses
  sfs.rate_limit.cache.evictions
  sfs.authentication.identity.cache.size
  sfs.authentication.identity.cache.operations
  sfs.logging.queue.size
  sfs.logging.queue.capacity
  sfs.logging.queue.peak
  sfs.logging.events.discarded
  sfs.logging.appender.failures
)

actuator_curl "$BASE_URL/actuator/health/readiness" > "$OUTPUT_DIR/readiness.json"
for metric in "${metrics[@]}"; do
  if ! actuator_curl "$BASE_URL/actuator/metrics/$metric" > "$OUTPUT_DIR/$metric.json"; then
    printf '{"unavailable":true,"metric":"%s"}\n' "$metric" > "$OUTPUT_DIR/$metric.json"
    echo "WARN: metric unavailable: $metric" >&2
  fi
done

echo "Actuator baseline written to $OUTPUT_DIR"
