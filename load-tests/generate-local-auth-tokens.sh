#!/usr/bin/env bash
#
# Generates real JWT access/refresh tokens for the 20 local fixed-OTP test
# users (+919900000001..+919900000020) against a locally running backend.
# Local-only: relies on sfs.local-staging.fake-otp being enabled, which is
# hard-gated to the local/local-fake-otp/local-staging Spring profiles.

set -euo pipefail

base_url="${BASE_URL:-http://localhost:8084}"
token_file="${TOKEN_FILE:-.k6-local-tokens}"
fixed_otp="${OTP_CODE:-123456}"
max_retries="${MAX_RETRIES:-10}"
inter_op_delay_seconds="${INTER_OP_DELAY_SECONDS:-0.3}"
inter_user_delay_seconds="${INTER_USER_DELAY_SECONDS:-1}"
health_url="${HEALTH_URL:-${base_url}/actuator/health}"

test_numbers=(
  "+919900000001"
  "+919900000002"
  "+919900000003"
  "+919900000004"
  "+919900000005"
  "+919900000006"
  "+919900000007"
  "+919900000008"
  "+919900000009"
  "+919900000010"
  "+919900000011"
  "+919900000012"
  "+919900000013"
  "+919900000014"
  "+919900000015"
  "+919900000016"
  "+919900000017"
  "+919900000018"
  "+919900000019"
  "+919900000020"
)

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    printf 'Required command not found on PATH: %s\n' "$command_name" >&2
    exit 1
  fi
}

require_command curl
require_command jq

verify_backend_reachable() {
  local status
  status="$(
    curl --silent --show-error --max-time 5 \
      --output /dev/null --write-out '%{http_code}' \
      "$health_url" || true
  )"

  if [[ "$status" != "200" ]]; then
    printf \
      'Backend not reachable at %s (HTTP %s). Start it before generating tokens.\n' \
      "$health_url" \
      "${status:-no response}" \
      >&2
    exit 1
  fi

  printf 'Backend reachable at %s (HTTP 200)\n' "$health_url"
}

# Strips any field that could carry a token/secret before a response body is
# ever written to the terminal, so a failed request can be diagnosed without
# ever printing an accessToken/refreshToken value.
redact_response_body() {
  jq -c '
    def redact:
      if type == "object" then
        with_entries(
          if (.key | test("token|secret|password"; "i")) then
            .value = "***REDACTED***"
          else
            .value |= redact
          end
        )
      elif type == "array" then
        map(redact)
      else
        .
      end;
    redact
  ' 2>/dev/null || printf '<unparsable response body omitted>'
}

post_with_rate_limit_retry() {
  local url="$1"
  local payload="$2"
  local response_file
  local status
  local retry_after
  local attempt=1

  response_file="$(mktemp)"

  while (( attempt <= max_retries )); do
    status="$(
      curl --silent --show-error \
        --output "$response_file" \
        --write-out '%{http_code}' \
        --request POST \
        --header 'Content-Type: application/json' \
        --data "$payload" \
        "$url"
    )"

    if [[ "$status" =~ ^2[0-9][0-9]$ ]]; then
      cat "$response_file"
      rm -f "$response_file"
      return 0
    fi

    if [[ "$status" == "429" ]]; then
      retry_after="$(
        jq -r '
          .retryAfterSeconds
          | select(type == "number" and . > 0)
        ' "$response_file" 2>/dev/null || true
      )"

      retry_after="${retry_after:-60}"

      printf \
        'Rate limited by %s. Waiting %d seconds before retry %d/%d...\n' \
        "$url" \
        "$((retry_after + 1))" \
        "$attempt" \
        "$max_retries" \
        >&2

      sleep "$((retry_after + 1))"
      attempt=$((attempt + 1))
      continue
    fi

    printf 'Request failed: HTTP %s %s\n' "$status" "$url" >&2
    redact_response_body <"$response_file" >&2
    printf '\n' >&2
    rm -f "$response_file"
    return 1
  done

  printf 'Exceeded retry limit for %s\n' "$url" >&2
  rm -f "$response_file"
  return 1
}

verify_backend_reachable

tokens=()

for index in "${!test_numbers[@]}"; do
  phone_number="${test_numbers[$index]}"
  user_number=$((index + 1))
  device_id="k6-user-$(printf '%02d' "$user_number")"

  printf '\nGenerating token %d/%d for user index %d\n' \
    "$user_number" \
    "${#test_numbers[@]}" \
    "$user_number"

  request_payload="$(
    jq -cn \
      --arg phoneNumber "$phone_number" \
      '{phoneNumber: $phoneNumber}'
  )"

  request_otp_response="$(
    post_with_rate_limit_retry \
      "$base_url/api/auth/request-otp" \
      "$request_payload"
  )"

  if ! jq -e '.status == "OTP_SENT"' <<<"$request_otp_response" >/dev/null 2>&1; then
    printf 'request-otp did not return status=OTP_SENT for user %d\n' "$user_number" >&2
    redact_response_body <<<"$request_otp_response" >&2
    printf '\n' >&2
    exit 1
  fi

  sleep "$inter_op_delay_seconds"

  verify_payload="$(
    jq -cn \
      --arg phoneNumber "$phone_number" \
      --arg code "$fixed_otp" \
      --arg deviceId "$device_id" \
      '{
        phoneNumber: $phoneNumber,
        code: $code,
        deviceId: $deviceId,
        fcmToken: null
      }'
  )"

  verify_response="$(
    post_with_rate_limit_retry \
      "$base_url/api/auth/verify-otp" \
      "$verify_payload"
  )"

  if ! jq -e '
    (.accessToken | type == "string" and length > 0) and
    (.refreshToken | type == "string" and length > 0) and
    (.user.userId != null) and
    (.user.phoneNumber | type == "string" and length > 0) and
    (.user.role | type == "string" and length > 0)
  ' <<<"$verify_response" >/dev/null 2>&1; then
    printf 'verify-otp response missing required fields for user %d\n' "$user_number" >&2
    redact_response_body <<<"$verify_response" >&2
    printf '\n' >&2
    exit 1
  fi

  access_token="$(
    jq -er '
      .accessToken
      | select(type == "string" and length > 0)
    ' <<<"$verify_response"
  )"

  tokens+=("$access_token")

  printf 'Captured token %d; length=%d\n' \
    "$user_number" \
    "${#access_token}"

  # Small delay between users to avoid unnecessary OTP rate-limit bursts.
  sleep "$inter_user_delay_seconds"
done

expected_count="${#test_numbers[@]}"

if [[ "${#tokens[@]}" -ne "$expected_count" ]]; then
  printf \
    'Expected %d tokens but captured %d\n' \
    "$expected_count" \
    "${#tokens[@]}" \
    >&2
  exit 1
fi

unique_count="$(printf '%s\n' "${tokens[@]}" | sort -u | wc -l | tr -d ' ')"
if [[ "$unique_count" -ne "$expected_count" ]]; then
  printf \
    'Expected %d unique access tokens but only %d were unique\n' \
    "$expected_count" \
    "$unique_count" \
    >&2
  exit 1
fi

temporary_file="${token_file}.tmp"
cleanup_temp_file() {
  rm -f "$temporary_file"
}
trap cleanup_temp_file EXIT

(
  IFS=,
  printf '%s' "${tokens[*]}"
) >"$temporary_file"

chmod 600 "$temporary_file"
mv "$temporary_file" "$token_file"
trap - EXIT

printf '\nToken count: %d\n' "${#tokens[@]}"
printf 'Written securely to: %s\n' "$token_file"
