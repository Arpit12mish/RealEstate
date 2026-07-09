# Floor Plan Improvement Plan

This plan is backward-compatible with the current backend. It keeps existing fields and APIs usable while adding structured data needed for a production mobile UI.

## Goals

- Reliable BHK/configuration grouping without parsing title text.
- Support `3 BHK`, `3.5 BHK`, `4 BHK`, `4.5 BHK`, and future custom configurations.
- Support multiple variants under one configuration, e.g. `3 BHK Type A`, `3 BHK Type B`, `3 BHK + Study`.
- Let dashboard data-entry users manage images, structured areas, room dimensions, and public insights.
- Let mobile render the reference design from API data without guessing.
- Keep old records and old mobile clients working.

## Proposed Production Model

Keep existing `project_floor_plan` rows as the main unit-layout table. Add missing structured fields instead of replacing the table.

Recommended floor-plan fields:

| Field | Current? | Recommendation |
|---|---:|---|
| `id` | Yes | Keep |
| `projectId` | Yes | Keep FK |
| `title` | Yes | Keep as card title / variant name |
| `floorCode` | Yes | Keep; use for internal stable variant code |
| `configurationKey` | No | Add string, e.g. `BHK_3`, `BHK_3_5` |
| `configurationLabel` | No | Add display label, e.g. `3 BHK`, `3.5 BHK` |
| `unitConfigurationType` | Yes | Keep for backward compatibility and filtering |
| `bedrooms` | Yes | Keep |
| `bathrooms` | Yes | Keep |
| `balconies` | Yes | Keep |
| `studyRoomCount` | No | Add |
| `servantRoomCount` | No | Add |
| `carpetAreaSqft` | Yes | Keep |
| `builtUpAreaSqft` | Yes | Keep |
| `superAreaSqft` | Yes | Keep |
| `saleableAreaSqft` | Yes | Keep |
| `layoutAreaSqft` | No | Add |
| `areaUnit` | No | Add, default `SQFT` |
| `price` | Yes | Keep |
| `priceLabel` | No | Add formatted display label |
| `facing` | Yes | Keep |
| `directionLabel` | No | Add; migrate/display from `directionSummary` |
| `floorRangeLabel` | No | Add; migrate/display from `floorRange` |
| `towerLabel` | No | Add; migrate/display from `towerName` |
| `imageUrl` | Yes | Keep required image |
| `thumbnailUrl` | No | Add if image optimization pipeline needs it |
| `description` | Yes | Keep |
| `sortOrder` | Yes | Keep |
| `active` | Yes | Keep |
| `publicVisible` | No | Add separate public visibility flag |
| `verified` | No | Add floor-plan-level verification flag |
| `insightsAvailable` | Yes | Keep auto-sync |
| `createdAt` | Yes | Expose if useful on dashboard |
| `updatedAt` | Yes | Expose if useful on dashboard |

## Configuration Design Decision

Use structured key plus display label:

```text
configurationKey: BHK_3
configurationLabel: 3 BHK

configurationKey: BHK_3_5
configurationLabel: 3.5 BHK
```

Why:

- Current `UnitConfigurationType` already supports `BHK_3_5` and `BHK_4_5`, but an enum alone can become restrictive for future marketing labels.
- `configurationKey` gives mobile a stable chip/group key.
- `configurationLabel` gives dashboard/public/mobile exact display text.
- `title` remains variant-specific, not grouping-critical.

Backward compatibility:

- Keep `unitConfigurationType`.
- On create/update, if `configurationKey` is absent but `unitConfigurationType` is present, derive:
  - `configurationKey = unitConfigurationType.name()`
  - `configurationLabel = unitConfigurationType.toLabel()`
- For old rows where both are absent, keep current fallback parser but improve it for decimal BHK labels.

## Database Migration Plan

Add columns to `project_floor_plan`:

