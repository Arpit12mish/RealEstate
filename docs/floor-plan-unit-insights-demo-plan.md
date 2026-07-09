# Floor Plan Unit Insights Demo Plan

## Real Backend Endpoint

The existing Floor Plan Intelligence v1 public endpoint remains the source of truth for real insights:

`GET /api/projects/{projectId}/floor-plans/{floorPlanId}/insights`

The response is `ProjectFloorPlanInsightDetailResponse` and includes:

- Floor-plan summary fields such as `floorPlanId`, `projectId`, `title`, `imageUrl`, `unitConfigurationType`, area fields, facing, tower, and key-plan image.
- `rooms[]` from `FloorPlanRoomDimensionResponse`.
- `insights[]` from `FloorPlanInsightResponse`.

Public visibility is already enforced by `ProjectFloorPlanInsightServiceImpl.publicGetDetail`:

- Project must be publicly visible.
- Floor plan must belong to the project and be active.
- Rooms are returned only when active and not deleted.
- Insights are returned only when `publicVisible=true`, `active=true`, and `deleted=false`.

`ProjectFloorPlanResponse` already exposes `insightsAvailable`, and project detail includes grouped floor-plan items through `floorPlanGroups`.

## Mobile Fallback Strategy

The mobile app should use this priority:

1. If `floorPlan.insightsAvailable=true`, call the real endpoint.
2. If the response contains useful real `rooms` or `insights`, render the real data.
3. If the endpoint returns 404/403/401/network error/empty data, render mobile-local demo data while the demo flag is enabled.
4. If `insightsAvailable=false`, skip the real endpoint and render demo data while the demo flag is enabled.

Demo data must remain mobile-only. Do not seed demo rows into the production database, and do not mark demo data as verified.

## Demo Data Structure

The mobile demo model mirrors the real response shape enough for the same bottom sheet to render both sources:

- `source: "REAL" | "DEMO"`
- `floorPlanId`
- `projectId`
- `title`
- `imageUrl`
- `rooms[]`
- `insights[]`
- `analysisFactors[]`
- Optional visual asset metadata for the lighting/windflow section.

The demo fallback currently contains static room cards, one master-bedroom comparison insight, comparison bars, and analysis factors matching the product reference.

## Removal Path

When dashboard-managed real floor-plan insights are ready:

1. Set the mobile demo flag `ENABLE_STATIC_UNIT_INSIGHTS_DEMO` to `false`.
2. Remove the local demo data file and fallback-only copy.
3. Keep the real API client method and bottom sheet renderer.
4. Optionally hide the CTA when `insightsAvailable=false` if product no longer wants a fallback experience.
5. Add integration coverage for a real insight response from the dashboard-created data.
