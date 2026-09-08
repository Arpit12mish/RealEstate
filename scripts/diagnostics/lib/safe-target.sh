#!/usr/bin/env bash
set -euo pipefail

require_safe_base_url() {
  local base_url=${BASE_URL:-}
  [[ -n "$base_url" ]] || { echo "ERROR: BASE_URL is required" >&2; exit 1; }
  case "$base_url" in
    *appapi.squarefootstory.com*|*13.235.101.204*)
      echo "ERROR: production target is forbidden" >&2
      exit 1
      ;;
  esac
  [[ "$base_url" =~ ^https?:// ]] || {
    echo "ERROR: BASE_URL must be an absolute http(s) URL" >&2
    exit 1
  }
}

actuator_curl() {
  if [[ -n "${ACTUATOR_TOKEN:-}" ]]; then
    curl --fail --silent --show-error -H "Authorization: Bearer ${ACTUATOR_TOKEN}" "$@"
  else
    curl --fail --silent --show-error "$@"
  fi
}
