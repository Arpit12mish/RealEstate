# Floor Plan Insights — API Contract Plan

Goal: extend the existing public endpoint `GET /api/projects/{projectId}/floor-plans/{floorPlanId}/insights` to support the Visual Analysis + Space Comparison bottom sheet, **without** creating a duplicate endpoint or removing/renaming the current `rooms[]`/`insights[]` fields that `Mobile_SFS/SFS_App` already consumes in production.

Prerequisite reading: [`floor-plan-insights-current-state-audit.md`](./floor-plan-insights-current-state-audit.md) (gap analysis) and [`floor-plan-insights-target-spec.md`](./floor-plan-insights-target-spec.md) (UI requirements). This plan also reconciles with the `insightsUrl`/`demo`/`sourceLabel` proposal already sketched in [`floor-plan-improvement-plan.md`](./floor-plan-improvement-plan.md) — that plan proposed the same demo/source fields from a different angle (floor-plan grouping) and never shipped them; this plan makes them concrete.

## Design decision: don't reuse `FloorPlanInsightType` for Space Comparison

The current `insights[]`/`FloorPlanInsightType` model is a good fit for *floor-plan-level factor scores* (ventilation, Vastu, privacy — the "future sections" in the target spec). It is a poor fit for *per-room size comparisons*, because:

- It has no FK to a room row (the core gap — see current-state audit §5).
- Its enum coverage is incomplete for rooms without a `*_SIZE` value (bathroom, study, servant, utility, pooja, foyer, store, terrace, garage).
- Trying to force a join via naming convention (`MASTER_BEDROOM` ↔ `MASTER_BEDROOM_SIZE`) is fragile and doesn't scale to arbitrary room labels dashboard users type in.

**Decision: put Space Comparison fields directly on the room-dimension row.** A room's "average size in this locality" is a property of that room, authored once by a dashboard user, not a separate benchmark object that needs joining. This is simpler, always correctly linked, and works for every room type without enum changes.

Visual Analysis (media + tags) has no existing home at all — it needs one new small entity, kept in the same `project` package (not a new module), consistent with "do not create duplicate backend modules."

## Schema changes

### 1. Extend `project_floor_plan_room_dimension`

Migration `V138__floor_plan_room_comparison.sql`:

```sql
ALTER TABLE project_floor_plan_room_dimension
  ADD COLUMN IF NOT EXISTS average_area_sqft DECIMAL(10,2),
  ADD COLUMN IF NOT EXISTS comparison_context_label VARCHAR(120),
  ADD COLUMN IF NOT EXISTS difference_percent DECIMAL(6,2),
  ADD COLUMN IF NOT EXISTS comparison_summary TEXT,
  ADD COLUMN IF NOT EXISTS comparison_verified BOOLEAN NOT NULL DEFAULT FALSE;
```

- `average_area_sqft` — the benchmark figure (e.g. `149`).
- `comparison_context_label` — the locality/context named in the summary sentence (e.g. `"Varthur"`); kept separate from free text so it could later be templated or reused.
- `difference_percent` — signed percent (`+13.00` / `-8.00`); sign drives the badge color and "bigger"/"smaller" wording client-side.
- `comparison_summary` — the authored sentence (e.g. *"An average master bedroom in Varthur measures 149 sq ft. This unit's master bedroom is 13% bigger."*). Dashboard-authored free text rather than fully templated, matching how `FloorPlanInsightResponse.summary` already works today.
- `comparison_verified` — mirrors the existing `verified` pattern on insights; lets a reviewer flag comparison data as checked before it's trusted, without needing full `publicVisible`-style gating (comparison visibility is governed by "are these fields non-null", not a separate flag — see response shape below).

All new columns nullable/defaulted — a room row with none of them set behaves exactly as it does today (dimension + area only, no comparison block), satisfying the target spec's "graceful when comparison data isn't authored yet" requirement.

### 2. New entity: floor-plan visual analysis (one row per floor plan)

Migration (same file, `V138`):

