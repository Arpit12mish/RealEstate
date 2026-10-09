#!/usr/bin/env bash
#
# Verifies each token in the local k6 token file independently against
# GET /actuator/health and GET /api/projects/{id}/meter. Never prints a
# token value - only tokenIndex, HTTP status, resolved userId (decoded
# locally from the JWT payload, not printed as the raw token), and length.

set -euo pipefail

base_url="${BASE_URL:-http://localhost:8084}"
token_file="${TOKEN_FILE:-.k6-local-tokens}"
project_id="${PROJECT_ID:-27}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Required command not found on PATH: %s\n' "$1" >&2
    exit 1
  fi
}

require_command curl
require_command jq

if [[ ! -f "$token_file" ]]; then
  printf 'Token file not found: %s\n' "$token_file" >&2
  exit 1
fi

decode_user_id_from_jwt() {
  local token="$1"
  local payload_segment
  payload_segment="$(cut -d '.' -f2 <<<"$token")"

  local remainder=$(( ${#payload_segment} % 4 ))
  if (( remainder != 0 )); then
    payload_segment="${payload_segment}$(printf '=%.0s' $(seq 1 $((4 - remainder))))"
  fi

  tr '_-' '/+' <<<"$payload_segment" \
    | base64 --decode 2>/dev/null \
    | jq -r '.userId // "unknown"' 2>/dev/null || echo "unknown"
}

IFS=',' read -r -a tokens <<<"$(cat "$token_file")"

expected_count=20
actual_count="${#tokens[@]}"

printf 'Verifying %d tokens against %s (project id %s)\n\n' \
  "$actual_count" "$base_url" "$project_id"

printf '%-10s %-8s %-24s %-8s %-10s\n' "tokenIdx" "length" "resolvedUserId" "health" "meter"

pass_count=0
seen_user_ids=()

for index in "${!tokens[@]}"; do
  token_index=$((index + 1))
  token="${tokens[$index]}"
  token_length="${#token}"

  user_id="$(decode_user_id_from_jwt "$token")"
  seen_user_ids+=("$user_id")

  health_status="$(
    curl -s -o /dev/null -w '%{http_code}' --max-time 10 \
      -H "Authorization: Bearer ${token}" \
      "${base_url}/actuator/health"
  )"

  meter_status="$(
    curl -s -o /dev/null -w '%{http_code}' --max-time 10 \
      -H "Authorization: Bearer ${token}" \
      "${base_url}/api/projects/${project_id}/meter"
  )"

  printf '%-10s %-8s %-24s %-8s %-10s\n' \
    "$token_index" "$token_length" "$user_id" "$health_status" "$meter_status"

  if [[ "$health_status" == "200" && "$meter_status" == "200" && "$token_length" -gt 0 ]]; then
    pass_count=$((pass_count + 1))
  fi
done

echo
printf 'Total tokens: %d\n' "$actual_count"
printf 'Passed (health=200, meter=200, non-blank): %d\n' "$pass_count"

unique_user_ids="$(printf '%s\n' "${seen_user_ids[@]}" | sort -u | wc -l | tr -d ' ')"
printf 'Distinct resolved userIds: %d\n' "$unique_user_ids"

unique_tokens="$(printf '%s\n' "${tokens[@]}" | sort -u | wc -l | tr -d ' ')"
printf 'Distinct token values: %d\n' "$unique_tokens"

if [[ "$actual_count" -ne "$expected_count" ]]; then
  printf 'FAILED: expected %d tokens, found %d\n' "$expected_count" "$actual_count" >&2
  exit 1
fi

if [[ "$pass_count" -ne "$expected_count" ]]; then
  printf 'FAILED: only %d/%d tokens passed verification\n' "$pass_count" "$expected_count" >&2
  exit 1
fi

if [[ "$unique_user_ids" -ne "$expected_count" ]]; then
  printf 'FAILED: expected %d distinct userIds, found %d\n' "$expected_count" "$unique_user_ids" >&2
  exit 1
fi

if [[ "$unique_tokens" -ne "$expected_count" ]]; then
  printf 'FAILED: expected %d distinct token values, found %d\n' "$expected_count" "$unique_tokens" >&2
  exit 1
fi

printf '\nAll %d tokens verified successfully.\n' "$expected_count"
