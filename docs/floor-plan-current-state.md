# Floor Plan Current State

This document records the current implementation found in the backend repo at `/Users/mac/Desktop/RealEstate` and the available mobile app repos at `../SFS_App` and `../Mobile_SFS/SFS_App`.

No dashboard frontend package was found in this workspace. The backend contains dashboard APIs, API docs, and Node dashboard API test scripts, but no React/Angular/Vue dashboard UI code.

## 1. Database Schema

### `project_floor_plan`

Created by `V45__create_project_floor_plan_table.sql`; expanded by `V84__floor_plan_intelligence_v1.sql`.

| Column | Type | Required | Notes |
|---|---:|---:|---|
| `id` | `BIGSERIAL` | Yes | Primary key |
| `project_id` | `BIGINT` | Yes | FK to `project(id)` |
| `title` | `VARCHAR(160)` | Yes | Main display title, e.g. "3 BHK Type A" |
| `floor_code` | `VARCHAR(80)` | No | Optional internal code |
| `image_url` | `TEXT` | Yes | Floor-plan image URL saved directly on floor plan |
| `carpet_area` | `VARCHAR(100)` | No | Legacy display string |
| `exclusive_area` | `VARCHAR(100)` | No | Legacy display string |
| `super_area` | `VARCHAR(100)` | No | Legacy display string |
| `unit_label` | `VARCHAR(120)` | No | Display/helper label |
| `description` | `TEXT` | No | Description |
| `sort_order` | `INTEGER` | Yes | Default `0`; list ordering |
| `active` | `BOOLEAN` | Yes | Default `true`; public and preview lists require active |
| `deleted` | `BOOLEAN` | Yes | Default `false`; soft delete |
| `created_at` | `TIMESTAMPTZ` | Yes | From `BaseEntity` |
| `updated_at` | `TIMESTAMPTZ` | Yes | From `BaseEntity` |
| `unit_configuration_type` | `VARCHAR(30)` | No | Enum `UnitConfigurationType`; supports `BHK_3_5`, `BHK_4_5`, etc. |
| `price` | `BIGINT` | No | Unit/floor-plan price |
| `saleable_area_sqft` | `DECIMAL(10,2)` | No | Numeric saleable area |
| `carpet_area_sqft` | `DECIMAL(10,2)` | No | Numeric carpet area |
| `built_up_area_sqft` | `DECIMAL(10,2)` | No | Numeric built-up area |
| `super_area_sqft` | `DECIMAL(10,2)` | No | Numeric super area |
| `floor_height_meters` | `DECIMAL(5,2)` | No | Structured intelligence field |
| `carpet_efficiency_percent` | `DECIMAL(5,2)` | No | 0-100 intended by DTO validation |
| `bedrooms` | `INTEGER` | No | Count |
| `bathrooms` | `INTEGER` | No | Count |
| `balconies` | `INTEGER` | No | Count |
| `facing` | `VARCHAR(100)` | No | Free text |
| `direction_summary` | `VARCHAR(255)` | No | Free text summary |
| `tower_name` | `VARCHAR(100)` | No | Tower name |
| `floor_range` | `VARCHAR(100)` | No | Floor range |
| `key_plan_image_url` | `TEXT` | No | Additional image URL |
| `featured` | `BOOLEAN` | Yes | Default `false`; used in project-card native queries |
| `insights_available` | `BOOLEAN` | Yes | Default `false`; auto-synced from public active insights |

Indexes:

- `idx_project_floor_plan_project_id(project_id)`

### `project_floor_plan_room_dimension`

Created by `V84__floor_plan_intelligence_v1.sql`; patched by `V85__floor_plan_intelligence_v1_fixes.sql`.

