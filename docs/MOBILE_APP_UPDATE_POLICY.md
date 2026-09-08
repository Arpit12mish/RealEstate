# Mobile native update policy

This feature controls native Play Store and App Store updates. Android compares
`versionCode`; iOS compares `buildNumber`. Expo EAS Update/runtime versions are
separate and are never used for this decision.

## Public API

```http
GET /api/public/mobile-app/update-policy?platform=ANDROID&currentBuild=20
Cache-Control: no-store
```

```json
{
  "platform": "ANDROID",
  "status": "OPTIONAL",
  "latestVersion": "2.1.0",
  "latestBuild": 21,
  "minimumSupportedBuild": 18,
  "title": "Update available",
  "message": "Update Square Foot Story to get the latest improvements.",
  "releaseNotes": null,
  "remindAfterHours": 24,
  "storeUrl": "https://play.google.com/store/apps/details?id=com.squarefootstory.app"
}
```

Rules, in order:

1. Global or platform emergency disable -> `CURRENT`.
2. Missing policy row -> HTTP 503 `UPDATE_POLICY_UNAVAILABLE`.
3. `DRAFT` or `SUSPENDED` -> `CURRENT`.
4. `OFF` or `OBSERVE` enforcement mode -> `CURRENT`.
5. `currentBuild > latestBuild` -> `CURRENT` plus an operational metric.
6. `currentBuild < minimumSupportedBuild` -> `REQUIRED`.
7. `currentBuild < latestBuild` -> `OPTIONAL`.
8. Otherwise -> `CURRENT`.

Database failures return HTTP 503. The mobile app fails open on network and 5xx
responses. Inactive policies still return `CURRENT`; missing data does not hide
as a valid configuration. Missing or invalid active policies are logged and
increment `sfs.mobile_update.policy_unavailable` for operational alerting.

The response is not publicly cached. A bounded Caffeine cache stores at most one
policy per platform for 30 seconds and is evicted after every successful admin
update.

For multiple backend instances, replace this local cache or add Redis/event
invalidation before relying on instant cross-instance eviction.

## Dashboard API

- `GET /api/dashboard/mobile-app/update-policies`
- `GET /api/dashboard/mobile-app/update-policies/{ANDROID|IOS}`
- `GET /api/dashboard/mobile-app/update-policies/{ANDROID|IOS}/audit`
- `PUT /api/dashboard/mobile-app/update-policies/{ANDROID|IOS}` (ADMIN only)
- `PUT /api/dashboard/mobile-app/update-policies/{ANDROID|IOS}/emergency-disable` (ADMIN only)

Example safe activation:

```json
{
  "latestVersion": "2.1.0",
  "latestBuild": 21,
  "minimumSupportedBuild": 18,
  "storeUrl": "https://play.google.com/store/apps/details?id=com.squarefootstory.app",
  "title": "Update available",
  "message": "Update Square Foot Story to get the latest improvements.",
  "releaseNotes": null,
  "remindAfterHours": 24,
  "policyState": "ACTIVE",
  "storeAvailability": "FULLY_AVAILABLE",
  "availabilityConfirmed": true,
  "enforcementMode": "PROMPT_ONLY",
  "emergencyDisabled": false,
  "changeReason": "Version 2.1.0 verified as fully available in Google Play",
  "expectedVersion": 0
}
```

Every update requires a reason. A stale `expectedVersion` returns
`UPDATE_POLICY_VERSION_CONFLICT`.

## Lifecycle

Policy state:

- `DRAFT`: configuration work only; the app sees `CURRENT`.
- `ACTIVE`: eligible to prompt after all activation checks pass.
- `SUSPENDED`: emergency operational pause; the app sees `CURRENT`.

Store availability:

- `UNVERIFIED`
- `PARTIAL`
- `FULLY_AVAILABLE`

`ACTIVE` requires `FULLY_AVAILABLE`, `availabilityConfirmed=true`, a non-zero
build, a non-bootstrap display version, and a non-`OFF` enforcement mode. The
backend records the confirming admin and time. A partial/staged release cannot
be activated.

Enforcement mode:

- `OFF`: no prompt and no server enforcement.
- `OBSERVE`: no prompt/enforcement; use while measuring future header adoption.
- `PROMPT_ONLY`: mobile update UI is enabled; recommended initial production mode.
- `ENFORCE_REPORTED_BUILDS`: reserved for the future 426 filter; it currently
  behaves like prompt-only because no global filter is installed.

## Store URL allow-list

Android must use:

- host `play.google.com`;
- path `/store/apps/details`;
- query ID `com.squarefootstory.app`.

iOS must use host `apps.apple.com` and a path ending in App Store ID
`6764284866`. User-info, custom ports, fragments, short links, redirects, and
other domains are rejected.

## Audit history

`mobile_app_update_policy_audit` stores immutable:

- previous and new policy snapshots;
- actor ID and name;
- request ID;
- timestamp and reason;
- action: `CREATED`, `UPDATED`, `ACTIVATED`, `MINIMUM_RAISED`, `DEACTIVATED`,
  or `ROLLED_BACK`.

Raising the minimum build and all other writes require a non-empty reason.

## Emergency controls

Fast dashboard control per platform:

- set `emergencyDisabled=true`, or
- change `policyState` to `SUSPENDED`.

The dedicated emergency endpoint accepts only `disabled`, `changeReason`, and
`expectedVersion`. Disabling does not validate the release fields, so even a
policy with a bad store URL can be shut off immediately. Re-enabling validates
the active policy first.

```json
{
  "disabled": true,
  "changeReason": "Emergency rollback: incorrect store listing",
  "expectedVersion": 4
}
```

Global environment kill switch:

```text
SFS_MOBILE_UPDATE_EMERGENCY_KILL_SWITCH_ENABLED=true
```

The global switch returns `CURRENT` without reading policy data. It is disabled
by default and normally requires the application's configuration refresh or
restart, depending on deployment setup.

Because responses use `Cache-Control: no-store` and dashboard updates evict the
internal cache, policy rollback is visible on the next request. A required
mobile screen rechecks whenever the app returns to the foreground.

## Rate limiting

The public route is anonymous and normally keyed by IP. Its ceiling is
300 requests/minute to avoid blocking offices or carrier-NAT users. The mobile
client also checks only once per six hours under normal conditions. Installation
IDs are not treated as trusted security credentials.

## Future HTTP 426 enforcement

Global 426 enforcement remains unimplemented and disabled. Before adding it:

- start in `OBSERVE` and measure native-header adoption;
- identify mobile requests reliably;
- exempt this policy endpoint, health checks, and operational routes;
- ignore missing/malformed headers during adoption;
- preserve `Cache-Control: no-store` on 426 responses;
- verify NGINX passes the JSON body and status unchanged;
- return code `APP_UPDATE_REQUIRED` and the same policy in `data`;
- enable only when the required store build is fully available.

The compatible Expo client already processes 426 before authentication
refresh/logout. Website traffic remains unaffected because no enforcement
filter exists.
