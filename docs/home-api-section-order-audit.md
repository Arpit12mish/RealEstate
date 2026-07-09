# Home API Section Order — Current-State Audit

## 1. Endpoint

`GET /api/public/home`

Query params: `cityId`, `categoryId`, `builderId`, `v` (clientVersion), `lat`, `lng`, `deviceCity`, `accuracyMeters`.

## 2. Controller

[`HomePublicController`](../src/main/java/com/brandPitara/sfs/home/controller/publicapi/HomePublicController.java) — delegates directly to `HomeFeedService.getHome(...)`. No ordering logic lives here.

(There is also an authenticated `HomeController` under `home/controller/HomeController.java`, but the public feed consumed by the mobile app is `HomePublicController`.)

## 3. Service/composer building `sections`

[`HomeFeedServiceImpl`](../src/main/java/com/brandPitara/sfs/home/service/impl/HomeFeedServiceImpl.java), method `getHome(HomeFeedRequest)`.

## 4. Section type enum

[`HomeSectionType`](../src/main/java/com/brandPitara/sfs/home/enums/HomeSectionType.java) — plain enum, no ordinal-based ordering is relied upon anywhere in the ordering logic (good — ordinal order is declaration order in the file, which is unrelated to home feed order).

`QUICK_SQUARE` **does not exist** as a value today (see item 11).

## 5. Current section generation order — how it actually works today

Order is **database-driven**, not hardcoded and not incidental loader-registration order:

1. `home_section_config` rows are fetched for the resolved `homeCategoryId` via
   `findByHomeCategory_IdAndEnabledTrueOrderBySortOrderAscIdAsc(...)` — i.e. ordered by the `sort_order` column (ties broken by `id`).
