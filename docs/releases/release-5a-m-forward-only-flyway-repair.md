# Release 5A-M: forward-only Flyway bootstrap repair

## Root cause

The committed V1–V134 history cannot bootstrap an empty database by itself. V6 inserts a business that references `city.id = 1`, although no preceding migration creates that row. Later migrations also alter or seed objects that were never created by an earlier valid versioned migration. In addition, `V12_loginHistory.sql` does not follow Flyway's versioned-migration naming convention and is ignored. The Release 5A-P local profile concealed these defects with `db/local-staging/beforeEachMigrate.sql`.

Applied migrations are immutable, so changing V1–V134 would invalidate production checksums. A repair at V140 alone also cannot help an empty database because migration stops at V6. Release 5A-M therefore uses Flyway's native baseline-migration mechanism: a schema-only `B134` migration for genuinely empty databases and a guarded forward `V140` migration for databases that already have V1–V134 history.

## Migration inventory

| Version | File or group | Committed before 5A-M? | Applied in production-shaped DB? | Creates/changes | Depends on | Risk |
|---|---|---:|---:|---|---|---|
| V1–V11 | `V1__initial_schema.sql` … `V11__business_event_indexes.sql` | Yes | Yes | Initial city/category/business, seed data, banners/events | V6 assumes city 1 | Clean bootstrap stops at V6 |
| V12 | `V12_loginHistory.sql` | Yes | No | Intended login history | Invalid filename | Silently ignored by Flyway |
| V13–V31 | Favorites, provider, requests, content, brand/distributor | Yes | Yes | Core service and brand schema | `users` is assumed before V107 | Callback supplied missing prerequisite |
| V32–V41 | Home configuration and analytics | Yes | Yes | Home sections/carousels/analytics | V40 assumes `home_project_analytics` | Missing creation ordering |
| V42–V65 | Company projects, project details, calculators, app content, OTP, cities | Yes | Yes | Project/company/calculator tables and seed data | Company/project foundation objects | Historical objects were absent |
| V66–V95 | Dashboard/review, builder improvement, public review, project intelligence | Yes | Yes | Dashboard, moderation and project intelligence | Builder/project/company foundation | Foundation/schema drift |
| V96–V107 | Search indexes, home features, Instagram and auth alignment | Yes | Yes | Search/home/auth | `users`, guest/login history | Auth tables arrive too late |
| V108–V134 | Screen content, builder highlights, brand/company CMS, public review target | Yes | Yes | Current committed schema | Earlier repaired objects | Type/column drift remained |
| B134 | `B134__sfs_schema_baseline.sql` | New | No | Complete validated schema at the V134 boundary | Empty schema, PostgreSQL 16 | Runs only for an empty database |
| V135–V139 | Five untracked user migrations | No | No evidence | Promo/company/Instagram/floor-plan work | User work in progress | Excluded from this release |
| V140 | `V140__repair_historical_schema_drift.sql` | New | No | Guarded object, column, type, FK and index repair | Existing V134 schema or B134 | Additive except validated numeric conversions |

There are 133 successful rows in the production-shaped local history: V1–V11 and V13–V134. There are no committed duplicate versions or repeatable migrations. `V44__create_project_meter_detail_tables.sql.sql` is oddly named but is a valid committed migration. V12 remains deliberately untouched.

## Callback audit

