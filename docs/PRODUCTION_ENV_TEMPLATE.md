# Production Environment Template

This is the full set of environment variables `application-prod.yml` reads.
Copy the block below into `/etc/sfs-app.env` on the EC2 host and replace every
`REDACTED` with the real value. **Never commit real values anywhere** - this
file only contains placeholders.

Every key here was found by directly inspecting the Java classes that bind
it (see the config inventory below) - not guessed. A few keys the original
request examples omitted were added because the code actually requires them
(`TWILIO_VERIFY_SERVICE_SID`, the `DASHBOARD_SEED_*` sub-fields, `AWS_S3_*`,
`SPRING_MAIL_*`) - flagged inline below.

```env
SPRING_PROFILES_ACTIVE=prod
APP_LOGGING_PATH=/var/log/sfs/app

# ── Database (required, no safe default - HikariCP fails startup if blank) ──
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sfs_db
SPRING_DATASOURCE_USERNAME=sfs_user
SPRING_DATASOURCE_PASSWORD=REDACTED

# ── Mobile/app JWT (required - JwtTokenUtil now fails fast at startup if blank) ──
JWT_SECRET=REDACTED
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_DAYS=15

# ── Dashboard JWT (required - DashboardJwtService now fails fast at startup if blank) ──
DASHBOARD_JWT_SECRET=REDACTED
DASHBOARD_JWT_ACCESS_EXPIRATION_MS=900000
DASHBOARD_REFRESH_EXPIRATION_DAYS=15

# ── Dashboard fixed-user seeding ──
# SAFE DEFAULT IS false/false. If the app currently seeds/resets dashboard
# admin/reviewer/data-entry passwords on every restart (it does today, per
# the gitignored application.yml), that behavior needs an EXPLICIT decision
# before rollout - see "Dashboard seed" in the risk notes below. Do not set
# these to true without that sign-off.
DASHBOARD_SEED_ENABLED=false
DASHBOARD_SEED_UPDATE_PASSWORDS=false
# Only read if DASHBOARD_SEED_ENABLED=true. Leave blank (seeder skips
# creation for any role whose email/password is blank) unless seeding is
# intentionally turned on.
DASHBOARD_SEED_ADMIN_EMAIL=
DASHBOARD_SEED_ADMIN_PASSWORD=
DASHBOARD_SEED_ADMIN_NAME=SFS Admin
DASHBOARD_SEED_REVIEWER_EMAIL=
DASHBOARD_SEED_REVIEWER_PASSWORD=
DASHBOARD_SEED_REVIEWER_NAME=SFS Reviewer
DASHBOARD_SEED_DATA_ENTRY_EMAIL=
DASHBOARD_SEED_DATA_ENTRY_PASSWORD=
DASHBOARD_SEED_DATA_ENTRY_NAME=SFS Data Entry

# ── Twilio Verify (OTP) ──
# Required - TwilioOtpServiceImpl's @PostConstruct calls Twilio.init() eagerly
# for the prod profile, so a blank accountSid/authToken fails startup
# immediately (this is Twilio SDK behavior, not something added by this task).
TWILIO_ACCOUNT_SID=REDACTED
TWILIO_AUTH_TOKEN=REDACTED
# NOTE: not in the original request's example list, but the code actually
# requires it - every Verify API call uses verifyServiceSid, not phoneNumber.
TWILIO_VERIFY_SERVICE_SID=REDACTED
# NOTE: twilio.phoneNumber is bound but never actually read anywhere in
# TwilioOtpServiceImpl's Verify-API flow (confirmed via source inspection) -
# currently vestigial. Still externalized for completeness/future use.
TWILIO_PHONE_NUMBER=REDACTED

# ── Google Places / Maps (connectivity, nearby-place features) ──
GOOGLE_MAPS_PLACES_ENABLED=true
GOOGLE_PLACES_API_KEY=REDACTED
GOOGLE_PLACES_BASE_URL=https://places.googleapis.com/v1
GOOGLE_PLACES_TEXT_SEARCH_URL=https://places.googleapis.com/v1/places:searchText
GOOGLE_PLACES_TIMEOUT_MS=8000
GOOGLE_PLACES_MAX_RESULTS=10
GOOGLE_PLACES_MAX_RADIUS_METERS=5000

# ── Meta / Instagram (LIVE feature - do not disable) ──
META_INSTAGRAM_SYNC_ENABLED=true
META_ACCESS_TOKEN=REDACTED
META_APP_ID=REDACTED
META_APP_SECRET=REDACTED
META_FACEBOOK_PAGE_ID=REDACTED
META_INSTAGRAM_BUSINESS_ACCOUNT_ID=REDACTED
META_GRAPH_API_VERSION=v25.0
META_INSTAGRAM_PUBLIC_LIMIT=10
META_INSTAGRAM_REQUEST_TIMEOUT_SECONDS=15
META_INSTAGRAM_SYNC_CRON=0 0 * * * *

# ── Elasticsearch - stays DISABLED. Do not enable. ──
SFS_SEARCH_ENABLED=false
# Inert placeholder, not a real cluster - ElasticsearchConfig builds its
# client beans unconditionally (no feature flag gates bean construction),
# and HttpHost.create("") throws at startup, so this needs a syntactically
# valid URL even while disabled. Leave as-is unless Elasticsearch is
# genuinely being turned on.
ELASTICSEARCH_URL=http://localhost:9200
ELASTICSEARCH_API_KEY=

# ── AWS S3 (media uploads/presigned URLs) ──
# bucket/region are not secrets - safe as plain values, override only if
# they change.
AWS_S3_BUCKET=sfs-s3bucket
AWS_S3_REGION=ap-south-1
AWS_S3_PUBLIC_BASE_URL=
AWS_S3_PRESIGN_EXPIRY_SECONDS=300
# Leave BOTH blank in production - AwsS3Config already falls back to
# DefaultCredentialsProvider (supports an EC2 IAM instance role) whenever
# AWS_ACCESS_KEY_ID is blank. Only set these if IAM role access is genuinely
# unavailable on this host.
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=

# ── Mail ──
# Not used to send anything today (EmailService is dead/commented-out code)
# and management.health.mail.enabled=false already disables the only thing
# that would otherwise probe this at /actuator/health. Leave blank unless
# EmailService is revived.
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=
SPRING_MAIL_PASSWORD=
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
```

