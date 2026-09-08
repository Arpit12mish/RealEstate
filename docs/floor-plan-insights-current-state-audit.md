# Floor Plan Insights — Current-State Backend Audit

Scope: this document answers whether the existing **Floor Plan Intelligence v1** backend (`project_floor_plan_room_dimension`, `project_floor_plan_insight`, `ProjectFloorPlanInsightPublicController`) can support the new **Floor Plan Insights bottom sheet** (Visual Analysis + Space Comparison sections) shown in the product screenshots.

This audit is read-only. No backend files were modified. For full schema/endpoint reference material that already exists, see [`floor-plan-current-state.md`](./floor-plan-current-state.md) and [`floor-plan-improvement-plan.md`](./floor-plan-improvement-plan.md) — this doc does not repeat that material wholesale, it focuses on the new gap: **can today's insight model represent "select a room chip → see that room's dimension/area/average/%diff/summary" plus a "Visual Analysis" media+tags block?**

## Files audited (full reads)

`ProjectFloorPlanInsightPublicController`, `ProjectFloorPlanInsightServiceImpl`, `ProjectFloorPlanInsightDetailResponse`, `FloorPlanRoomDimensionResponse`, `FloorPlanInsightResponse`, `FloorPlanInsightUpsertRequest`, `FloorPlanRoomDimensionUpsertRequest`, `ProjectFloorPlanRoomDimensionEntity`, `ProjectFloorPlanInsightEntity`, `FloorPlanRoomType`, `FloorPlanInsightType`, `InsightComparisonResult`, `ProjectFloorPlanResponse`, `ProjectDetailComposerImpl`, `DashboardProjectFloorPlanInsightController`, `DashboardProjectFloorPlanRoomDimensionController`, `DashboardMediaUploadType`, `DashboardMediaPresignServiceImpl`, `V84__floor_plan_intelligence_v1.sql`, `V85__floor_plan_intelligence_v1_fixes.sql`, plus every test file that references floor plans/insights/room dimensions.

## 1. What the public insight endpoint returns

`GET /api/projects/{projectId}/floor-plans/{floorPlanId}/insights` → `ProjectFloorPlanInsightDetailResponse`:

- Floor-plan header fields (title, imageUrl, unit config, price, areas, bedrooms/bathrooms/balconies, facing, tower, floor range, keyPlanImageUrl).
- `rooms: List<FloorPlanRoomDimensionResponse>` — flat list.
- `insights: List<FloorPlanInsightResponse>` — flat list.

`ProjectFloorPlanInsightServiceImpl.java:134-146` (`publicGetDetail`): project must pass `assertPubliclyVisible`; floor plan must belong to the project and be `active=true`; rooms filtered `active=true, deleted=false`; insights filtered `publicVisible=true, active=true, deleted=false`.

## 2. Room chips — supported

`rooms[].roomType` + `roomTypeLabel` give exactly the chip set needed. `FloorPlanRoomType` enum (`FloorPlanRoomType.java:3-19`) already includes `MASTER_BEDROOM`, `DINING_ROOM`, `KITCHEN`, plus `LIVING_ROOM`, `BEDROOM` (generic — used for "Child Bedroom" via `label` override), `BATHROOM`, `BALCONY`, `STUDY_ROOM`, `SERVANT_ROOM`, `UTILITY_ROOM`, `POOJA_ROOM`, `FOYER`, `STORE_ROOM`, `TERRACE`, `GARAGE`, `OTHER`.

## 3. Room dimensions — supported

`FloorPlanRoomDimensionResponse` has `lengthFt`, `widthFt`, `areaSqft` (`BigDecimal`) plus a pre-formatted `dimensionText` (e.g. `"12.1 ft x 10.6 ft"`) intended for exactly this display.

## 4. Comparison-with-average — partially supported, not room-keyed

The average/%diff machinery (`unitValue`, `benchmarkValue`, `unitLabel`, `differenceValue`, `differencePercent`, `chartLabelThisUnit`, `chartLabelAverage`, `comparisonResult`) lives entirely on `FloorPlanInsightResponse`/`insights[]`, **not** on `FloorPlanRoomDimensionResponse`/`rooms[]`. Rooms carry no average/benchmark/percent fields at all.

## 5. Selected-room comparison — not supported (the core gap)

