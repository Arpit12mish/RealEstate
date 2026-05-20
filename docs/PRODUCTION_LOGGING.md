# SFS Production Logging Guide

## 1. What this system does

Every HTTP request gets a unique `X-Request-Id` correlation ID that appears in all log lines for that request.  
Five separate daily-rolling log files capture different concerns so you can grep for exactly what you need.

| File | What goes in it |
|---|---|
| `sfs-api.log` | Every completed API request: method, path, status, duration, userId, role |
| `sfs-error.log` | ERROR level only: unhandled exceptions, 500s, DB errors with stack traces |
| `sfs-security.log` | JWT/auth failures, role access denied, dashboard login failures |
| `sfs-audit.log` | High-value dashboard write actions |
| `sfs-app.log` | Application startup, business/system INFO logs |

All log entries are JSON (via logstash-logback-encoder) and include `requestId`, `userId`, `role`, `env`, and `app`.

---

## 2. Log file locations

```
/var/log/sfs/
  app/
    sfs-api.log
    sfs-error.log
    sfs-security.log
    sfs-audit.log
    sfs-app.log
  archive/
    sfs-api.2026-05-20.0.log.gz
    sfs-error.2026-05-20.0.log.gz
    ...
```

Override the root directory at runtime:
```bash
export SFS_LOG_DIR=/custom/path
# or in systemd unit:
Environment="SFS_LOG_DIR=/custom/path"
```

---

## 3. Retention policy

| Log | Retention |
|---|---|
| API | 14 days |
| Error | 30 days |
| Security | 60 days |
| Audit | 180 days |
| App | 14 days |

Archives are gzip-compressed (`.log.gz`).  
`totalSizeCap` prevents unbounded disk growth.

---

## 4. Event names (searchable in JSON)

| Event | Log file | Meaning |
|---|---|---|
| `api_request_completed` | api | Normal completed request |
| `api_request_failed` | api | Request ended with 5xx or exception |
| `slow_api` | api | Request exceeded `sfs.logging.slow-api-threshold-ms` (default 1500ms) |
| `jwt_expired` | security | Mobile access token expired |
| `jwt_malformed` | security | Token is not a valid JWT |
| `jwt_signature_invalid` | security | Token signature check failed |
| `jwt_invalid` | security | Token is invalid (unsupported, illegal) |
| `jwt_auth_failed` | security | Generic JWT processing failure |
| `refresh_token_invalid` | security | Refresh token is bad/expired |
| `dashboard_login_failed` | security | Dashboard email/password wrong |
| `dashboard_jwt_expired` | security | Dashboard access token expired |
| `dashboard_jwt_invalid` | security | Dashboard JWT invalid/malformed |
| `otp_verify_failed` | security | OTP verification failure |
| `auth_required` | security | Request hit a protected endpoint without a token |
| `access_denied_role` | security | Authenticated user lacks required role |
| `server_error` | error | Unhandled 500 exception |
| `validation_failed` | error | 400 validation failure |
| `database_error` | error | Data integrity violation |

---

## 5. Sensitive data policy

The following data is **never written to log files**:

- JWT tokens (access or refresh)
- OTP codes
- Passwords
- Full phone numbers (masked: `98******10`)
- Full email addresses (masked: `s***l@gmail.com`)
- Authorization headers (masked: `Bearer ****`)
- Query params: `token`, `password`, `otp`, `code`, `pin`, `secret`, `payment`, `card`, `cvv`, `upi`
- Request bodies (never logged)

---

## 6. How to check today's 500 errors

```bash
sfs-logs status 500 today

# or directly:
grep '"status":500' /var/log/sfs/app/sfs-api.log | tail -50

# with pretty-print (requires jq):
grep '"status":500' /var/log/sfs/app/sfs-api.log | jq '.'
```

---

## 7. How to check auth failures

```bash
sfs-logs security today

# specific event:
sfs-logs event jwt_expired today
sfs-logs event dashboard_login_failed today

# directly:
grep '"event":"jwt_expired"' /var/log/sfs/app/sfs-security.log
```

---

## 8. How to trace one requestId end-to-end

Every request gets `X-Request-Id` in the response header.  
Copy it and search all logs:

```bash
sfs-logs request req-abc1234567890123

# or directly:
grep 'req-abc1234567890123' /var/log/sfs/app/sfs-api.log
grep 'req-abc1234567890123' /var/log/sfs/app/sfs-error.log
grep 'req-abc1234567890123' /var/log/sfs/app/sfs-security.log
```

---

## 9. How to check slow APIs

```bash
sfs-logs slow today

# or:
grep '"event":"slow_api"' /var/log/sfs/app/sfs-api.log

# change the threshold (default 1500ms) in application.yml or environment:
SFS_SLOW_API_THRESHOLD_MS=2000
```

---

## 10. How to check logs between a time range

```bash
sfs-logs between "2026-05-20 10:00" "2026-05-20 12:00"

# or with jq on raw JSON:
jq 'select(.["@timestamp"] >= "2026-05-20T10:00" and .["@timestamp"] <= "2026-05-20T12:00")' \
  /var/log/sfs/app/sfs-api.log
```