```sql
ALTER TABLE project_floor_plan
  ADD COLUMN IF NOT EXISTS configuration_key VARCHAR(50),
  ADD COLUMN IF NOT EXISTS configuration_label VARCHAR(80),
  ADD COLUMN IF NOT EXISTS study_room_count INTEGER,
  ADD COLUMN IF NOT EXISTS servant_room_count INTEGER,
  ADD COLUMN IF NOT EXISTS layout_area_sqft DECIMAL(10, 2),
  ADD COLUMN IF NOT EXISTS area_unit VARCHAR(20) NOT NULL DEFAULT 'SQFT',
  ADD COLUMN IF NOT EXISTS price_label VARCHAR(80),
  ADD COLUMN IF NOT EXISTS direction_label VARCHAR(120),
  ADD COLUMN IF NOT EXISTS floor_range_label VARCHAR(120),
  ADD COLUMN IF NOT EXISTS tower_label VARCHAR(120),
  ADD COLUMN IF NOT EXISTS thumbnail_url TEXT,
  ADD COLUMN IF NOT EXISTS public_visible BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN IF NOT EXISTS verified BOOLEAN NOT NULL DEFAULT FALSE;
```

Add indexes:

```sql
CREATE INDEX IF NOT EXISTS idx_project_floor_plan_public_group
  ON project_floor_plan(project_id, public_visible, active, deleted, configuration_key, sort_order, id);

CREATE INDEX IF NOT EXISTS idx_project_floor_plan_configuration
  ON project_floor_plan(configuration_key)
  WHERE active = true AND deleted = false AND public_visible = true;
```

Backfill:

- From `unit_configuration_type` into `configuration_key` and `configuration_label`.
- From `direction_summary` into `direction_label` if label is null.
- From `floor_range` into `floor_range_label` if label is null.
- From `tower_name` into `tower_label` if label is null.
- Keep legacy string area fields untouched.

## Dashboard API Proposal

### Create/update floor plan request

```json
{
  "title": "3 BHK Type A",
  "floorCode": "3BHK-A",
  "configurationKey": "BHK_3",
  "configurationLabel": "3 BHK",
  "unitConfigurationType": "BHK_3",
  "imageUrl": "https://cdn.squarefootstory.com/projects/123/floorplans/3bhk-a.webp",
  "thumbnailUrl": "https://cdn.squarefootstory.com/projects/123/floorplans/3bhk-a-thumb.webp",
  "carpetAreaSqft": 1180,
  "builtUpAreaSqft": 1450,
  "superAreaSqft": 1550,
  "saleableAreaSqft": 1550,
  "layoutAreaSqft": 1180,
  "areaUnit": "SQFT",
  "bedrooms": 3,
  "bathrooms": 3,
  "balconies": 2,
  "studyRoomCount": 0,
  "servantRoomCount": 0,
  "facing": "EAST",
  "directionLabel": "East Facing",
  "price": 18500000,
  "priceLabel": "₹1.85 Cr onwards",
  "unitLabel": "Tower A, Type A",
  "towerLabel": "Tower A",
  "floorRangeLabel": "12th-20th Floor",
  "description": "Spacious 3 BHK with efficient carpet layout.",
  "sortOrder": 1,
  "active": true,
  "publicVisible": true,
  "verified": false
}
```

Validation:

- `title`, `imageUrl` remain required.
- Require either `configurationKey` or `unitConfigurationType` for new production floor plans.
- `configurationLabel` should be required when `configurationKey` is present and not derivable from enum metadata.
- Area values must be numeric and non-negative.
- `areaUnit` should default to `SQFT`.
- Counts should be non-negative.
- `publicVisible` defaults to true.
- `verified` should default to false and be controlled by reviewer/admin workflow if used as trust signal.

### Dashboard response