`rooms` and `insights` are two **independent, unlinked flat arrays**. Neither `ProjectFloorPlanRoomDimensionEntity` nor `ProjectFloorPlanInsightEntity` has a foreign key to the other — both are sibling children of `ProjectFloorPlanEntity` only (confirmed against both `V84` and `V85` migrations: no `room_dimension_id` column on `project_floor_plan_insight`, no `insight_id` on `project_floor_plan_room_dimension`).

A client wanting "tap Master Bedroom chip → show its dimension + average + %diff + summary" has no reliable server-side join. The only path is string-matching `roomType.name()` against an `insightType` naming convention (`MASTER_BEDROOM` ↔ `MASTER_BEDROOM_SIZE`), which is fragile and incomplete — there is no `insightType` for `BATHROOM`, `STUDY_ROOM`, `SERVANT_ROOM`, `UTILITY_ROOM`, `POOJA_ROOM`, `FOYER`, `STORE_ROOM`, `TERRACE`, or `GARAGE`. **This is the single blocking gap for the Space Comparison section as designed.**

## 6. Visual Analysis title/description/media/tags — not supported

- Title/description-like text exists per-insight-row (`title`, `summary`, `detailedText`) but is designed for one benchmark metric, not a standalone narrative card.
- **No media field anywhere.** Neither `ProjectFloorPlanInsightEntity` nor its DTOs have `imageUrl`/`videoUrl`/`lottieUrl`/`mediaType`. The floor plan itself has `imageUrl`/`keyPlanImageUrl`, but those are plan renderings, not "visual analysis" illustrations.
- **No tags/chips concept.** Nothing resembling `List<{label,color,sortOrder}>` exists anywhere in the insight entity, DTO, or migrations.

## 7. `FloorPlanInsightType` coverage vs. requested tag set

Full enum (`FloorPlanInsightType.java:3-26`): `MASTER_BEDROOM_SIZE, CHILD_BEDROOM_SIZE, LIVING_ROOM_SIZE, DINING_ROOM_SIZE, KITCHEN_SIZE, BALCONY_SIZE, CARPET_AREA_EFFICIENCY, NATURAL_VENTILATION, CROSS_VENTILATION, NATURAL_LIGHT, VASTU_COMPLIANCE, PRIVACY_SCORE, FLOW_EFFICIENCY, SPACE_UTILIZATION, OTHER`.

| Requested tag | Match |
|---|---|
| Sun Lighting | Close — `NATURAL_LIGHT` |
| Air ventilation | Close — `NATURAL_VENTILATION`, `CROSS_VENTILATION` |
| Vastu analysis | Exact — `VASTU_COMPLIANCE` |
| Space comparison | Indirect — the `*_SIZE` values + `SPACE_UTILIZATION` |
| Layout movement | Approximate — `FLOW_EFFICIENCY` |
| Privacy | Exact — `PRIVACY_SCORE` |
| Tower analysis | **None** — no matching value |

This enum is a **benchmark-metric taxonomy** (one numeric comparison per row), not a **visual-analysis-category taxonomy** (title+description+media+color-chip bundle). It has conceptual overlap but wasn't designed for this UI.

## 8–9. Entity media / chart fields

`ProjectFloorPlanInsightEntity` (`.java:18-96`): **no media columns**. It **does** already carry the full chart/comparison numeric model (`unitValue`, `benchmarkValue`, `unitLabel`, `differenceValue`, `differencePercent`, `chartLabelThisUnit`, `chartLabelAverage`, `comparisonResult`) — this part is solid and reusable, it's just not room-keyed (see §5).

## 10. Room ↔ insight linkage

Only to the floor plan, not to each other. See §5.

## 11–12. Dashboard CRUD today

Both already exist and work:

- `DashboardProjectFloorPlanRoomDimensionController` — `/api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/rooms` (GET/POST/PUT/DELETE).
- `DashboardProjectFloorPlanInsightController` — `/api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/insights` (GET/POST/PUT/DELETE).

Both enforce `DashboardProjectOwnershipService.assertCurrentUserCanEditProject` on writes and log via `DashboardAuditAction.FLOOR_PLAN_UPDATED`. POST/PUT/DELETE on insights trigger `recalculateInsightsAvailable`.

## 13. Dashboard media upload for visual-analysis content

