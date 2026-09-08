#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=lib/common.sh
source "$SCRIPT_DIR/lib/common.sh"

validate_compose
mkdir -p "$SCRIPT_DIR/runtime/logs" "$SCRIPT_DIR/reports"
compose up -d --build postgres backend nginx
compose ps
