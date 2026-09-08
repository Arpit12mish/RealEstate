#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"

validate_compose
project_name=$(awk -F= '$1 == "COMPOSE_PROJECT_NAME" {print $2}' "$ENV_FILE" | tr -d '[:space:]')
[[ "$project_name" =~ ^sfs-local-staging[-a-zA-Z0-9_]*$ ]] \
  || fail "COMPOSE_PROJECT_NAME must begin with sfs-local-staging"

read -r -p "Type RESET to delete only ${project_name}-postgres-data: " confirmation
[[ "$confirmation" == "RESET" ]] || fail "Reset cancelled"

compose down --remove-orphans
docker volume rm "${project_name}-postgres-data"
echo "Removed local volume ${project_name}-postgres-data"