```sql
CREATE TABLE IF NOT EXISTS project_floor_plan_visual_analysis (
  id BIGSERIAL PRIMARY KEY,
  floor_plan_id BIGINT NOT NULL REFERENCES project_floor_plan(id),
  title VARCHAR(160) NOT NULL DEFAULT 'Visual analysis of every important factor',
  description VARCHAR(500),
  media_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
  media_url TEXT,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_pfp_visual_analysis_floor_plan UNIQUE (floor_plan_id)
);

CREATE TABLE IF NOT EXISTS project_floor_plan_visual_analysis_tag (
  id BIGSERIAL PRIMARY KEY,
  visual_analysis_id BIGINT NOT NULL REFERENCES project_floor_plan_visual_analysis(id),
  label VARCHAR(60) NOT NULL,
  color VARCHAR(9) NOT NULL DEFAULT '#3B7DDD',
  sort_order INTEGER NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_pfp_visual_analysis_floor_plan ON project_floor_plan_visual_analysis(floor_plan_id);
CREATE INDEX IF NOT EXISTS idx_pfp_visual_analysis_tag_parent ON project_floor_plan_visual_analysis_tag(visual_analysis_id);
```

- `media_type` — `IMAGE | VIDEO | LOTTIE_JSON`, matches the mobile `SmartMedia` extension plan.
- One-to-one with floor plan (`UNIQUE(floor_plan_id)`) — the target spec has exactly one Visual Analysis block per sheet, not a list.
- Tags as a child table (not a JSON/CSV column) so dashboard CRUD, `sortOrder`, and per-tag `active` toggling work the same way every other list in this module already does (rooms, insights).

### 3. `FloorPlanInsightType` — no changes required for this release

`TOWER_ANALYSIS` and other future-section types are deferred per the target spec §4 — adding them now with no consuming UI would be speculative. Add when that section is actually built.

### 4. `DashboardMediaUploadType` — new value

```java
FLOOR_PLAN_INSIGHT_VISUAL_MEDIA(true /* project-scoped */, ...)
```

Content-type validation should accept image types (existing rules), plus `video/mp4` and `application/json` (Lottie) — mirroring how `HOME_LOTTIE_JSON`/`APP_SCREEN_LOTTIE_JSON` already handle Lottie uploads elsewhere in `DashboardMediaUploadType`. Storage key convention: `dashboard/projects/{projectId}/floor-plans/{floorPlanId}/visual-analysis/{uuid}.{ext}`.

## Response shape (additive to `ProjectFloorPlanInsightDetailResponse`)

Existing top-level fields, `rooms[]`, and `insights[]` are **unchanged** — any client still reading the old shape (including the current demo-fallback code path) keeps working. New fields are added alongside:

```json
{
  "floorPlanId": 991,
  "projectId": 123,
  "title": "3 BHK Type A",
  "unitConfigurationType": "BHK_3",
  "unitConfigurationTypeLabel": "3 BHK",
  "imageUrl": "https://cdn.squarefootstory.com/projects/123/floorplans/3bhk-a.webp",

  "rooms": [ /* unchanged FloorPlanRoomDimensionResponse[], now each item MAY also carry: */ ],
  "insights": [ /* unchanged FloorPlanInsightResponse[] */ ],

  "visualAnalysis": {
    "title": "Visual analysis of every important factor",
    "description": "Visualize how natural light, ventilation, and Vastu influence your space before making a decision.",
    "mediaType": "IMAGE",
    "mediaUrl": "https://cdn.squarefootstory.com/projects/123/floorplans/991/visual-analysis/insight.webp",
    "tags": [
      { "label": "Sun Lighting", "color": "#3B7DDD", "sortOrder": 1 },
      { "label": "Air ventilation", "color": "#3B7DDD", "sortOrder": 2 },
      { "label": "Vastu analysis", "color": "#3B7DDD", "sortOrder": 3 }
    ]
  },

  "spaceComparison": {
    "title": "Space Comparison",
    "rooms": [
      {
        "roomKey": "MASTER_BEDROOM",
        "label": "Master Bedroom",
        "iconKey": "bed",
        "dimensionText": "12ft * 14ft",
        "areaSqft": 168,
        "averageAreaSqft": 149,
        "differencePercent": 13,
        "comparisonLabel": "+13% from avg",
        "summary": "An average master bedroom in Varthur measures 149 sq ft. This unit's master bedroom is 13% bigger.",
        "hasComparisonData": true,
        "sortOrder": 1
      },
      {
        "roomKey": "DINING_ROOM",
        "label": "Dining Room",
        "iconKey": "dining",
        "dimensionText": "12 ft * 14 ft",
        "areaSqft": null,
        "averageAreaSqft": null,
        "differencePercent": null,
        "comparisonLabel": null,
        "summary": null,
        "hasComparisonData": false,
        "sortOrder": 2
      }
    ]
  },

  "demo": false,
  "sourceLabel": "Verified floor-plan intelligence"
}
```

