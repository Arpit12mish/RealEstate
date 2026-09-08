# CMS Phase 8 Certification Record

Certification date: 2026-08-19  
Migration head at certification: `V155`  
Branch at start: `fix/hikari-pool-exhaustion`  
HEAD at start: `9ec1c69864b40121dc4753a1af4b9051f6890c56`

This record captures the backend acceptance evidence used before dashboard implementation. It does not certify that AWS CloudFront/OAC infrastructure has been provisioned.

**Post-certification update (D6, 2026-08-27):** `V156__support_content_document_table_callout_checklist.sql` was added after this record was written, raising the migration head to `V156`. It only widens the `content_document` schema-version CHECK to allow schema version 3 (already the write version this record's own acceptance coverage exercises, per the "Structured-content security" section below) — additive and backward-compatible with everything certified here. No re-certification of already-covered behavior was required; see `CMS_API_CONTRACT.md` for the current contract.

## Acceptance coverage

- Real Spring Security filter-chain requests cover login, ADMIN staff creation, Writer/Editor/Publisher JWTs, unauthenticated 401, and wrong-authority 403.
- The HTTP editorial sequence covers author/category/tag creation, a Writer-owned draft, taxonomy/cover metadata, a realistic schema-v2 document, revision 1, changes requested, revision 2, approval, publication, public read, unpublish, 404, and republish.
- The published response is asserted against revision 2 while revision 1 remains immutable.
- Shared metadata/document optimistic locking is tested in both save orders; stale writes return 409.
- Public ETag/304, no-store errors, stable public media URLs, private-storage field omission, and revision-backed list/detail behavior are covered.
- Existing media tests use the fake storage adapter to cover JPEG, PNG, WebP, MP4, SVG rejection, spoofed signatures, missing/oversize/mismatched objects, finalization, and conditional `If-None-Match: *` upload headers. No production AWS call is made.
- PostgreSQL 16/Testcontainers exercises fresh migration through V155 and application startup with Hibernate schema validation. Forward-only migration tests also cover production-shaped pre-CMS schemas.

## Structured-content security

The document parser rejects unknown properties and polymorphic types. Tests cover unknown blocks/marks, raw HTML/iframe blocks, arbitrary `src`, style/event attributes, unsafe `javascript:` and `data:` links, unsupported media, media type/status mismatches, and excessive nesting. Text such as `<script>alert(1)</script>` remains an ordinary text value; renderers must escape it and must never use arbitrary `dangerouslySetInnerHTML`.

Certified limits include boundary and boundary-plus-one cases for:

- 750 blocks
- 20,000 characters per text node
- 500,000 aggregate text characters
- 200 list items
- 1,000 caption characters
- 768 KiB canonical document JSON
- 1 MiB document request payload

Excess input maps to deterministic 400/413 responses rather than an exception trace or 500.

## Query bounds

Hibernate statistics assertions certify:

| Operation | Bound |
|---|---:|
| Dashboard content page including owner/updater labels | 1 measured; maximum 2 |
| Public detail without referenced media | 1 measured; maximum 2 with one bulk media query |
| Public list without covers | 2 measured: projection + count |
| Public list with covers | projection/count + at most 1 bulk cover query |
| Author selector including profile-media IDs | 1 measured for the non-empty first page; maximum 2 with count |
| Revision detail | readable-post check + revision + at most 1 bulk media query |

Content list projections exclude document JSON. Public list projections read immutable revision card fields and do not load revision documents or workflow history. Media IDs are deduplicated and bulk loaded.

## Lightweight concurrent-read check

The acceptance test launches 50 concurrent public-detail requests followed by 50 concurrent public-list requests against Spring MVC, Spring Security, rate limiting, and PostgreSQL. The representative focused run produced:

| Route | Success | 5xx | p50 | p95 | p99 | max Hikari active | max Hikari pending |
|---|---:|---:|---:|---:|---:|---:|---:|
| Detail | 50/50 | 0 | 45 ms | 54 ms | 59 ms | 4 | 46 |
| List | 50/50 | 0 | 14 ms | 36 ms | 37 ms | 5 | 26 |

This is a burst regression check, not capacity planning. Pending acquisition briefly rose because the test pool is intentionally small; all requests completed without timeout or 5xx and latency stayed below 100 ms. Production pool/thread sizing still requires environment-specific load testing.

## Rate limiting

`PUBLIC_CMS_CONTENT_READ` binds to one primary identity, capacity 120, refill 60/minute. A deterministic test proves requests 1–120 are allowed, request 121 is rejected with a positive retry interval, and a different identity remains unaffected. This validates policy behavior without generating production traffic.

## Defects discovered during certification

1. A scoped public-content exception was intercepted by the global dashboard advice in the full application context and returned 500. Scoped public/dashboard advice now has explicit highest precedence; unpublished public detail returns the intended 404 with `no-store`.
2. Login and `/me` did not expose effective CMS permissions/profile names, leaving the dashboard unable to drive permission-aware controls. The auth user response now includes both, with ADMIN's implicit CMS permissions represented explicitly.
3. The author selector could lazily fetch profile-media references per row. The repository page query now entity-fetches that single-valued reference and is guarded by a Hibernate-statistics assertion.

## Database safety

Database tests cover unique content/taxonomy/storage slugs/keys, valid content type/status/media lifecycle checks, unique per-post revision numbers, revision immutability, and cross-post workflow pointer rejection. Cover/media type and READY-state rules are cross-row business invariants enforced transactionally by the application; PostgreSQL foreign keys still enforce referenced-row existence. No new migration was required for Phase 8.

## Infrastructure boundary

The application validates `CMS_MEDIA_PUBLIC_BASE_URL`, derives stable HTTPS delivery URLs without an AWS API call, and fails closed for media-bearing public articles when delivery cannot be resolved. The S3 bucket remains private. CloudFront distribution, OAC, bucket policy, ACM certificate, DNS, and production verification remain manual infrastructure actions documented in `CMS_MEDIA_CLOUDFRONT_DELIVERY.md`.

## Final verification gate

Focused CMS/security/limits/rate-limit tests passed 40/40. PostgreSQL 16 migration and full-application startup tests passed 16/16 in an execution context with Docker access. The final `./mvnw clean verify` was subsequently forced into the restricted sandbox after the execution environment exhausted its escalation quota. It ran 1,307 tests but could not reach Docker or bind four loopback test sockets: 0 assertion failures, 5 environment errors, 78 skipped, BUILD FAILURE. A final unrestricted `./mvnw clean verify` remains required before this record can be treated as an unconditional release certificate.
