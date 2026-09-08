# Release 3 refresh-token cleanup reconnaissance

Date: 2026-07-31

Scope: mobile `refresh_tokens` cleanup only. Dashboard refresh tokens, identity caching,
logging, rate limiting, public caching, infrastructure, and load testing are excluded.

## Current implementation

`RefreshTokenCleanupScheduler.cleanup()` runs daily at 02:00 and invokes
`RefreshTokenRepository.deleteExpiredTokens(now)`. The repository method is one
JPQL bulk delete:

```sql
delete from refresh_tokens
where expires_at < :now or revoked = true
```

There is no row limit, batch limit, run deadline, failure checkpoint, explicit
same-JVM overlap guard, or multi-instance coordination. The repository invocation
is one transaction, so every matching row is deleted and committed together.

Spring scheduling is enabled by `SfsApplication`. There is no custom scheduler or
`spring.task.scheduling.pool.size` configuration, so Spring Boot's default scheduler
uses one thread. That prevents ordinary scheduled methods from overlapping inside
one application instance today, but it is an incidental executor property rather
than a cleanup invariant. Separate application instances can execute cleanup at the
same time.

## Schema and indexes

The only production migration affecting the mobile table is
`V107__align_mobile_auth_tables.sql`. It defines:

- primary key: `id BIGSERIAL PRIMARY KEY`
- expiry: `expires_at TIMESTAMPTZ NOT NULL`
- revocation: `revoked BOOLEAN NOT NULL DEFAULT FALSE`
- token hash: `token VARCHAR(256) NOT NULL`
- ownership/device fields: `user_id`, `device_id`, and `fcm_token`

Existing indexes:

- primary-key B-tree on `id`
- unique `ux_refresh_tokens_token_hash(token)`
- `idx_refresh_tokens_user_id(user_id)`
- `idx_refresh_tokens_user_device_active(user_id, device_id, revoked, expires_at)`

The composite user/device index cannot efficiently lead a global expiry/revocation
scan because `user_id` and `device_id` precede the cleanup columns.

No existing applied migration will be changed.

## Token lifecycle and transaction boundaries

`RefreshTokenServiceImpl` has class-level `@Transactional`:

- creation/login: generates an opaque token, persists only its SHA-256 digest, and
  commits in the service transaction;
- rotation: locks the submitted digest with `PESSIMISTIC_WRITE`, revokes it, creates
  a replacement, and commits those changes atomically;
- expired rotation: marks the stored token revoked and rejects it;
- reuse response: revokes active tokens for the same user/device, or all user tokens
  if no device is present;
- logout: reads and revokes one token in one transaction;
- logout-all: performs a read-only verification transaction followed by a separate
  bulk revocation transaction;
- account deletion: `ProfileServiceImpl` deletes all user refresh tokens inside the
  wider account-deletion transaction.

Controller composition is not transactional. Login-related user, guest-session,
history, user-details, and refresh-token operations therefore use their respective
service/repository transactions rather than one controller-wide transaction.

No raw token values are stored. Cleanup logging must remain aggregate-only.

## PostgreSQL 16 baseline

Measured with PostgreSQL 16 Alpine using the production table shape and indexes.
Fixtures covered 100 users, eight devices, valid, expired, revoked,
revoked-unexpired, and overlapping expired/revoked rows.

| Fixture | Total | Valid | Expired | Revoked | Revoked-unexpired | Deleted by current SQL | Delete duration |
|---|---:|---:|---:|---:|---:|---:|---:|
| 10k | 10,000 | 6,000 | 2,500 | 2,000 | 1,500 | 4,000 | 3.619 ms |
| 100k | 100,000 | 60,000 | 25,000 | 20,000 | 15,000 | 40,000 | 76.727 ms |

For the current repository call, SQL duration, transaction duration, and connection
hold duration cover the same single unbounded operation, apart from small framework
begin/commit overhead. All 40,000 deletions commit together at 100k rows.

A representative valid-token lock/update/replacement transaction completed in
12.088 ms while the 100k cleanup ran. A subsequent uncontended sample took
17.548 ms, showing no regression in this small local sample; it does not remove the
production risk from larger tables, slower storage, WAL pressure, or cleanup of a
token concurrently being rotated.

## Query-plan evidence

Safe `EXPLAIN (ANALYZE, BUFFERS)` SELECT matching the unbounded combined predicate
at 100k rows:

```text
Seq Scan on refresh_tokens
  actual rows=40000, rows removed by filter=60000
  buffers: shared hit=1819
  execution time: 37.452 ms warm (86.571 ms first sample)
```

PostgreSQL also selected sequential scans for separate revoked-only and expired-only
full selections. Their measured execution times were 9.757 ms and 9.366 ms, but a
split cleanup must scan/lock/delete twice and handle rows matching both predicates.

Deterministic `ORDER BY id LIMIT 1000` candidate selection used the existing primary
key:

```text
combined predicate: Index Scan using refresh_tokens_pkey, 0.763 ms
revoked only:       Index Scan using refresh_tokens_pkey, 1.025 ms
expired only:       Index Scan using refresh_tokens_pkey, 0.467 ms
```

The combined candidate selection with `FOR UPDATE SKIP LOCKED` completed in
2.123 ms. At this candidate density it is simpler and faster overall than running
two cleanup streams, while preserving the exact existing predicate.

## Root cause and planned boundary

The risk is not token lookup correctness; it is unbounded work ownership. One
scheduler invocation owns every qualifying row, lock, WAL record, connection, and
commit until the global delete finishes.

The implementation will use one scheduler/orchestrator with an explicit atomic
same-JVM guard and a separate transactional worker. Each worker call will select at
most the configured number of candidate IDs ordered by primary key using
`FOR UPDATE SKIP LOCKED`, delete those IDs, and commit before the next call. The
orchestrator will stop on exhaustion, configured batch count, configured duration,
or first failure.

`SKIP LOCKED` provides lightweight multi-instance safety: instances may divide the
eligible rows without waiting on the same candidate locks. It does not provide a
cluster-wide single-run guarantee, but avoids the need for a distributed-lock
dependency and keeps every transaction bounded.

## Index decision before implementation

No new index is justified by this representative evidence. The primary key supports
deterministic bounded selection efficiently when cleanup candidates are common, and
an expiry/revocation index would add write cost to token creation, rotation, logout,
and revocation. The PostgreSQL integration test will preserve plan evidence and fail
if the bounded candidate query stops using an index-supported plan.