```json
{
  "id": 991,
  "projectId": 123,
  "title": "3 BHK Type A",
  "floorCode": "3BHK-A",
  "configurationKey": "BHK_3",
  "configurationLabel": "3 BHK",
  "unitConfigurationType": "BHK_3",
  "unitConfigurationTypeLabel": "3 BHK",
  "imageUrl": "https://cdn.squarefootstory.com/projects/123/floorplans/3bhk-a.webp",
  "thumbnailUrl": "https://cdn.squarefootstory.com/projects/123/floorplans/3bhk-a-thumb.webp",
  "carpetArea": "1180 sq ft",
  "exclusiveArea": null,
  "superArea": "1550 sq ft",
  "carpetAreaSqft": 1180,
  "builtUpAreaSqft": 1450,
  "superAreaSqft": 1550,
  "saleableAreaSqft": 1550,
  "layoutAreaSqft": 1180,
  "areaUnit": "SQFT",
  "price": 18500000,
  "priceLabel": "₹1.85 Cr onwards",
  "bedrooms": 3,
  "bathrooms": 3,
  "balconies": 2,
  "studyRoomCount": 0,
  "servantRoomCount": 0,
  "facing": "EAST",
  "directionLabel": "East Facing",
  "towerLabel": "Tower A",
  "floorRangeLabel": "12th-20th Floor",
  "unitLabel": "Tower A, Type A",
  "description": "Spacious 3 BHK with efficient carpet layout.",
  "sortOrder": 1,
  "active": true,
  "publicVisible": true,
  "verified": false,
  "featured": false,
  "insightsAvailable": true,
  "createdAt": "2026-07-08T10:00:00Z",
  "updatedAt": "2026-07-08T10:00:00Z"
}
```

## Public/Mobile API Proposal

Keep `GET /api/projects/{projectId}` returning grouped floor plans, but enrich each group and item.

```json
{
  "floorPlanGroups": [
    {
      "groupKey": "BHK_3",
      "groupLabel": "3 BHK",
      "configurationKey": "BHK_3",
      "configurationLabel": "3 BHK",
      "count": 3,
      "items": [
        {
          "id": 991,
          "projectId": 123,
          "title": "3 BHK Type A",
          "floorCode": "3BHK-A",
          "configurationKey": "BHK_3",
          "configurationLabel": "3 BHK",
          "imageUrl": "https://cdn.squarefootstory.com/projects/123/floorplans/3bhk-a.webp",
          "thumbnailUrl": "https://cdn.squarefootstory.com/projects/123/floorplans/3bhk-a-thumb.webp",
          "carpetAreaSqft": 1180,
          "builtUpAreaSqft": 1450,
          "superAreaSqft": 1550,
          "saleableAreaSqft": 1550,
          "layoutAreaSqft": 1180,
          "areaUnit": "SQFT",
          "carpetAreaLabel": "1,180 sq ft",
          "builtUpAreaLabel": "1,450 sq ft",
          "superAreaLabel": "1,550 sq ft",
          "saleableAreaLabel": "1,550 sq ft",
          "layoutAreaLabel": "1,180 sq ft",
          "price": 18500000,
          "priceLabel": "₹1.85 Cr onwards",
          "bedrooms": 3,
          "bathrooms": 3,
          "balconies": 2,
          "studyRoomCount": 0,
          "servantRoomCount": 0,
          "directionLabel": "East Facing",
          "towerLabel": "Tower A",
          "floorRangeLabel": "12th-20th Floor",
          "unitLabel": "Tower A, Type A",
          "description": "Spacious 3 BHK with efficient carpet layout.",
          "verified": false,
          "insightsAvailable": true,
          "insightsUrl": "/api/projects/123/floor-plans/991/insights"
        }
      ]
    }
  ]
}
```

Mobile rendering contract:

- Section title: `Floor Plans`
- Subtitle: derive from `floorPlanGroups.length`, or use optional server field `floorPlanSummary.configurationCountLabel`.
- Chips: `floorPlanGroups[].groupLabel`.
- Cards: selected group's `items`.
- Card title: `item.title`.
- Area rows:
  - Always show `carpetAreaLabel` when present.
  - Show `builtUpAreaLabel` or `superAreaLabel` depending on availability/business preference.
  - Show `layoutAreaLabel`/`saleableAreaLabel` if applicable.
- CTA:
  - Show `View Unit Insights` only when `insightsAvailable=true`.
  - CTA opens `insightsUrl` or calls the existing insight detail endpoint.
- Expand/fullscreen image:
  - Use `imageUrl`; use `thumbnailUrl` for card if present.

## Insight API Proposal

Keep:

- `GET /api/projects/{projectId}/floor-plans/{floorPlanId}/insights`

Enhance response with explicit demo/source metadata if demo content is introduced:

```json
{
  "floorPlanId": 991,
  "projectId": 123,
  "title": "3 BHK Type A",
  "configurationKey": "BHK_3",
  "configurationLabel": "3 BHK",
  "imageUrl": "...",
  "rooms": [],
  "insights": [],
  "demo": false,
  "sourceLabel": "Verified project data"
}
```

Static demo insight policy:

- Do not silently return static insights as real data.
- If demo fallback is required, add `demo=true` at response or insight item level.
- Demo insights must not set `verified=true`.
- Demo fallback should be disabled in production unless product explicitly approves the labeling.

## Dashboard UX Requirements

The dashboard floor-plan editor should expose:

- Configuration selector populated by `GET /api/dashboard/project-metadata/unit-configurations`.
- Variant title, e.g. `3 BHK Type A`.
- Floor code/internal code.
- Image upload using `FLOOR_PLAN_IMAGE`.
- Numeric area inputs:
  - Carpet area
  - Built-up area
  - Super built-up/super area
  - Saleable area
  - Layout area
- Area unit selector, default `SQFT`.
- Counts:
  - Bedrooms
  - Bathrooms
  - Balconies
  - Study rooms
  - Servant rooms
- Price and price label.
- Facing/direction, tower label, floor range label.
- Active/public-visible/featured controls based on role.
- Sort order.
- Nested room-dimension manager.
- Nested unit-insight manager.
- Mobile preview link after save.

Where 2 BHK / 3 BHK should be selected:

- In the floor-plan create/edit form, as the configuration selector.
- It should save `unitConfigurationType` today.
- After model upgrade, it should save `configurationKey`, `configurationLabel`, and optionally `unitConfigurationType` for backward compatibility.

## Backend Implementation Steps

1. Add migration for new columns and indexes.
2. Extend `ProjectFloorPlanEntity`.
3. Extend `ProjectFloorPlanUpsertRequest` and `ProjectFloorPlanResponse`.
4. Add helper to derive `configurationKey/configurationLabel` from `unitConfigurationType`.
5. Improve fallback grouping parser for decimal BHK labels.
6. Update `ProjectDetailComposerImpl.groupFloorPlans` to prefer `configurationKey/configurationLabel`.
7. Filter public floor plans by `publicVisible=true` in addition to active/deleted.
8. Add label formatting for numeric area fields, either in mapper or a dedicated presenter.
9. Add public/mobile `insightsUrl` when `insightsAvailable=true`.
10. Add tests for:
    - Create/update with configuration fields
    - Backfill/derivation from enum
    - Grouping by `BHK_3_5` and `BHK_4_5`
    - Multiple variants under one configuration
    - Public visibility filtering
    - Insight detail filters
    - Dashboard role/ownership checks
    - Mobile preview response shape

## Mobile Implementation Steps

1. Update `ProjectFloorPlanDto` to include structured backend fields.
2. Use `floorPlanGroups` from project detail as the primary floor-plan source.
3. Remove or de-emphasize flat `/floor-plans` usage for UI that needs grouped chips.
4. Render subtitle from group count.
5. Render all groups, not only `groups.slice(0, 4)`, or make horizontal chips scroll.
6. Use structured area labels/numbers, not legacy strings.
7. Show floor-plan-specific `priceLabel`.
8. Show "View Unit Insights" when `insightsAvailable=true`.
9. Fetch and render insight detail screen from `/api/projects/{projectId}/floor-plans/{floorPlanId}/insights`.
10. Preserve fullscreen image preview.

## Compatibility Notes

- Existing clients can keep using `title`, `imageUrl`, `carpetArea`, `exclusiveArea`, `superArea`.
- Existing flat endpoint can remain for older meter screens.
- New clients should use `floorPlanGroups` from project detail.
- `unitConfigurationType` should remain supported for browse filters and old dashboard payloads.
- Null-clearing behavior should be addressed separately, likely with PATCH semantics or explicit empty values.

## Open Decisions

- Whether `configurationKey/configurationLabel` should be nullable for commercial/plot plans or required for every plan.
- Whether `verified` is controlled by reviewers only.
- Whether `publicVisible` can be toggled by DATA_ENTRY or only ADMIN/REVIEWER.
- Whether `thumbnailUrl` is manually uploaded or generated by a media pipeline.
- Whether demo/static insights are allowed in production, and if yes, exact labeling.