Not supported. `DashboardMediaUploadType` (`.java:3-49`) has no value scoped to an insight row or room row — the only floor-plan-related upload type is `FLOOR_PLAN_IMAGE`, which is floor-plan-scoped (writes to `ProjectFloorPlanEntity.imageUrl`/`keyPlanImageUrl`), not insight-scoped. No upload type supports video or Lottie JSON for this feature (video/Lottie upload types exist elsewhere — Instagram reels, home promo banners — but none apply here).

## 14. Mobile app calling the endpoint today

Yes — see [`floor-plan-insights-mobile-audit.md`](./floor-plan-insights-mobile-audit.md). `Mobile_SFS/SFS_App` already calls `fetchProjectFloorPlanInsights` against this exact endpoint, with a client-side demo-data fallback per [`floor-plan-unit-insights-demo-plan.md`](./floor-plan-unit-insights-demo-plan.md).

## 15. `insightsAvailable` / `insightsUrl` on `ProjectFloorPlanResponse`

- `insightsAvailable: boolean` — present, auto-synced by `recalculateInsightsAvailable` (true iff ≥1 `publicVisible+active+non-deleted` insight exists).
- `insightsUrl` — **does not exist on the Java response** (repo-wide grep: zero hits). Note: the mobile TypeScript type (`ProjectFloorPlanDto`/`ProjectFloorPlanResponse` in both mobile repos) already declares `insightsUrl?: string | null` — this was speculatively added per the proposal in `floor-plan-improvement-plan.md` §"Public/Mobile API Proposal" but never implemented backend-side. It is currently a dead field on the client that always resolves to `undefined`. Flag for cleanup regardless of this feature.

## 16. What backend changes are required

Ranked by what blocks the new UI vs. what's nice-to-have — full proposal in [`floor-plan-insights-api-contract-plan.md`](./floor-plan-insights-api-contract-plan.md):

1. **Blocking — room-keyed comparison data.** Add comparison fields directly to room rows (recommended: extend `project_floor_plan_room_dimension` with `average_area_sqft`, `comparison_context_label`, `difference_percent`, `summary_text`) rather than trying to join through `FloorPlanInsightType`. This sidesteps the enum-coverage gap in §5/§7 entirely and is the simplest correct fix.
2. **Blocking — Visual Analysis media + tags.** New small entity (one row per floor plan) for title/description/media, plus a child table for colored tag chips. New `DashboardMediaUploadType` value(s) for image/video/Lottie JSON media on this new entity.
3. **Non-blocking — `TOWER_ANALYSIS` and other future insight types.** Only needed when the "future sections" in the target spec (§4) are built; not needed for this bottom sheet's first release.
4. **Cleanup — `insightsUrl`.** Either implement it server-side or remove the dead mobile field; don't leave it declared-but-never-populated.
5. **Risk flag, independent of this feature:** there is **zero automated test coverage** for `ProjectFloorPlanInsightPublicController`, both dashboard insight/room controllers, both mappers, the `insightsAvailable` recalculation, or any repository query on these two tables (confirmed via full `src/test` scan — the only floor-plan-adjacent tests are `ProjectDetailComposerImplTest`, which covers BHK grouping only, and the unrelated `projectcompare` package tests, which share the word "Insight" in their class names but test a different feature). Any new work on this module should add tests as it goes rather than compounding an already-untested surface.

## Summary gap table

| UI need | Backend support today | Gap |
|---|---|---|
| Room chips | Yes (`rooms[].roomType`/`roomTypeLabel`) | None |
| Dimension text | Yes (`rooms[].dimensionText`) | None |
| Area (sqft) | Yes (`rooms[].areaSqft`) | None |
| Average area for comparison | Only on `insights[]`, not room-keyed | Needs room-level fields or FK join |
| "+13% from avg" / percent diff | Only on `insights[]`, not room-keyed | Same gap |
| Per-room summary sentence | Only on `insights[]`, not room-keyed | Same gap |
| Visual Analysis title/description | Usable via `insights[].title`/`summary`, semantically mismatched | Needs dedicated entity |
| Visual Analysis media (image/video/Lottie) | None | Full gap — new column(s) + new upload type |
| Colored tag chips | Enum values conceptually close, no chip/color/multi-tag model | Partial gap |
| "Tower analysis" tag | No matching enum value | Full gap (defer to future phase) |