---

## 11. How to install sfs-logs on EC2

```bash
# From the project root, after deploying the JAR:
sudo cp scripts/sfs-logs /usr/local/bin/sfs-logs
sudo chmod +x /usr/local/bin/sfs-logs

# Verify:
sfs-logs help
```

---

## 12. How to create log directory with correct permissions

Run once on EC2 after first deploy:

```bash
# Replace "ec2-user" or "ubuntu" with your actual app service user
APP_USER="sfs"

sudo mkdir -p /var/log/sfs/app /var/log/sfs/archive
sudo chown -R ${APP_USER}:${APP_USER} /var/log/sfs
sudo chmod -R 750 /var/log/sfs
```

If you run the app as `root` (not recommended) or the default OS user, adjust accordingly.

For systemd units, set `User=` and `Group=` in the unit file and ensure the log directory is owned by that user.

---

## 13. How to verify log rotation

Logback rotates automatically at midnight and when a file exceeds `maxFileSize`.  
To force a rotation test manually:

```bash
# 1. Check current log files exist:
ls -lh /var/log/sfs/app/

# 2. Trigger app traffic, then check archive:
ls -lh /var/log/sfs/archive/

# 3. Verify gzip integrity on an archive:
gzip -t /var/log/sfs/archive/sfs-api.2026-05-20.0.log.gz && echo "OK"
```

---

## 14. How to troubleshoot missing logs

**Logs not appearing at all:**
```bash
# Check app process is running:
ps aux | grep sfs

# Check if log directory exists and is writable:
ls -la /var/log/sfs/
ls -la /var/log/sfs/app/

# Check app user owns the directory:
stat /var/log/sfs
```

**Logs appear in console but not files:**
- The `local` Spring profile is active. Files are only written for non-local profiles.
- Check: `grep "profiles.active" /path/to/application.yml` or `echo $SPRING_PROFILES_ACTIVE`

**Wrong log directory:**
```bash
echo $SFS_LOG_DIR
sfs-logs disk
```

**Permission denied errors in app startup:**
```bash
# Check the app user owns the log directory
sudo chown -R $(whoami) /var/log/sfs
```

---

## 15. EC2 useful one-liners

```bash
# Last 20 API errors today:
grep '"status":5' /var/log/sfs/app/sfs-api.log | tail -20 | jq '.'

# Count requests per status code:
grep '"status"' /var/log/sfs/app/sfs-api.log | grep -oP '"status":\K[0-9]+' | sort | uniq -c | sort -rn

# Top 10 slowest requests today:
grep '"event":"api_request_completed"' /var/log/sfs/app/sfs-api.log \
  | jq '.durationMs' | sort -rn | head -10

# Count auth failures today:
grep '"event":"jwt_' /var/log/sfs/app/sfs-security.log | wc -l

# Count dashboard login failures today:
grep '"event":"dashboard_login_failed"' /var/log/sfs/app/sfs-security.log | wc -l

# Live tail all errors:
sfs-logs tail errors

# Disk usage:
sfs-logs disk
```

---

## 16. New configuration properties

Added to `application.yml`:

| Property | Default | Description |
|---|---|---|
| `sfs.logging.slow-api-threshold-ms` | `1500` | Requests slower than this are logged as `slow_api` |

Override via environment variable:
```bash
SFS_SLOW_API_THRESHOLD_MS=2000
```

---

## 17. Test checklist

### API logging
- [ ] Public API returns 200 → `api_request_completed` in sfs-api.log
- [ ] Protected API without token → 401 in sfs-api.log + `auth_required` in sfs-security.log
- [ ] Expired JWT → 401 + `jwt_expired` in sfs-security.log
- [ ] Wrong role → 403 + `access_denied_role` in sfs-security.log
- [ ] Bad request body → 400 + `validation_failed` in error log
- [ ] Unhandled exception → 500 + `server_error` in error log with stack trace
- [ ] Slow request (simulate with Thread.sleep) → `slow_api` event

### Security logging
- [ ] Dashboard login with wrong password → `dashboard_login_failed` in sfs-security.log
- [ ] Dashboard JWT expired → `dashboard_jwt_expired`
- [ ] Mobile JWT expired → `jwt_expired`
- [ ] Malformed JWT → `jwt_malformed`

### Sensitive data
- [ ] `curl` logs do NOT contain Bearer token value
- [ ] OTP field in query params is masked as `****`
- [ ] Phone number in logs is masked `98******10`

### Correlation ID
- [ ] Every response has `X-Request-Id` header
- [ ] requestId in sfs-api.log matches `X-Request-Id` in response
- [ ] Client sends `X-Request-Id: custom-id` → same ID echoed back and logged

### File rotation
- [ ] `/var/log/sfs/app/sfs-api.log` exists after first request
- [ ] After 1 day, archive appears in `/var/log/sfs/archive/` as `.log.gz`
- [ ] `sfs-logs disk` shows log directory size