| Column | Type | Required | Notes |
|---|---:|---:|---|
| `id` | `BIGSERIAL` | Yes | Primary key |
| `floor_plan_id` | `BIGINT` | Yes | FK to `project_floor_plan(id)` |
| `room_type` | `VARCHAR(50)` | Yes | Enum `FloorPlanRoomType` |
| `label` | `VARCHAR(100)` | No | Display label |
| `length_ft` | `DECIMAL(8,2)` | No | Numeric length |
| `width_ft` | `DECIMAL(8,2)` | No | Numeric width |
| `area_sqft` | `DECIMAL(10,2)` | No | Numeric area |
| `dimension_text` | `VARCHAR(100)` | No | Preformatted string, e.g. `12.1 ft x 10.6 ft` |
| `icon_key` | `VARCHAR(60)` | No | UI icon key |
| `notes` | `VARCHAR(255)` | No | Notes |
| `sort_order` | `INTEGER` | Yes | Default `0` |
| `active` | `BOOLEAN` | Yes | Default `true` |
| `deleted` | `BOOLEAN` | Yes | Default `false` |
| `created_at` | `TIMESTAMPTZ` | Yes | Audit timestamp |
| `updated_at` | `TIMESTAMPTZ` | Yes | Audit timestamp |

Indexes:

- `idx_pfp_room_dim_floor_plan_id(floor_plan_id)`
- `idx_pfp_room_dim_floor_plan_active(floor_plan_id, active, deleted)`

### `project_floor_plan_insight`

Created by `V84__floor_plan_intelligence_v1.sql`; patched by `V85__floor_plan_intelligence_v1_fixes.sql`.

| Column | Type | Required | Notes |
|---|---:|---:|---|
| `id` | `BIGSERIAL` | Yes | Primary key |
| `floor_plan_id` | `BIGINT` | Yes | FK to `project_floor_plan(id)` |
| `insight_type` | `VARCHAR(50)` | Yes | Enum `FloorPlanInsightType` |
| `title` | `VARCHAR(160)` | Yes | Display title |
| `summary` | `VARCHAR(500)` | No | Short summary |
| `detailed_text` | `TEXT` | No | Long explanation |
| `unit_value` | `DECIMAL(12,4)` | No | Numeric benchmark field |
| `benchmark_value` | `DECIMAL(12,4)` | No | Numeric benchmark field |
| `unit_label` | `VARCHAR(30)` | No | Unit for benchmark values |
| `difference_value` | `DECIMAL(12,4)` | No | Numeric difference |
| `difference_percent` | `DECIMAL(8,4)` | No | Percent difference |
| `chart_label_this_unit` | `VARCHAR(80)` | No | Chart label |
| `chart_label_average` | `VARCHAR(80)` | No | Chart label |
| `comparison_result` | `VARCHAR(30)` | No | Enum `InsightComparisonResult` |
| `score` | `INTEGER` | No | Legacy/simple score |
| `positive` | `BOOLEAN` | Yes | Default `true` |
| `public_visible` | `BOOLEAN` | Yes | Default `true`; public detail filters on this |
| `verified` | `BOOLEAN` | Yes | Default `false` |
| `active` | `BOOLEAN` | Yes | Default `true` |
| `deleted` | `BOOLEAN` | Yes | Default `false` |
| `sort_order` | `INTEGER` | Yes | Default `0` |
| `created_at` | `TIMESTAMPTZ` | Yes | Audit timestamp |
| `updated_at` | `TIMESTAMPTZ` | Yes | Audit timestamp |

Indexes:

- `idx_pfp_insight_floor_plan_id(floor_plan_id)`
- `idx_pfp_insight_floor_plan_public(floor_plan_id, public_visible, active, deleted)`

## 2. How 2 BHK / 3 BHK / 3.5 BHK / 4 BHK Are Represented

There are two representations today:

1. Legacy/free-text fields:
   - `title`, e.g. `2BHK Type A`
   - `unitLabel`
   - `floorCode`
   - public grouping fallback parses these strings.

2. Structured enum:
   - `ProjectFloorPlanEntity.unitConfigurationType`
   - Java enum `UnitConfigurationType` includes `STUDIO`, `BHK_1`, `BHK_1_5`, `BHK_2`, `BHK_2_5`, `BHK_3`, `BHK_3_5`, `BHK_4`, `BHK_4_5`, `BHK_5`, `BHK_5_PLUS`, `OFFICE`, `RETAIL`, `PLOT`, `OTHER`.
   - `toLabel()` returns display labels like `3 BHK`, `3.5 BHK`, `4 BHK`, `4.5 BHK`.

