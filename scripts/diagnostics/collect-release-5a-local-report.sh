#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_DIR=$(cd "$SCRIPT_DIR/../.." && pwd)
# shellcheck source=lib/safe-target.sh
source "$SCRIPT_DIR/lib/safe-target.sh"
require_safe_base_url

timestamp=$(date -u +%Y%m%dT%H%M%SZ)
OUTPUT_DIR=${OUTPUT_DIR:-$SCRIPT_DIR/output/release-5a-local-$timestamp}
export OUTPUT_DIR
mkdir -p "$OUTPUT_DIR"

"$SCRIPT_DIR/capture-actuator-baseline.sh"
git -C "$REPO_DIR" rev-parse HEAD > "$OUTPUT_DIR/git-commit.txt"
docker version > "$OUTPUT_DIR/docker-version.txt"
docker compose --env-file "$REPO_DIR/infra/local-staging/.env" \
  -f "$REPO_DIR/infra/local-staging/docker-compose.yml" ps > "$OUTPUT_DIR/compose-ps.txt"
docker compose --env-file "$REPO_DIR/infra/local-staging/.env" \
  -f "$REPO_DIR/infra/local-staging/docker-compose.yml" exec -T postgres \
  sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f -' \
  < "$SCRIPT_DIR/postgresql-active-sessions.sql" > "$OUTPUT_DIR/postgresql-active-sessions.txt"

echo "Release 5A-P local report written to $OUTPUT_DIR"
