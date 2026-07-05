# Rate Limit Production Smoke Test

Manual verification steps for confirming `com.brandPitara.sfs.ratelimit` is
behaving correctly against a real (staging or production) deployment, without
spamming real users with OTP SMS or exposing any secret in a command you might
paste into a shared terminal/ticket.

**Rules for every command below:**
- Replace `<PRODUCTION_HOST>` / `<STAGING_HOST>` with the real base URLs for
  those environments - not asserted here, since this doc doesn't know them.
- Phone numbers, tokens, and JWTs shown are placeholders (`+91XXXXXXXXXX`,
  `<REFRESH_TOKEN>`, `<JWT>`). Never paste a real user's phone number, OTP,
  refresh token, or JWT into this doc, a ticket, or a shared terminal session.
- Commands marked **STAGING ONLY** must never be run against production - they
  either send real SMS or intentionally trip a low-traffic policy that could
  briefly affect a real caller sharing that IP/user bucket.
- Commands marked **PRODUCTION SAFE** are read-only or hit a route whose limit
  is generous enough that a handful of manual test calls won't meaningfully
  affect real traffic.

---

## 1. OTP request limit — **STAGING ONLY**

`MOBILE_OTP_REQUEST` sends a real SMS through Twilio in `verifyServiceSid`
config. Never loop this against production - it costs money and can look like
an attack against a real phone number.

```bash
export HOST="<STAGING_HOST>"
export TEST_PHONE="+91XXXXXXXXXX"   # use a test/dev-owned number only

for i in 1 2 3 4; do
  curl -s -o /dev/null -w "attempt $i: %{http_code}\n" \
    -X POST "$HOST/api/auth/request-otp" \
    -H "Content-Type: application/json" \
    -d "{\"phoneNumber\":\"$TEST_PHONE\"}"
done
```

Expected: the configured PHONE-keyed limit (3/min in the tracked config)
trips by the 4th attempt, returning `429` with a `Retry-After` header.

---

## 2. Public home/project read limit — **PRODUCTION SAFE**

Read-only, IP-keyed, no auth needed. Safe to run a small burst against
production to confirm the policy is active (do not loop hundreds of times).

```bash
export HOST="<PRODUCTION_HOST>"

for i in $(seq 1 10); do
  curl -s -o /dev/null -w "attempt $i: %{http_code}\n" "$HOST/api/home"
done
```

`PUBLIC_HOME_READ` is 120/min per IP in the tracked config, so 10 requests
should all return `200`. To actually observe a `429`, raise the loop count
past 120 **only on staging**, or rely on the automated test suite
(`RateLimitingFilterIntegrationTest`) which already proves the block behavior
with tiny configured limits - there is no need to trip a real production
bucket just to confirm the mechanism works.

---

## 3. Public search limit — **PRODUCTION SAFE** (light burst only)

```bash
export HOST="<PRODUCTION_HOST>"

for i in $(seq 1 5); do
  curl -s -o /dev/null -w "attempt $i: %{http_code}\n" \
    "$HOST/api/public/search?q=test&cityId=1"
done
```

`PUBLIC_SEARCH` keys on IP + a normalized fingerprint of `q`. All 5 should
return `200` (limit is 60/min). Do not run a loop designed to actually trip
this in production - it shares a bucket with real users on the same IP
(relevant behind a corporate/NAT gateway or if run from the same jump host
other tooling uses).

---

## 4. Public calculator POST limit — **STAGING ONLY** to actually trip it

Body-aware (`BODY_FINGERPRINT` + `IP`), so this one caches the request body in
memory briefly. Fine to call a couple of times on production for a smoke
check; do not loop past the limit there.

```bash
export HOST="<STAGING_HOST>"

BODY='{"cityId":1,"propertyCategory":"RESIDENTIAL","propertyValue":5000000}'

for i in 1 2 3; do
  curl -s -o /dev/null -w "attempt $i: %{http_code}\n" \
    -X POST "$HOST/api/public/stamp-duty/calculate" \
    -H "Content-Type: application/json" \
    -d "$BODY"
done
```