Where dashboard should select this:

- Backend metadata endpoint: `GET /api/dashboard/project-metadata/unit-configurations`
- Dashboard floor-plan create/update field: `unitConfigurationType` in `ProjectFloorPlanUpsertRequest`
- Current dashboard UI code is not present in this workspace, so the exact screen/component cannot be verified. Based on backend routing, this belongs in the floor-plan form behind `/api/dashboard/projects/{projectId}/floor-plans`, not only in the title field.

## 3. Backend Write Flow

### Floor plan CRUD

Dashboard endpoint:

- `POST /api/dashboard/projects/{projectId}/floor-plans`
- `PUT /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}`
- `PATCH /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/active?active=true|false`
- `DELETE /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}`

Admin-only legacy/admin endpoint:

- `POST /api/admin/projects/{projectId}/floor-plans`
- `GET /api/admin/projects/{projectId}/floor-plans`
- `PUT /api/admin/projects/{projectId}/floor-plans/{floorPlanId}`
- `PATCH /api/admin/projects/{projectId}/floor-plans/{floorPlanId}/active`
- `DELETE /api/admin/projects/{projectId}/floor-plans/{floorPlanId}`

Request DTO: `ProjectFloorPlanUpsertRequest`

Required fields:

- `title`: `@NotBlank`, max 150
- `imageUrl`: `@NotBlank`, max 500

Optional fields:

- `floorCode` max 50
- `carpetArea`, `exclusiveArea`, `superArea` max 50 legacy strings
- `unitLabel` max 80
- `description` max 1000
- `sortOrder` 0-9999
- `active`
- `unitConfigurationType`
- `price` min 0
- numeric areas `saleableAreaSqft`, `carpetAreaSqft`, `builtUpAreaSqft`, `superAreaSqft`: decimal min 0, up to 8 integer digits and 2 fractional digits
- `floorHeightMeters`: decimal min 0
- `carpetEfficiencyPercent`: 0-100
- `bedrooms`, `bathrooms`, `balconies`: 0-99
- `facing`, `directionSummary`, `towerName`, `floorRange`, `keyPlanImageUrl`, `featured`

Service: `ProjectFloorPlanServiceImpl`

- `create(projectId, request)` resolves `ProjectEntity` with `findByIdAndDeletedFalse`.
- Builds `ProjectFloorPlanEntity`, trims strings, defaults `sortOrder=0`, `active=true`, `deleted=false`, `featured=false`.
- Saves with `ProjectFloorPlanRepository.save`.
- Bumps content versions:
  - Always `PROJECTS`
  - `HOME` if project is published and active

Update behavior:

- Resolves floor plan by `findByIdAndDeletedFalse`.
- Verifies floor plan belongs to `projectId`.
- Applies only non-null request fields.
- This means current update cannot clear nullable fields by sending `null`; null means "leave unchanged".

Delete behavior:

- Soft deletes by `deleted=true`, `active=false`.

Repository methods:

- `findByProjectIdAndDeletedFalseOrderBySortOrderAscIdAsc`
- `findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc`
- `findByProjectIdInAndActiveTrueAndDeletedFalseOrderByProjectIdAscSortOrderAscIdAsc`
- `findByIdAndDeletedFalse`
- `findDistinctProjectIdsByUnitConfigurationTypes`

Role access:

- Dashboard list: `ADMIN`, `REVIEWER`, `DATA_ENTRY`
- Dashboard create/update: `ADMIN`, `DATA_ENTRY`
- Dashboard active toggle/delete: `ADMIN` only
- Admin endpoints: `ADMIN` only

DATA_ENTRY ownership checks:

- Dashboard create/update call `DashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId)`.
- That policy allows:
  - `ADMIN`: always
  - `DATA_ENTRY`: only projects created by the same dashboard user, and only review statuses `DRAFT`, `RECHECK`, `REJECTED`
  - `REVIEWER`: cannot edit

Audit logging:

- Dashboard floor plan create records `FLOOR_PLAN_CREATED` with `ReviewEntityType.PROJECT_FLOOR_PLAN`.
- Update records `FLOOR_PLAN_UPDATED`.
- Active toggle records `FLOOR_PLAN_ACTIVATED`.
- Delete records `FLOOR_PLAN_DELETED`.

### Room dimensions

Dashboard endpoint:

- `GET /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/rooms`
- `POST /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/rooms`
- `PUT /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/rooms/{roomId}`
- `DELETE /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/rooms/{roomId}`

Request DTO: `FloorPlanRoomDimensionUpsertRequest`

- Required: `roomType`
- Optional: `label`, `lengthFt`, `widthFt`, `areaSqft`, `dimensionText`, `iconKey`, `notes`, `sortOrder`, `active`

Roles:

- GET: `ADMIN`, `REVIEWER`, `DATA_ENTRY`
- POST/PUT: `ADMIN`, `DATA_ENTRY`, with project ownership check
- DELETE: `ADMIN`

Service: `ProjectFloorPlanRoomDimensionServiceImpl`

- Verifies floor plan exists and belongs to project.
- List returns non-deleted rooms ordered by `sortOrder`, then `id`.
- Public insight detail later filters rooms to active and non-deleted.

### Floor-plan insights

Dashboard endpoint:

- `GET /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/insights`
- `POST /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/insights`
- `PUT /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/insights/{insightId}`
- `DELETE /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/insights/{insightId}`

Request DTO: `FloorPlanInsightUpsertRequest`

- Required: `insightType`, `title`
- Optional: `summary`, `detailedText`, benchmark fields, chart labels, `comparisonResult`, `score`, `positive`, `publicVisible`, `verified`, `active`, `sortOrder`

Roles:

- GET: `ADMIN`, `REVIEWER`, `DATA_ENTRY`
- POST/PUT: `ADMIN`, `DATA_ENTRY`, with project ownership check
- DELETE: `ADMIN`

Service: `ProjectFloorPlanInsightServiceImpl`

- Verifies floor plan exists and belongs to project.
- Creates/updates/deletes insight rows.
- After POST/PUT/DELETE, recalculates `project_floor_plan.insights_available`.
- `insightsAvailable=true` only when at least one insight is `publicVisible=true`, `active=true`, `deleted=false`.

## 4. Backend Read Flow

### Dashboard list

Endpoint:

- `GET /api/dashboard/projects/{projectId}/floor-plans`

Service:

- `ProjectFloorPlanServiceImpl.adminList`

Filters:

- `deleted=false`
- Does not require `active=true`
- Does not verify project existence in `adminList`; repository returns empty if project has no plans or invalid ID.

Sorting:

- `sortOrder ASC`, then `id ASC`

Shape:

- Flat `List<ProjectFloorPlanResponse>`

### Dashboard workspace

Endpoint:

- `GET /api/dashboard/projects/{projectId}/workspace`

Service:

- `DashboardProjectWorkspaceServiceImpl.getWorkspace`

Floor plan shape:

- `floorPlans: List<ProjectFloorPlanResponse>`
- Flat list, not grouped.

### Dashboard mobile preview

Endpoint:

- `GET /api/dashboard/projects/{projectId}/mobile-preview`

Service:

- `DashboardMobilePreviewServiceImpl.getPreview`
- Calls `ProjectDetailComposer.composeForPreview`
- Floor plans come from `dashboardPreviewList`

Filters:

- `active=true`
- `deleted=false`
- Does not require project to be published/approved.

Shape:

- `detail.floorPlanGroups: List<ProjectFloorPlanGroupResponse>`
- Grouped for the mobile detail-style preview.

### Public project detail

Endpoint:

- `GET /api/projects/{projectId}`

Service:

- `ProjectServiceImpl.publicGet`
- `ProjectDetailComposerImpl.composePublic`
- `buildFloorPlanGroups(projectId)` calls `ProjectFloorPlanService.publicList`.

Project visibility:

- Project must pass `ProjectPublicVisibilityPolicy.assertPubliclyVisible`.
- The project must be published, active, non-deleted, and approved according to that policy.

Floor plan filters:

- `active=true`
- `deleted=false`

