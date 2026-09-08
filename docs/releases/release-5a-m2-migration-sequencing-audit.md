# Release 5A-M2: migration sequencing and baseline-equivalence audit

## Decision

AWS staging remains blocked until this release is validated and committed. The immutable V140 repair had five lower-numbered, unreleased migrations in the working tree. If V140 were applied first, a later normal Flyway run would not apply V135-V139 because `outOfOrder` is not enabled. The files have therefore been resequenced to V142-V146. V141 restores the minimal deterministic reference/configuration rows required by a fresh public API baseline.

## V135-V139 review and sequencing

| Old | New | Scope | Finding | Resolution |
|---|---|---|---|---|
| V135 | V142 | Promo banner video/delete support | Existing named media CHECK could exclude `VIDEO`; the migration silently skipped it | Inspect the actual definition, reject unknown values, replace only the obsolete named CHECK |
| V136 | V143 | Company-project CMS/media | `slug VARCHAR(160)` disagreed with the entity's 220-character contract and no DB uniqueness existed | Use `VARCHAR(220)`, preflight duplicates, add a unique index |
| V137 | V144 | Instagram preview image | Re-entry/partial schema would fail because `IF NOT EXISTS` was absent | Add guarded column creation |
| V138 | V145 | Floor-plan comparison/visual analysis | Additive entity-aligned schema; no released history | Resequence without destructive changes |
| V139 | V146 | Floor-plan insight dimension text | Additive entity-aligned schema; no released history | Resequence without destructive changes |

`git log` and `git ls-files` showed no committed or released V135-V139 file. These are still part of their owning uncommitted feature work and are not staged with Release 5A-M2. No `outOfOrder` configuration was introduced.

## B134+V140 schema equivalence

Two isolated PostgreSQL 16.14 databases were built:

1. B134 followed by V140 from exact commit `7eff72e3d2824290d86867b84b4efd6cfcc6b46c`.
2. Historical V1-V134 with the former callback from exact commit `e3b88b70928ba3512327bb72991dfa2e7076d248`, followed by V140 from `7eff72e`.

| Catalog inventory | B134+V140 | Historical V134+V140 | Difference |
|---|---:|---:|---:|
| Tables/views/sequences | 207 | 207 | 0 |
| Columns | 1,510 | 1,510 | 0 |
| Constraints | 371 | 371 | 0 structural |
| Indexes | 396 | 396 | 0 |

Schema-only dumps had equal line counts (9,147 each). Their differences were PostgreSQL-equivalent renderings of CHECK arrays, for example a text array built by per-element casts versus an array-level cast. Constraint names/types/validation flags, relations, columns, defaults, nullability, indexes, and object inventories match.

## Historical seed/reference audit

The historical path populated 22 tables; B134 is intentionally schema-only.

| Historical data | Rows | Source | Classification | V141 action |
|---|---:|---|---|---|
| Categories | 68 | V3-V5, V76, V97 | Canonical navigation/reference | Restore 68 by natural slug; retain required id 0 only |
| Cities | 12 | callback, V65 | 11 canonical NCR cities plus 1 local-only fake city | Restore 11; exclude `Local Staging City` |
| Content versions | 5 | V23, V77, V109 | Cache/version reference | Restore with conflict-safe inserts |
| Global home sections | 7 | V97, V99, V100, V102, V103, V122/V123 | Core home configuration | Restore missing natural `(category,type)` rows |
| Global promo slot | 1 | V129 | Core home configuration | Restore missing HOME/HERO slot |
| Dashboard field help | 83 | V71, V72, V101 | Admin help copy, not a public runtime prerequisite | Audited; do not replay in V141; existing DBs retain it |
| App content pages | 3 | V61 | Placeholder legal/about copy | Exclude: placeholder policy text is unsafe to publish |
| App settings | 5 | V61 | Placeholder store/contact values | Exclude: contains generic URLs/example contact values |
| Businesses | 10 | V6 | Explicit dummy data | Exclude |
| Promo banners | 6 | V7, V105 | Dummy/sample media | Exclude |
| Featured carousel | 3 | V38 | Demo carousel | Exclude |
| Brands/collaborations | 4/4 | V76, V131 | Branded showcase/demo content | Exclude from automatic bootstrap |
| Company/projects/stats/links | 5/2/3/4 | V76 | Showcase content with external media | Exclude from automatic bootstrap |
| Circle/stamp rules | 69/18 | V52, V54 | Files explicitly identify sample/dummy values | Exclude pending domain-owner approval |
| Interior cost/add-ons | 13,152/144 | V57, V63 | Sample matrix tied to fixed company IDs | Exclude; fixed IDs are not safe baseline data |
| User | 1 | callback/historical local bootstrap | Local-only synthetic prerequisite | Exclude |
| Screen content | 1 | V108-era content | Environment-owned presentation content | Exclude |