## Config key inventory and classification

| Property key | Source class | Hardcoded today? | Secret? | Feature owner | Required in prod? | Has default? | Recommended env var | Action needed | Class |
|---|---|---|---|---|---|---|---|---|---|
| `spring.datasource.url/username/password` | Spring Boot autoconfig (Hikari) | Yes (`application.yml`) | password: yes | Core | Yes - context fails to start if blank | No | `SPRING_DATASOURCE_*` | Externalize, no default | A |
| `jwt.secret` / `jwt.expiration.ms` | `JwtTokenUtil` | Yes | secret: yes | Mobile auth | Yes - now fails fast at startup (new `@PostConstruct`) | No | `JWT_SECRET`, `JWT_EXPIRATION_MS` | Externalize, no default | A |
| `jwt.refresh.expiration.days` | `RefreshTokenServiceImpl` | Yes | No | Mobile auth | No | Yes (`:15`) | `JWT_REFRESH_EXPIRATION_DAYS` | Externalize, keep default | D |
| `dashboard.jwt.secret` / `.access-expiration-ms` | `DashboardJwtService` | Yes | secret: yes | Dashboard auth | Yes - now fails fast at startup (new `@PostConstruct`) | No | `DASHBOARD_JWT_SECRET`, `DASHBOARD_JWT_ACCESS_EXPIRATION_MS` | Externalize, no default | A |
| `dashboard.refresh.expiration-days` | `DashboardRefreshTokenService` | Yes | No | Dashboard auth | Yes (`@Value` with **no** default - would fail startup if absent from every source) | No in Java, yes in yml now (`:15`) | `DASHBOARD_REFRESH_EXPIRATION_DAYS` | Externalize, yml-level default added | D |
| `dashboard.seed.enabled` / `.update-passwords` | `DashboardFixedUserSeeder` | Yes (**currently `true`/`true`**) | No | Dashboard auth | No | Yes (`:false`) | `DASHBOARD_SEED_ENABLED`, `DASHBOARD_SEED_UPDATE_PASSWORDS` | **Flagged - see risk notes** | F (behavior) |
| `dashboard.seed.{admin,reviewer,data-entry}.email/password` | `DashboardFixedUserSeeder` | Yes, 3 real plaintext passwords | Yes | Dashboard auth | No | Yes (empty string) | `DASHBOARD_SEED_*` | Externalize, no secret default | F |
| `twilio.accountSid/authToken/verifyServiceSid` | `TwilioProperties` / `TwilioOtpServiceImpl` | Yes | authToken: yes | Mobile OTP | Yes - `@PostConstruct Twilio.init()` fails startup for `dev/prod/staging` profiles | No | `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_VERIFY_SERVICE_SID` | Externalize, no default | A |
| `twilio.phoneNumber` | `TwilioProperties` | Yes | No (vestigial, unread) | Mobile OTP | No (dead field) | No | `TWILIO_PHONE_NUMBER` | Externalize for completeness | D |
| `otp.provider` / `msg91.*` | none - zero Java consumers | Yes (placeholder values only) | No | Dead config | No | n/a | n/a | Safe to delete (not done, low priority) | E |
| `elasticsearch.url` / `.api-key` | `ElasticsearchConfig` | Yes | api-key: yes | Search (disabled) | **Bean construction is unconditional** - blank URL fails startup | Now yes (`:http://localhost:9200` / `:`) | `ELASTICSEARCH_URL`, `ELASTICSEARCH_API_KEY` | Fixed with safe inert default | C |
| `sfs.search.enabled` | `BusinessSearchServiceImpl`, `BusinessIndexInitializer` | Yes (`false`) | No | Search (disabled) | No | Yes (`:false`) | `SFS_SEARCH_ENABLED` | Keep `false` | C |
| `google.places.*`, `google.maps.places.*` | `GooglePlacesProperties` (x2 classes), `GooglePlacesClient`, `GoogleNearbyPlaceProvider` | Yes, real API key | api-key: yes | Connectivity/nearby-places | No (fails on first call, not at boot) | Partial | `GOOGLE_PLACES_*`, `GOOGLE_MAPS_PLACES_ENABLED` | Externalize, no secret default | B |
| `aws.credentials.access-key/secret-key` | `AwsS3Config` | Yes | Yes | Media/S3 | No - falls back to `DefaultCredentialsProvider` (EC2 IAM role) | Yes (`:` empty) | `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | Leave blank, prefer IAM role | D/F |
| `app.media.s3.bucket/region/publicBaseUrl/presignExpirySeconds` | `S3Properties` | Yes | No (identifiers, not secrets) | Media/S3 | `region` yes - `Region.of(null)` NPEs at startup | Now yes | `AWS_S3_*` | Externalize with real non-secret defaults | D |
| `spring.mail.*` | Spring Boot autoconfig | Yes | password: yes but unused | Dead feature | No - `EmailService` is commented-out dead code | Yes (safe blanks) | `SPRING_MAIL_*` | Externalize, no functional risk | E |
| `sfs.instagram.meta.*` | `InstagramMetaProperties` | Yes, real Meta token+secret | Yes (access-token, app-secret) | Meta/Instagram (**live**) | No (checked at call/cron-fire time, not at boot) | Partial | `META_*` | Externalize, no secret default | B |
| `google.instagram.meta.*` (orphaned duplicate) | **none - unbound, dead** | Yes, a SECOND real Meta token+secret | Yes | Dead/orphaned | No | n/a | n/a | **Deleted** (Step in this task) | F |

## Hardcoded secrets found (values redacted)

All of the following were found hardcoded as real values in the gitignored `application.yml` / `application-local.yml`. **Important: `application.yml` was tracked in git history for 11 commits (`bede9b9` through `3ef80a6`) before being gitignored.** Anything that was present in that history must be treated as compromised regardless of whether the current local value still matches - `git log` doesn't stop a secret from being retrievable once it's been pushed anywhere.

| Secret | Confirmed in git history? | Rotation urgency |
|---|---|---|
| DB password (`spring.datasource.password`) | **Yes** | Rotate - treat as burned |
| JWT signing secret (`jwt.secret`) | **Yes** | Rotate - existing tokens will invalidate, expected |
| Twilio auth token (`twilio.authToken`) | **Yes** | Rotate via Twilio console |
| AWS access key + secret key (`aws.credentials.*`) | **Yes** | Rotate via IAM console; prefer migrating to EC2 instance role entirely (see below) instead of reissuing a new key pair |
| Elasticsearch API key (`elasticsearch.api-key`) | **Yes** | Rotate via Elastic Cloud console (low urgency - search is disabled and this key grants access to nothing currently in active use) |
| Dashboard JWT signing secret | Not found in the 11 historical commits (added after gitignore) | Rotate anyway - good hygiene, low urgency |
| Dashboard seed admin/reviewer/data-entry passwords (3 real plaintext passwords) | Not found in history | Change these 3 accounts' real passwords directly; do not rely on the seeder to do it silently going forward (see risk notes) |
| Google Places API key | Not found in history | Rotate at low priority |
| Meta/Instagram access token + app secret (appeared **twice** - once under the real, bound `sfs.instagram.meta.*`, and again under a second, dead, unbound `google.instagram.meta.*` block) | Not found in history | Rotate via Meta developer console at your convenience; the duplicate dead copy has been deleted from the local file |

No secret value is reproduced above or anywhere else in this document.