Shape:

- `ProjectPublicResponse.floorPlanGroups`
- Each group has `groupKey`, `groupLabel`, `items`.

Grouping logic:

- If `unitConfigurationType` exists, group key is `unitConfigurationType.name()`, e.g. `BHK_3_5`; group label is `unitConfigurationTypeLabel`, e.g. `3.5 BHK`.
- If `unitConfigurationType` is null, fallback key is derived from `unitLabel`, then `title`, then `floorCode`.
- Fallback recognizes `STUDIO`, `OFFICE`, `1 BHK`/`1BHK`, `2 BHK`/`2BHK`, `3 BHK`/`3BHK`, `4 BHK`/`4BHK`, `PENTHOUSE`, `VILLA`.
- Fallback does not explicitly recognize `3.5 BHK` or `4.5 BHK`; those are reliable only through `unitConfigurationType`.

### Public flat floor-plan endpoint

Endpoint:

- `GET /api/projects/{projectId}/floor-plans`

Service:

- `ProjectFloorPlanServiceImpl.publicList`

Filters:

- Project must be publicly visible.
- Floor plans must be `active=true`, `deleted=false`.

Shape:

- Flat `List<ProjectFloorPlanResponse>`.

### Public insight detail endpoint

Endpoint:

- `GET /api/projects/{projectId}/floor-plans/{floorPlanId}/insights`

Service:

- `ProjectFloorPlanInsightServiceImpl.publicGetDetail`

Filters:

- Project must be publicly visible.
- Floor plan must belong to project and be `active=true`.
- Rooms: `active=true`, `deleted=false`.
- Insights: `publicVisible=true`, `active=true`, `deleted=false`.

Shape:

- `ProjectFloorPlanInsightDetailResponse` includes floor-plan summary fields, rooms, and insights.

## 5. Upload Flow

Dashboard presign endpoint:

- `POST /api/dashboard/media/presign-upload`

Access:

- `ADMIN`, `DATA_ENTRY`

Request DTO:

```json
{
  "uploadType": "FLOOR_PLAN_IMAGE",
  "contentType": "image/webp",
  "fileSizeBytes": 1200000,
  "projectId": 123
}
```

Validation:

- `uploadType=FLOOR_PLAN_IMAGE` is project-scoped and requires `projectId`.
- Project-scoped uploads except `MASTER_PLAN_IMAGE` call `DashboardProjectOwnershipService.assertCurrentUserCanEditProject`.
- Supported image content types: `image/jpeg`, `image/jpg`, `image/png`, `image/webp`.
- WebP is supported.
- Generic image max size is 2 MB.

Storage key:

- `dashboard/projects/{projectId}/floor-plans/{uuid}.{ext}`

Response DTO:

```json
{
  "uploadUrl": "...",
  "publicUrl": "...",
  "storageKey": "dashboard/projects/123/floor-plans/uuid.webp",
  "expiresInSeconds": 300,
  "requiredHeaders": {
    "Content-Type": "image/webp",
    "Cache-Control": "public, max-age=31536000, immutable"
  }
}
```

Where URL is saved:

- The returned `publicUrl` is not automatically attached to any media row.
- Dashboard must save it into `ProjectFloorPlanUpsertRequest.imageUrl`.
- `ProjectFloorPlanEntity.imageUrl` stores it directly.

Relationship to `ProjectMedia`:

- Floor-plan images are not represented as `ProjectMedia`.
- `ProjectMediaType` only has `IMAGE`, `BROCHURE_PDF`, and `VIDEO`.
- `FLOOR_PLAN_IMAGE` is an upload type, not a `ProjectMediaType`.

## 6. Dashboard Flow

Backend-supported dashboard flow:

1. Load unit options from `GET /api/dashboard/project-metadata/unit-configurations`.
2. Upload image through `POST /api/dashboard/media/presign-upload` with `uploadType=FLOOR_PLAN_IMAGE`.
3. PUT the file to the returned `uploadUrl` using the returned `requiredHeaders`.
4. Save the returned `publicUrl` as `imageUrl` in `POST /api/dashboard/projects/{projectId}/floor-plans`.
5. Add optional structured fields, especially `unitConfigurationType`, numeric areas, bedrooms/bathrooms/balconies, facing/tower/floor info.
6. Add room dimensions through `/rooms` if required.
7. Add insights through `/insights` if required.
8. Preview with `GET /api/dashboard/projects/{projectId}/mobile-preview`.
9. Submit project for review through the dashboard review workflow.

