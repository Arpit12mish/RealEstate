# Release 2 home-feed reconnaissance

## Scope and environment

The measurements use PostgreSQL 16 through Testcontainers, OSIV disabled, every production home-section type enabled, eight builders/projects, and oversized curated/category/plan/banner fixtures. Hikari is constrained to three connections with a one-second acquisition timeout. Production configuration remains unchanged at a maximum pool size of 10.

The inspected path includes `HomeFeedServiceImpl`, all 19 production `HomeSectionLoader` implementations, their delegated services and repositories, project/builder/company/category/promo DTO mapping, all builder-credibility repositories, and all project-comparison repositories and section builders.

No external HTTP call is reachable from the home endpoint. Instagram, nearby, favourites, meter, brand, city, banner, and credibility paths used by home are database/local mapping operations. Media URLs are data only.

## Baseline

Before Release 2 changes, a fully configured home call executed 56 SQL statements in 214 ms and produced 23,609 bytes. The broad home transaction held the connection across composition; the one-millisecond sampler observed it active for 170 ms, with peak active 1 and peak pending 0.

Per-section baseline SQL counts were:

| Section | SQL |
| --- | ---: |
| TOP_PROJECTS | 4 |
| TOP_BUILDERS | 1 |
| CONNECTED_BRANDS | 5 |
| TOP_CATEGORIES | 1 |
| ARCHITECTS_AND_DESIGNERS | 3 |
| ARCHITECTS | 3 |
| DESIGNERS | 3 |
| TOP_DISTRIBUTORS | 2 |
| PROJECT_PLAN | 2 |
| PROJECT_ANALYTICS | 5 |
| NEARBY_LISTINGS | 2 |
| INSTAGRAM_REELS | 2 |
| TRENDING_CITIES | 1 |
| SMART_CALCULATORS | 0 |
| COMPANIES | 2 |
| FEATURED_CAROUSEL | 1 |
| GENERIC_CARDS | 3 |
| COMPARE_PROPERTIES | 0 |
| BUILDER_CREDIBILITY_CARDS | 9 |

Four-project comparison executed 31 SQL statements. Four builder credibility cards executed 9 statements. Direct serialization after the home service returned failed with `LazyInitializationException` on `ProjectEntity.propertyTypes` when OSIV was disabled; the baseline payload therefore had to be measured inside a test transaction.

## Root causes

- Both `HomeFeedServiceImpl.getHome` entry points owned a read-only transaction, so config lookup, sequential section composition, pure computation, error handling, banner injection, and logging shared one connection-holding scope.
- Credibility cards ran one highlight `exists` statement per builder.
- Project comparison invoked the complete credibility query group once per unique builder.
- builders, curated home items, categories, project plans, promo banners, and company types had fetch-then-truncate paths instead of database bounds.
- architect/designer cards loaded every project for the selected companies and retained the first project in Java.
- `ProjectCardMapper` exposed the entity's lazy property-type collection in the response DTO.

## Release 2 result

The broad home transaction is removed. Each database-backed loader owns a short `REQUIRED`, read-only transaction; pure comparison/calculator loaders own none. Loaders remain sequential. Builder highlight availability is one batched query, and project comparison calls one batched credibility operation.

Database pagination now enforces configured limits with deterministic ordering. The architect/designer company query applies normalized type filtering and the limit in PostgreSQL; a correlated query returns only the top public project per selected company. No index or Flyway migration was added.

Post-change measurements:

- Fully configured home: 54 SQL, 23,609 bytes, 358 ms in the first post-change measurement; active sampled for 234 ms cumulatively across short transactions, peak active 1, peak pending 0, and active/pending returned to zero.
- Per-section SQL matches the baseline except `TOP_PROJECTS` is 5 because property types are initialized and copied inside its short transaction, and `BUILDER_CREDIBILITY_CARDS` is 6 because three per-builder existence statements were removed.
- Project comparison: 16 SQL for each of 2, 3, and 4 projects (4-project baseline 31).
- Credibility cards: 6 SQL for both 1 and 4 builders (4-builder baseline 9).
- Ten concurrent clients with Hikari size 3: peak active 3, peak pending 9, zero acquisition timeouts; active and pending returned to zero after completion.
- A deliberately blocked pure home section observed no active transaction, active connections 0, and pending 0 while blocked.
- OSIV-disabled serialization succeeds after the home service returns.

The measured development/Testcontainers timings are regression evidence, not a production capacity rating. Release 5 staging k6 work remains required before setting an operating limit.