2. For each config row, the loader whose `supports()` matches `cfg.getSectionType()` is invoked (loaders are indexed into an `EnumMap` first, so **Spring bean injection order has zero effect on output order** — this part is already safe).
3. Empty sections are dropped (`shouldIncludeSection`).
4. Promo banners (`PROMO_BANNERS`, key `HERO`/`MID`) are injected via `PromoBannerInjector.inject(...)`, which inserts each banner at an explicit anchor position (`insertAfterSectionType` + `positionIndex`; null anchor = insert at index 0). This is rule-driven, not part of the section list's own sort.
5. **After** banner injection, an ad-hoc patch — `orderLocationAwareHomeSections(...)` — forcibly removes `NEARBY_LISTINGS` and `TRENDING_CITIES` from wherever they landed and reinserts them immediately after `PROJECT_ANALYTICS`. Every other section type (`COMPARE_PROPERTIES`, `BUILDER_CREDIBILITY_CARDS`, `CONNECTED_BRANDS`, `SMART_CALCULATORS`, `INSTAGRAM_REELS`, `ARCHITECTS`, `DESIGNERS`, `TOP_PROJECTS`, `TOP_BUILDERS`, ...) is left exactly wherever the DB `sort_order` (as perturbed by step 4's insertions) happened to put it.

**This is the bug.** Only 2 of the 11 desired sections have a code-enforced position; the rest depend entirely on `sort_order` values in `home_section_config`, which:
- are not fully version-controlled (see item 6 below — several rows were never seeded via tracked Flyway migrations),
- can drift independently per environment/category,
- were assigned incrementally as features shipped (25, 28, 29, 30, 32, 35, 38, ...) with no single source of truth for the *intended* final order.

## 6. Is ordering hardcoded, DB-driven, or loader-assembled?

**DB-driven** (via `home_section_config.sort_order`), with one hardcoded patch (`orderLocationAwareHomeSections`) bolted on top. Loader execution order is irrelevant (already keyed by `EnumMap`).

Traceable `sort_order` values from tracked migrations (`home_category_id = 0`, the global/"All Home" feed):

| section_type | sort_order | migration |
|---|---:|---|
| SMART_CALCULATORS | 25 | V99 |
| INSTAGRAM_REELS | 28 | V102 |
| NEARBY_LISTINGS | 29 | V100 |
| TRENDING_CITIES | 30 | V97 |
| COMPARE_PROPERTIES | 32 | V103 |
| BUILDER_CREDIBILITY_CARDS | 35 | V99 |
| GENERIC_CARDS ("Top Builders", per V122 comment) | 37 | pre-existing, not tracked |
| CONNECTED_BRANDS (was TOP_BRANDS) | 38 | V122 / renamed V123 |

**`PROJECT_ANALYTICS`, `TOP_PROJECTS`, `ARCHITECTS`, `DESIGNERS`, `TOP_BUILDERS`, and the `PROMO_BANNERS` HERO/MID rules have no tracked migration** — `V32__home_section_config.sql` only creates the table; every row for those types was inserted directly against a live database outside of version control. Their `sort_order` values are therefore unknown/unverifiable from this repo and can differ across environments. This alone is a strong argument for not trusting `sort_order` as the sole source of ordering truth going forward.

## 7. Loader responsible for each desired section

| Desired section | HomeSectionType | Loader |
|---|---|---|
| Trending Properties | `PROJECT_ANALYTICS` (see item 9) | `ProjectAnalyticsSectionLoader` |
| Recommended Projects / Nearby Listings | `NEARBY_LISTINGS` | `NearbyListingsSectionLoader` |
| Compare Properties | `COMPARE_PROPERTIES` | `ComparePropertiesSectionLoader` |
| Builder Credibility | `BUILDER_CREDIBILITY_CARDS` | `BuilderCredibilityCardsSectionLoader` |
| Connected Brands | `CONNECTED_BRANDS` | `ConnectedBrandsSectionLoader` |
| Trending Cities | `TRENDING_CITIES` | `TrendingCitiesSectionLoader` |
| Smart Calculators | `SMART_CALCULATORS` | `SmartCalculatorsSectionLoader` |
| Instagram Reels | `INSTAGRAM_REELS` | `InstagramReelsSectionLoader` |
| Quick Square | `QUICK_SQUARE` | **none — does not exist** (see item 11) |
| Architects | `ARCHITECTS` | `ArchitectsSectionLoader` |
| Interior Designers | `DESIGNERS` | `DesignersSectionLoader` |

All 10 existing loaders are correctly wired (`supports()` returns the matching enum, registered as `@Component`, picked up by the `EnumMap` in `HomeFeedServiceImpl`). Nothing here needs to be built — only ordering needs fixing.

## 8. Duplicate `PROMO_BANNERS` HERO?

Not a true duplicate. `PromoBannerInjector` enforces `maxInsert = 2` and a `usedSlots` set that blocks a second insertion of the same normalized slot key, so at most one `HERO` and one `MID` banner section can be injected per request. If two `HERO`-keyed rows ever appeared in the response, the cause would be upstream — either two active `PromoBannerSlotConfigEntity` rows with different-cased/whitespace-padded `slotKey` values both resolving to `"HERO"` after `normalize()` in the *same* call (not possible, `usedSlots` is checked pre-normalization-collapse) — in practice this isn't reachable from current code. No fix needed here.

## 9. Do `TOP_PROJECTS` and `PROJECT_ANALYTICS` represent the same thing?

Functionally, yes for feed purposes — both render lists of project cards on the home screen — but they pull from different sources and rankings:

- **`PROJECT_ANALYTICS`** (`ProjectAnalyticsSectionLoader`) → `ProjectMeterService.publicListMeterCards(...)`, sorted by `priority, id desc`. Cards (`ProjectMeterCardResponse`) carry construction-progress %, appreciation %, timeline/delay status — i.e. genuine "trending/momentum" signal. Its configured title in the current DB is **"Square Meter"** (the internal feature/product name for this analytics tool), not "Trending Properties" — but the *content* is exactly what "Trending Properties" is supposed to show.
- **`TOP_PROJECTS`** (`TopProjectsSectionLoader`) → plain `ProjectRepository` query for published/active projects, sorted by `priority, id desc`, mapped through the generic `ProjectCardMapper`. No analytics/trending signal — just a static "top" list.

**Conclusion:** `PROJECT_ANALYTICS` is the correct backend section for "Trending Properties" (position 1). `TOP_PROJECTS` is a legacy/overlapping section that duplicates the same visual slot (project cards) without adding anything `PROJECT_ANALYTICS` doesn't already cover.

## 10. Is `TOP_BUILDERS` still needed on home?

`TOP_BUILDERS` (`TopBuildersSectionLoader`) renders builder cards ranked by `priority`. `BUILDER_CREDIBILITY_CARDS` (position 4 in the desired order) already covers "Builder Credibility" with an evidence-backed presentation ("Trusted Builders" / "Evidence-backed builder reliability" per its V99 seed). `TOP_BUILDERS` is not in the desired 11-section list — it appears to be superseded by `BUILDER_CREDIBILITY_CARDS`, the same pattern as `TOP_BRANDS` → `CONNECTED_BRANDS` (V122/V123).

**Decision for this task:** per the constraint "do not remove existing sections unless explicitly required," `TOP_BUILDERS` (and `TOP_PROJECTS`) are **not deleted**. They are excluded from the canonical 11-slot order and fall through to the "unknown type" bucket, which the sort keeps at the end of the feed, in their existing relative order. This preserves current API surface/shape while no longer letting them intrude on the desired ordering. Actually disabling them (if product confirms they're dead) is a one-row `enabled = false` update to `home_section_config`, not a code change, and is left as a follow-up decision for product/backend, not bundled into this ordering fix.

## 11. Does `QUICK_SQUARE` exist?

**No.** There is no `HomeSectionType.QUICK_SQUARE` enum constant, no `HomeSectionLoader` implementation, and no `home_section_config` row for it anywhere in the codebase or tracked migrations. It is a **net-new section that must be built** (entity/loader/migration/DB row) — out of scope for an ordering-only fix.

To keep the fix forward-compatible without scope creep, the canonical order map (see Phase 2) includes a `"QUICK_SQUARE"` entry keyed by string name even though no section currently produces that type. This costs nothing today (never matches any live section) and means that the day `QUICK_SQUARE` is implemented, it will automatically land in the correct position (between Instagram Reels and Architects) with zero further ordering changes.

## Summary of decisions for Phase 2

- Canonical order is enforced by a stable sort keyed on `section.getType().name()`, applied to the **content section list before promo banner injection** — so `PromoBannerInjector`'s anchor-based insertion (`insertAfterSectionType`) keeps working against a list that's already in the correct final order, instead of running before the ad-hoc reorder and getting shuffled afterward (today's bug for any banner anchored to a section other than `NEARBY_LISTINGS`/`TRENDING_CITIES`).
- `PROMO_BANNERS` is intentionally **not** in the order map — it's positioned by explicit slot-rule anchors, not by the canonical content order, matching current/expected behavior (HERO first, MID at its configured anchor).
- Unknown/legacy types (`TOP_PROJECTS`, `TOP_BUILDERS`, `GENERIC_CARDS`, `COMPANIES`, `TOP_CATEGORIES`, `TOP_DISTRIBUTORS`, `PROJECT_PLAN`, `ARCHITECTS_AND_DESIGNERS`, `FEATURED_CAROUSEL`, `BUILDER_HERO`, `BUILDER_STATS`, `BUILDER_STORY`, `BUILDER_CREDIBILITY`) sort after all 11 canonical sections, in their existing relative order — nothing is dropped, response shape is unchanged, only position moves.