Current verifiable dashboard UI:

- No dashboard frontend code was available in this workspace.
- Therefore the exact screen/form, visible fields, upload component, and whether users can actually enter room dimensions/insights from UI cannot be verified from code.
- API docs describe floor-plan endpoints and later docs describe insight/room endpoints, but the older "Project Floor Plans" section still documents only legacy fields and omits the newer structured floor-plan fields.

Fields visible to data-entry users from backend perspective:

- DATA_ENTRY can create/update floor plans, rooms, and insights if ownership/review-status checks pass.
- DATA_ENTRY can list floor plans, rooms, insights, workspace, and mobile preview.
- DATA_ENTRY cannot toggle active/delete floor plans, rooms, or insights.

Missing/uncertain dashboard UX fields:

- No verified UI for selecting `unitConfigurationType`.
- No verified UI for numeric area fields.
- No verified UI for room dimensions.
- No verified UI for insights.
- No verified UI for multiple variants under one configuration, though backend supports it.

## 7. Mobile/Public Flow

### Project detail screen in `../SFS_App`

File:

- `../SFS_App/app/projects/[projectId].tsx`

Consumes:

- Project detail response via `fetchProjectDetail`.
- Uses `data.floorPlanGroups`.

Rendering:

- `FloorPlansSection` receives `floorPlanGroups`.
- Shows section title `Floor Plans`.
- Does not show subtitle like `4 configurations available` in this copy.
- Displays only first four groups: `const visibleGroups = groups.slice(0, 4)`.
- Chips/tabs are generated from `group.groupLabel`.
- Selected chip filters to `activeGroup.items`.
- Cards show:
  - `item.title`
  - `item.superArea ?? item.carpetArea ?? "—"` as a single area string
  - `item.imageUrl`
  - expand icon
  - price derived from project `basePrice`, not floor-plan `price`
- Fullscreen image preview is supported through `ImageViewing`.
- No "View Unit Insights" CTA is rendered in this copy.
- Does not render structured numeric fields like `carpetAreaSqft`, `builtUpAreaSqft`, `saleableAreaSqft`, or `insightsAvailable`.

### Project meter detail screen in `../SFS_App`

File:

- `../SFS_App/app/(home)/meter/[projectId].tsx`

Consumes:

- Project detail
- Project meter detail
- Flat floor-plan list from `fetchProjectFloorPlans(projectId)` -> `GET /api/projects/{projectId}/floor-plans`

Rendering:

- `FloorPlanTab` receives flat `floorPlans`.
- Cards show image, title, legacy `carpetArea`, and legacy `exclusiveArea`.
- Fullscreen preview is supported with `react-native-image-zoom-viewer`.
- No grouping chips.
- No numeric structured fields.
- No insight CTA.

### `../Mobile_SFS/SFS_App`

This second mobile copy has an additional component:

- `components/projects/FloorPlanCard.tsx`

It renders closer to the reference:

- Title: `Floor Plans`
- Subtitle: `{n} configuration(s) available`
- Chips from `floorPlanGroups[].groupLabel`
- Selected chip filters cards.
- Multiple items per group are supported.
- Cards show image, title, expand icon, and legacy string areas: `carpetArea`, `exclusiveArea`, `superArea`.
- Fullscreen preview is supported.
- No "View Unit Insights" CTA.
- Type definitions still omit structured backend fields.

## 8. Current Data-Entry Guide

1. Open or create the project in dashboard.
2. Select the floor-plan configuration from dashboard metadata (`GET /api/dashboard/project-metadata/unit-configurations`) and save it as `unitConfigurationType` on the floor plan. If the current UI does not expose this field, backend supports it but dashboard UI needs to add it.
3. Upload the floor-plan image:
   - Call `POST /api/dashboard/media/presign-upload`.
   - Use `uploadType=FLOOR_PLAN_IMAGE`, `projectId`, image content type, and file size.
   - Upload the file to `uploadUrl` with the returned headers.