Notes on fields not in the user's original sketch, added based on the audit:

- `spaceComparison.rooms[].hasComparisonData` — explicit boolean so the client never has to infer "is this room comparable" from null-checking three fields; maps directly to the target spec's "render gracefully when not authored" requirement.
- `roomKey` is `FloorPlanRoomType.name()` (stable enum key), `label` is the display string (may be dashboard-overridden, e.g. "Dinning Room" typo fixes without an enum change) — mirrors the existing `roomType`/`roomTypeLabel` split on `FloorPlanRoomDimensionResponse`.
- `demo` / `sourceLabel` at the top level — resolves the current-state audit's dead `insightsUrl`-adjacent gap and gives the mobile "sample content" disclaimer a real backend signal instead of an always-on client string. Recommended semantics: `demo=true` when the floor plan has zero authored `spaceComparison`/`visualAnalysis` data and the server chooses to fall back to a seeded example (see Demo Strategy below); `demo=false` and `sourceLabel="Verified floor-plan intelligence"` (or similar) once real dashboard-authored data exists for at least the Visual Analysis block or one room comparison.

`spaceComparison.rooms[]` is built server-side by mapping every active/non-deleted `FloorPlanRoomDimensionResponse` for the floor plan into this shape — no client-side join, no enum-name string matching. This is the fix for current-state audit §5.

## DTO / Java changes

- `FloorPlanRoomDimensionResponse` — add `averageAreaSqft`, `differencePercent`, `comparisonLabel` (derived, not stored — computed in the mapper as `differencePercent >= 0 ? "+"+pct+"% from avg" : pct+"% from avg"`), `summary`, `hasComparisonData` (derived: true iff `averageAreaSqft != null`).
- `FloorPlanRoomDimensionUpsertRequest` — add `averageAreaSqft`, `comparisonContextLabel`, `differencePercent`, `comparisonSummary` (all optional, same validation style as existing `@Digits`/`@Size` annotations).
- New `ProjectFloorPlanVisualAnalysisResponse` (title, description, mediaType, mediaUrl, tags: `List<VisualAnalysisTagResponse>`).
- New `ProjectFloorPlanVisualAnalysisUpsertRequest` + `VisualAnalysisTagUpsertRequest`.
- `ProjectFloorPlanInsightDetailResponse` — add `visualAnalysis` (nullable object), `spaceComparison` (object wrapping `rooms[]`, built from the same rooms already fetched — no extra query), `demo`, `sourceLabel`.
- New entities: `ProjectFloorPlanVisualAnalysisEntity`, `ProjectFloorPlanVisualAnalysisTagEntity`, both under `com.brandPitara.sfs.project.entity`, following the existing `ProjectFloorPlanRoomDimensionEntity` conventions (`BaseEntity`, `active`/`deleted` soft-delete pattern).
- New repository interfaces + a new `ProjectFloorPlanVisualAnalysisServiceImpl` (or fold into the existing `ProjectFloorPlanInsightServiceImpl` — recommend a new small service to keep `ProjectFloorPlanInsightServiceImpl` from growing an unrelated responsibility, but reuse the existing controller package/module, not a new top-level module).

## Dashboard endpoints (new, additive)

Follow the exact pattern of the existing room/insight dashboard controllers:

- `GET/POST/PUT/DELETE /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/rooms/{roomId}` — extend existing PUT to accept the new comparison fields (no new endpoint needed, room CRUD already exists).
- `GET/PUT /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/visual-analysis` — singular resource (get-or-create semantics on PUT, since it's one-per-floor-plan), same role rules as existing insight/room controllers (`ADMIN`/`DATA_ENTRY` write, `ADMIN`/`REVIEWER`/`DATA_ENTRY` read, ownership check via `DashboardProjectOwnershipService`).
- Media upload: reuse `POST /api/dashboard/media/presign-upload` with the new `uploadType=FLOOR_PLAN_INSIGHT_VISUAL_MEDIA`; dashboard saves the returned `publicUrl` into the visual-analysis PUT payload's `mediaUrl`, same flow as `FLOOR_PLAN_IMAGE` today.

No dashboard frontend exists in this workspace to wire this into (per `floor-plan-current-state.md` §6) — this section specifies the API surface only; the actual dashboard screen is separate follow-on scope.

## Demo/real-data strategy (addresses the "no hardcoded production data" guardrail)

Current mobile behavior hardcodes a demo dataset client-side (`demoFloorPlanInsights.ts`) whose numbers happen to match the screenshots. Recommended production strategy:

1. **Backend owns the demo signal.** Add `demo`/`sourceLabel` to the response as shown above, computed server-side: `demo=true` when neither `visualAnalysis` nor any room in `spaceComparison.rooms[]` has `hasComparisonData=true` for that floor plan.
2. **No demo data injected into real projects' rows.** The backend does not synthesize fake numbers for a real floor plan lacking data — it simply returns `demo=true` with empty/null comparison fields, and the client decides whether to show its own local fallback illustration (current behavior) or an empty/"coming soon" state (a product decision, not a technical one — flag to the user before implementation).
3. **If product wants a guaranteed non-empty demo experience on every project** (matching today's behavior where the sheet never appears empty), the backend can optionally support one designated demo/showcase floor plan whose `spaceComparison`/`visualAnalysis` rows are real authored dashboard data (not code-level hardcoding) with `demo` still computed `false` for it — genuinely real content used as a reference example, not a lie. This is a dashboard data-entry task, not a schema change.
4. Either way, mobile's `demoFloorPlanInsights.ts` local fallback can remain as a **network-failure/loading fallback only** (matches its original purpose per `floor-plan-unit-insights-demo-plan.md`), not as the default experience — remove the `ENABLE_STATIC_UNIT_INSIGHTS_DEMO = true` hardcode once the backend `demo` flag ships, per that doc's own "Removal Path" section.

## Backward compatibility

- All new fields are additive and nullable/optional; no existing field is renamed, removed, or repurposed.
- `rooms[]` and `insights[]` keep their current shape exactly — any consumer (including the current demo-fallback merge logic in `FloorPlanInsightsBottomSheet.tsx`) continues to work unmodified during a phased mobile rollout.
- The new `average*`/`difference*`/`comparison*` columns on `project_floor_plan_room_dimension` default to `NULL`, so existing rows render exactly as they do today (`hasComparisonData=false`) until a dashboard user fills them in.
- This is genuinely an **extension** of Floor Plan Intelligence v1, not a v2/parallel system — same tables (mostly), same controller, same public endpoint, same dashboard module.

## Test coverage to add alongside implementation

Given the current-state audit found zero tests for this entire feature area, any implementation PR should include, at minimum:

- `ProjectFloorPlanInsightServiceImplTest` (new) — `publicGetDetail` returns correct `spaceComparison.rooms[]` mapping including `hasComparisonData` false/true branches, `demo` flag computation, visibility/active filtering unchanged.
- `ProjectFloorPlanVisualAnalysisServiceImplTest` (new) — get/upsert, one-per-floor-plan uniqueness, tag ordering.
- Dashboard controller tests for the new visual-analysis endpoint (role/ownership checks, mirroring existing `DashboardProjectFloorPlanInsightController` test patterns if any exist — confirmed none currently do, so this is new coverage, not an update).