Expected: `PUBLIC_CALCULATOR_WRITE`'s `BODY_FINGERPRINT` dimension (30/min in
the tracked config) will not trip in 3 calls - this just confirms `200 OK`
end-to-end. To see the actual `429`, either raise the loop count on staging
past 30, or trust the automated suite.

---

## 5. Authenticated profile/favorite/review action limit — **STAGING ONLY**

Requires a real (test-account) JWT. Never use a production user's token here.

```bash
export HOST="<STAGING_HOST>"
export JWT="<JWT>"   # a staging test-account access token only

# Profile read (MOBILE_PROFILE_READ, 120/min)
curl -s -o /dev/null -w "profile read: %{http_code}\n" \
  "$HOST/api/profile" -H "Authorization: Bearer $JWT"

# Favorite toggle (MOBILE_FAVORITE_WRITE, 60/min)
curl -s -o /dev/null -w "favorite toggle: %{http_code}\n" \
  -X POST "$HOST/api/project-favorites/1/toggle" -H "Authorization: Bearer $JWT"

# Review write (MOBILE_REVIEW_WRITE, 5/min - the tightest of the three)
for i in 1 2 3 4 5 6; do
  curl -s -o /dev/null -w "review attempt $i: %{http_code}\n" \
    -X POST "$HOST/api/projects/1/reviews" \
    -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" \
    -d '{"rating":5,"comment":"smoke test"}'
done
```

`MOBILE_REVIEW_WRITE` is the tightest configured limit (5/min per user), so
the 6th call above should return `429`. This is the fastest representative
check that authenticated userId-keyed policies are wired correctly end to
end.

---

## 6. Checking `Retry-After` — **PRODUCTION SAFE**

Whenever any of the above returns `429`, confirm the contract:

```bash
curl -sD - -o /dev/null "$HOST/api/home" | grep -i "retry-after"
```

Expected: a `Retry-After: <positive integer>` header, and a JSON body like:

```json
{
  "status": 429,
  "error": "TOO_MANY_REQUESTS",
  "message": "Too many requests. Please try again later.",
  "retryAfterSeconds": 42,
  "policy": "PUBLIC_HOME_READ"
}
```

If `Retry-After` is ever missing or `retryAfterSeconds` is `0`, treat that as
a real regression - `RateLimitDecision.block()` always floors it at `1` (see
`RateLimitResponseContractTest`), so a `0` means something upstream (a proxy,
a load balancer) is stripping or rewriting the header.

---

## 7. Checking logs do not expose sensitive data — **PRODUCTION SAFE**

Rate-limit warnings log to the root/app logger (`com.brandPitara.sfs.ratelimit.*`
is not routed to a dedicated file - it lands in `sfs-app.log` alongside other
application INFO/WARN logs; see `docs/PRODUCTION_LOGGING.md`).

```bash
# Using the sfs-logs helper (see docs/PRODUCTION_LOGGING.md):
sfs-logs tail app | grep "Rate limit exceeded"

# Or directly:
grep "Rate limit exceeded" /var/log/sfs/app/sfs-app.log | tail -20
```

A healthy blocked-request log line looks like:

```
Rate limit exceeded: policy=PUBLIC_HOME_READ method=GET path=/api/home keyType=IP keyHash=a1b2c3d4e5f6 retryAfterSeconds=42
```

Confirm the line contains **only**:
- `policy` (an enum name, e.g. `MOBILE_OTP_REQUEST`)
- `method` (`GET`/`POST`/etc.)
- `path` (the URI path only - never a query string, since
  `HttpServletRequest#getRequestURI()` excludes it)
- `keyType` (an enum name, e.g. `PHONE`, `IP`, `IP_OR_USER`)
- `keyHash` (a 12-hex-character non-reversible fingerprint)
- `retryAfterSeconds` (an integer)

It must **never** contain: a raw phone number, a raw OTP code, a raw refresh
token or JWT, a raw request body, a raw email, or anything resembling
`user:<id>` / `ip:<address>` in plaintext (those are hashed before logging -
see `RateLimitLoggingSafetyTest` for the automated version of this check).

If you ever see a raw sensitive value in this log line, treat it as a
security incident, not a rate-limit bug - stop, redact/rotate as needed, and
fix the leak in `RateLimitingFilter`/`RateLimitKeyResolver` before the next
deploy.