4. Copy/save the returned `publicUrl`.
5. Create the floor plan:
   - `POST /api/dashboard/projects/{projectId}/floor-plans`
   - Required: `title`, `imageUrl`.
   - Recommended: `unitConfigurationType`, `floorCode`, numeric area fields, room counts, `sortOrder`, `active=true`.
6. Add area details:
   - Prefer numeric fields: `carpetAreaSqft`, `builtUpAreaSqft`, `superAreaSqft`, `saleableAreaSqft`.
   - Use legacy strings (`carpetArea`, `exclusiveArea`, `superArea`) only for backward-compatible display until mobile is upgraded.
7. Add room dimensions if supported by dashboard UI:
   - `POST /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/rooms`
   - Add `roomType`, dimensions, display labels, and sort order.
8. Add unit insights if supported by dashboard UI:
   - `POST /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/insights`
   - Set `publicVisible=true` and `active=true` for public/mobile visibility.
   - `insightsAvailable` updates automatically.
9. Preview:
   - `GET /api/dashboard/projects/{projectId}/mobile-preview`
   - Check `detail.floorPlanGroups`.
10. Submit project for review through the dashboard review flow.

## 9. Current Gaps Against Reference UI

- Stable grouping exists when `unitConfigurationType` is set, but legacy records may rely on title/unit-label parsing.
- Existing enum already supports `3.5 BHK` and `4.5 BHK`.
- There is no separate `configurationKey`/`configurationLabel`; backend exposes enum value and derived enum label.
- Fallback grouping does not explicitly parse `3.5 BHK` or `4.5 BHK`.
- Dashboard UI code was not available, so actual field exposure cannot be verified.
- Backend supports multiple floor plans under one `unitConfigurationType`.
- Backend supports room dimensions and insights, but mobile project detail does not call insight detail or show "View Unit Insights".
- Public project detail exposes grouped floor plans but not embedded room dimensions or embedded insight lists.
- Public insight detail exists as a separate endpoint.
- Mobile types in both mobile copies only model legacy fields and omit structured fields.
- Mobile renders legacy string areas, not the structured numeric areas.
- Mobile project detail uses project base price, not floor-plan `price`.
- No `layoutAreaSqft`, `areaUnit`, `priceLabel`, `thumbnailUrl`, `publicVisible`, `verified`, `studyRoomCount`, or `servantRoomCount` fields exist on floor plans.
- `ProjectFloorPlanUpsertRequest` update cannot clear nullable fields because nulls are ignored.
- Tests cover dashboard floor-plan create/update permissions lightly and project comparison units, but there are no focused tests found for grouping behavior, 3.5/4.5 grouping fallback, insight public detail, or mobile preview floor-plan grouping.

## 10. Public Insights and Static Demo Data

Current public insights:

- Stored in `project_floor_plan_insight`.
- Public endpoint is `GET /api/projects/{projectId}/floor-plans/{floorPlanId}/insights`.
- Only active, non-deleted, public-visible insights are returned.
- Room dimensions are returned in the same public insight detail response.
- `ProjectFloorPlanResponse.insightsAvailable` tells clients whether a floor plan has at least one public active insight.

Can we show static data as demo for every project?

- Current backend does not inject static/demo floor-plan insights in public responses.
- Current migrations do not seed generic floor-plan insight demo rows for every project.
- Technically it is possible, but it should not be mixed silently with verified project data.
- Safer options:
  - Add explicit demo fallback in the API response only when no real insights exist, with fields like `demo=true`, `sourceLabel="Demo"`, and never set `verified=true`.
  - Or seed demo insights only in local/dev environments, not production.
  - Or let mobile show a disabled/empty "Unit Insights" state until dashboard users add real insights.

Production recommendation:

- Do not show static demo insights as if they are real project-specific data.
- If business wants demo content for every project, expose it as clearly labeled sample/demo content and keep it out of rating/verification/comparison logic.