V5 also encoded off-by-one fixed parent IDs. Historical rows consequently attach repair, flooring, moving, utility, sanitary, and construction children to the wrong parent categories. V141 uses parent slugs, giving fresh databases the intended hierarchy, while it deliberately does not rewrite existing production category relationships.

## V141 behavior and safety

`V141__restore_canonical_baseline_reference_data.sql` is insert-only. It:

- fails clearly if category id 0 or `sfs-home-all` is occupied inconsistently;
- uses unique/natural keys for all other reference rows;
- never updates or deletes existing application data;
- does not restore fake users, fixed-ID companies, demo brands, dummy providers, placeholder legal text, or sample financial matrices;
- is safe on both a fresh B134 database and an existing historical database.

## pg_trgm permission contract

PostgreSQL 16.14 supplied `pg_trgm` 1.6. Both comparison databases contained the extension owned by `sfs_app` and four expected GIN trigram indexes.

A PostgreSQL 16 Testcontainers permission test proves that a role with only `CONNECT` plus `USAGE, CREATE` on schema `public` cannot execute B134's `CREATE EXTENSION` (`SQLSTATE 42501`). If a different owner preinstalls the extension, that restricted role also cannot execute B134's `COMMENT ON EXTENSION` (`42501`). Because B134 is an already-applied immutable migration, a fresh deployment must use a migration role that owns/created `pg_trgm` (normally the database owner) or must provision the extension under that migration role before Flyway. This prerequisite must be checked on AWS staging before startup.

## V140 lock and rewrite measurement

The isolated historical audit database was expanded to 100,010 `business` rows (24 MiB) and 10,012 `city` rows (3.3 MiB), with all seven target columns converted to representative legacy numeric types.

- The seven V140 conversions completed in 2.78 seconds on local Docker/PostgreSQL 16.
- An active read transaction blocked the first `ALTER COLUMN TYPE`.
- With `lock_timeout = 1s`, the ALTER failed after 1.15 seconds with `canceling statement due to lock timeout`.
- PostgreSQL requires `ACCESS EXCLUSIVE` for each conversion, and V140 rewrites `business` three times and `city` three times when all legacy types are present.

This is acceptable for AWS staging validation, but it is not evidence for an uncoordinated production rollout. Production needs real row/size counts, a quiet deployment window, blocked-session monitoring, and an explicit lock-timeout/abort plan.

## Historical anomalies retained

- Committed `V12_loginHistory.sql` violates Flyway naming and is reported as one ignored SQL file on every validate/migrate. It remains immutable; B134/V140 provide `login_history`.
- `V44__create_project_meter_detail_tables.sql.sql` is unusual but accepted and already applied.
- Historical V5 parent assignments remain incorrect on existing databases; a separate data-owner-approved correction is required.
- Dashboard help copy, legal/contact content, calculator matrices, and branded content require explicit environment provisioning if a fresh environment needs them.

## AWS staging preconditions

Before AWS staging, verify no database has applied any V135-V139 file, deploy a clean commit containing V140 then V141, use a migration role satisfying the `pg_trgm` ownership contract, capture target row/table sizes, and run Flyway validate plus functional smoke flows. Do not enable Flyway out-of-order and do not deploy the uncommitted V142-V146 feature work as part of 5A-M2.