| Callback repair | Object | Historical migration | Failure or drift | Expected schema | Existing-DB risk | Forward handling |
|---|---|---|---|---|---|---|
| Insert city ID 1 | `city` | V6, V38 | Required FK seed absent | No synthetic runtime dependency | Fixed-ID collision/data invention | B134 avoids historical seed chain; V140 does not insert placeholder data |
| Create analytics prerequisite | `home_project_analytics` | V40 | ALTER precedes CREATE | Entity-compatible columns/FK/index | Existing rows must survive | B134 creates final table; V140 creates/adds only when absent |
| Add timestamp | `provider_media.updated_at` | V19/V22 | Entity expects missing column | `timestamptz`, non-null, `now()` | Existing nulls | V140 backfills from `created_at`, then adds default/non-null |
| Add grouping | `home_section_item.group_key` | V32/V41 | Naming/schema drift | nullable `varchar(50)` | None | `ADD COLUMN IF NOT EXISTS` |
| Add logo | `distributor.logo_url` | V26 | Entity/schema drift | nullable `text` | None | `ADD COLUMN IF NOT EXISTS` |
| Numeric mapping repair | business/city coordinates, rating/growth | V1/V18/V97 | Hibernate expects Java `Double` | `double precision` | Conversion/locking | Fail unless source is numeric; explicit cast |
| Aspect-ratio repair | `app_screen_content.aspect_ratio` | V108 | Hibernate expects `Double` | `double precision` | Conversion/locking | Same guarded numeric conversion |
| Add project fields | `company_project` | V42 | Entity expects missing fields | nullable text/varchar columns | None | Additive V140 columns |
| Create company foundation and placeholders | company/stat/certificate/award | V76/V132/V133 | Seed/ALTER precedes valid foundation | Final entity-compatible tables | Placeholder IDs can collide | B134 provides schema only; V140 never creates fake companies |
| Create project foundation | builder/project/property types/media | V77/V83/V92 and project features | Tables assumed before creation | Final FK/index shape | Orphan/duplicate ambiguity | Guarded tables/FKs/indexes; invalid data causes explicit FK failure |
| Create auth foundation | users/login history/guest sessions | invalid V12, V13, V107 | Users referenced before valid creation | Current auth schema | Authentication data is sensitive | Schema-only baseline; guarded V140; no auth row mutation |
| Create feed foundation | feed config/item | home/feed loaders | Missing base tables | Current feed schema | Existing feed rows | Guarded creation and indexes |
| Create builder | builder/project migrations | Missing foundation | Current builder schema | Existing builder rows | Guarded creation only |

Classification: missing prerequisite table and column, incorrect ordering, invalid naming, type drift, constraint/index drift, entity/schema drift, and local-only placeholder data. No callback statement was accepted without comparison to migration SQL, PostgreSQL metadata, entity mappings, and dependent repository/service behavior.

## New forward migrations

### `B134__sfs_schema_baseline.sql`

This is a PostgreSQL 16 schema-only dump from the exact baseline application after V1–V134, callback execution, successful Flyway history, and successful Hibernate validation. It contains no rows or secrets. Flyway selects it only when the schema is empty; an existing versioned history continues to use that history.

### `V140__repair_historical_schema_drift.sql`

This migration supports absent, correct, and partially shaped objects. It creates missing foundation tables, adds missing columns, repairs known numeric types, restores the company-city FK, and creates expected indexes. It does not delete application rows, seed fixed identifiers, use `CASCADE`, or suppress corrupt/incompatible shapes.

## Data-preservation and conversion strategy

- Existing versioned databases never execute B134.
- V140 uses `CREATE TABLE IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS`, conditional constraints, and `CREATE INDEX IF NOT EXISTS`.
- `provider_media.updated_at` is filled from each row's `created_at` before enforcing non-nullability.
- Numeric-to-double repairs first verify the source type is numeric, then use an explicit PostgreSQL cast. An unexpected type aborts with an object-specific error.
- No tables, application columns, rows, constraints, indexes, or types are dropped.
- No blanket `CASCADE`, `TRUNCATE`, or unqualified delete exists.
- Invalid foreign-key data is not guessed around: PostgreSQL rejects constraint creation and surfaces the defect.
- B134 intentionally excludes the callback's synthetic city and company rows. Reference/content data must be provisioned through application-owned workflows or separately reviewed forward seed migrations.

## Entity and schema reconciliation

