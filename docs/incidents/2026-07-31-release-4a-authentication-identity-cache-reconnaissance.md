# Release 4A authentication identity lookup reconnaissance

Date: 2026-07-31

Scope: mobile and dashboard access-token identity lookup only. Async logging,
rate-limit redesign, public response caching, infrastructure changes, and staging
load testing are excluded.

## Current request authentication

### Mobile

`JwtRequestFilter` parses and signature-validates the JWT subject and
`principalType`. For every `USER` request it invokes
`AppUserDetailsService.loadUserByUsername(subject)`. Phone-like subjects are
normalized and queried through `findByPhoneNumberIn`; email fallback may add a
second query. The filter then validates subject equality and expiry and creates a
`UsernamePasswordAuthenticationToken`.

The resulting Spring `UserDetails` currently contains the mobile user's password
hash even though access-token validation never reads it. The fields actually needed
for access authentication are user ID, canonical phone/username, verified/enabled
state, and current role/authority.

Guest JWTs validate their signed installation ID directly and perform no identity
database lookup.

### Dashboard

`DashboardJwtAuthenticationFilter` parses the signed email subject and loads a
`DashboardUserEntity` through `DashboardUserDetailsService` for every dashboard or
admin request. It rejects missing or inactive users, validates access-token type,
principal type, subject, expiry, and enabled state, and builds authorities from the
database role.

`DashboardUserDetails` currently retains the full JPA entity, including its password
hash. The access filter only requires dashboard user ID, normalized email, display
name, active state, and current role/authority.

Mobile and dashboard must use separate cache namespaces: they have different tables,
identifiers, enabled semantics, roles, JWT signing services, and principal models.

## Existing security semantics

- JWT signatures and expiry are checked on every request and must remain uncached.
- Mobile authorization uses the current database role rather than trusting the JWT
  role claim.
- Dashboard authorization uses the current database role and rejects inactive users.
- Mobile `is_verified` is mapped to `UserDetails.enabled`; however the existing
  filter does not check `isEnabled()` before constructing an authenticated token.
  Release 4A must close this stale/disabled-identity gap rather than preserve it.
- Missing identities result in no authenticated security context.
- Dashboard permissions are derived entirely from `DashboardRole`; there is no
  separate permission table or permission-version field.
- Neither mobile nor dashboard has a security-version column.
- Mobile logout-all revokes refresh tokens only. Existing access JWTs remain valid,
  so cache invalidation on logout-all is not required by current semantics and would
  not revoke the access token.

No raw JWT, refresh token, OTP, password hash, or JPA entity may enter the cache.

## Identity mutation paths

Mobile paths discovered:

- enable/verification and initial password creation: `UserServiceImpl`
- role selection: `OnboardingServiceImpl.chooseRole`
- provider role correction: `ProviderProfileServiceImpl.onboardOrUpdateMyProfile`
- deletion: `ProfileServiceImpl.deleteAccount`
- profile fields such as name/email/photo do not affect access authorization
- no current mobile disable or password-change endpoint exists

Dashboard paths discovered:

- role, active state, and optional password replacement:
  `DashboardFixedUserSeeder`
- login updates only `lastLoginAt`, which does not affect authorization
- no runtime dashboard-user administration/disable/delete endpoint currently exists

Every existing security-affecting mutation must invalidate after transaction commit,
not before it. Invalidating before commit permits another request to reload and cache
the old row while the mutation is still uncommitted.

## Repeated authenticated-user loads after the filter

Several mobile services query the same user again from `Authentication#getName()`,
including `CurrentUserService`, favorites, project favorites, business search, and
business response personalization. A snapshot principal containing the stable user ID
can remove ID-only repeats. Operations that genuinely require a managed `User` entity
still need an explicit repository read.

Dashboard services obtain the current actor through `DashboardCurrentUserService`.
The current principal already carries the JPA entity, avoiding another query but
making it unsafe to cache. A detached compatibility view can be created per request
from the immutable snapshot without storing an entity or proxy in the cache.

## PostgreSQL 16 baseline

Measured using real mobile and dashboard filters, signed JWTs, PostgreSQL 16,
Hibernate query statistics, and a Hikari pool capped at three connections. Latencies
cover the authentication filter and terminal no-op endpoint chain.

| Workload | Requests | Identity queries | p50 | p95 | Peak active | Peak pending |
|---|---:|---:|---:|---:|---:|---:|
| Mobile, one user, sequential | 100 | 100 | 3.299 ms | 7.073 ms | 1 observed by query ownership | 0 |
| Mobile, one user, concurrent | 100 | 100 | 98.937 ms | 110.373 ms | 3 | 6 |
| Mobile, ten users | 100 | 100 | 2.603 ms | 3.821 ms | 1 observed by query ownership | 0 |
| Guest | 100 | 0 | 0.906 ms | 1.322 ms | 0 | 0 |
| Dashboard, ten users | 100 | 100 | 2.132 ms | 3.521 ms | 1 observed by query ownership | 0 |

All requests completed; Hikari timeouts were zero and pending returned to zero. The
concurrent result demonstrates that identical cold identity lookups are not currently
coalesced.

## Planned cache boundary

- immutable mobile and dashboard authentication snapshots;
- Caffeine caches keyed by stable database ID, in isolated namespaces;
- `Cache.get(key, mappingFunction)` atomic loading to coalesce a cold-key stampede;
- bounded maximum size and expire-after-write;
- no negative caching of missing or disabled users;
- low-cardinality metrics tagged only by namespace/outcome;
- transaction-after-commit invalidation for every current security mutation;
- direct lookup fallback when caching is disabled;
- legacy mobile tokens without a user ID remain supported through the current
  uncached subject lookup path.

The TTL is only a fallback bound. Security-affecting application mutations require
explicit invalidation so disabled, deleted, or role-changed identities are not
authorized until expiry.