| Entity field/object | Expected DB result | Migration result | Match? | Action |
|---|---|---|---:|---|
| `User` / `users` | bigint identity, phone/role/onboarding/security columns | B134 exact schema; V140 foundation guard | Yes | None |
| `LoginHistory.user` | nullable bigint FK so history may survive profile deletion workflow | V140 drops only NOT NULL; preserves FK/data | Yes | Repair nullability |
| `GuestSession` | unique installation ID and nullable linked user | B134/V140 matching table | Yes | None |
| `CompanyEntity.city` | nullable bigint FK to city | B134 FK; conditional V140 FK | Yes | Restore FK if absent |
| company stats/certificates/awards | bigint IDs/FKs, visibility and audit columns | B134 exact; V140 additive guards | Yes | None |
| builder/project/media/property types | bigint identities/FKs and current indexes | B134 exact; V140 guarded foundations | Yes | None |
| provider media `updatedAt` | non-null `timestamptz` | backfill/default/non-null | Yes | Repair column |
| home/feed group keys | nullable `varchar(50)` | additive columns | Yes | Repair columns |
| business/city numeric Java `Double` fields | `double precision` | guarded explicit conversion | Yes | Repair types |
| app screen aspect ratio | `double precision` | guarded explicit conversion | Yes | Repair type |
| company project legacy fields | nullable mapped fields | additive V140 fields | Yes | Repair columns |

Hibernate remains `ddl-auto=validate`; OSIV remains disabled. Entity classes were not changed to accommodate broken historical DDL.

## Constraint and index strategy

The baseline captures all V134 primary keys, foreign keys, unique constraints, checks, sequences, and indexes. V140 adds only the specific missing company-city FK and known supporting indexes. Constraint existence is checked by relation, type, and constrained column rather than name alone. Index creation is name-guarded and non-destructive. Existing duplicate/orphaned data is never silently removed to force a constraint.

## Callback removal

`src/main/resources/db/local-staging/beforeEachMigrate.sql` is deleted. The local-staging profile loads only `classpath:db/migration`. Structure tests fail if the callback returns or if the callback location is restored.

## Test coverage

- PostgreSQL 16 empty-schema baseline and migration history verification
- V134-boundary upgrade with representative city, category, user, business, company, builder, project, media, property type, stat, and login-history rows
- Missing-column and legacy numeric-type repairs with row preservation
- Already-correct schema re-entry
- PK, FK, index and nullability assertions
- Flyway `validate`
- Spring Boot context startup with Hibernate `validate`
- Callback absence and local profile configuration contract

The integration tests use Testcontainers `postgres:16-alpine`; H2 is not used for migration correctness.

## Deployment order

1. Back up and record the target database's `flyway_schema_history` and PostgreSQL metadata.
2. Confirm the target has successful versioned history through V134 and no version collision at V140.
3. Deploy the application containing B134 and V140. Existing databases apply V140 only.
4. Monitor migration locks and duration; do not run concurrent application rollouts against the same schema.
5. Run Flyway validate, Hibernate validation/startup, readiness, and representative read/auth flows.
6. For a new empty database, verify history contains baseline version 134 followed by V140 and no callback rows.

## Rollback considerations

Flyway/schema rollback is forward-only. The application deployment may be rolled back after V140 because its changes are additive and historical application mappings already expect the repaired objects. Double-precision conversions are not automatically reversed; restore from backup only if post-deployment validation exposes an unforeseen semantic issue. Never delete successful Flyway history rows.

## Production verification checklist

- Confirm backup and maintenance window.
- Confirm PostgreSQL version and extension privileges (`pg_trgm`).
- Confirm V140 is not already occupied and validate checksums V1–V134.
- Capture affected-table row counts and invalid FK probes before migration.
- Apply once; verify no failed/duplicate history rows and latest version V140.
- Start with `ddl-auto=validate` and OSIV disabled.
- Verify readiness, Hikari maximum 10, pending/timeouts zero, and normal auth/read paths.
- Confirm no `beforeEachMigrate` callback appears in logs or packaged resources.

## Remaining schema risks

- Release 5A-M2 audits all skipped historical data and adds V141 for canonical categories, cities, content versions, home sections, and the HOME/HERO slot. Placeholder, demo, branded, and unapproved calculator data remain intentionally excluded.
- Numeric type conversion can take an access-exclusive lock proportional to affected table size; production row counts and a maintenance window remain necessary.
- `CREATE EXTENSION pg_trgm` and B134's extension comment require an extension-owning migration role on a new database; Release 5A-M2 includes a PostgreSQL 16 permission-contract test.
- V12's invalid historical filename and V44's unusual double extension remain immutable technical debt.
- The unreleased V135-V139 files were reviewed and resequenced to V142-V146 after immutable V140/V141. They remain unstaged feature work owned by their original workstream.
