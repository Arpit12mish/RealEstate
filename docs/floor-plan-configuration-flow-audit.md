# Floor Plan Configuration Flow Audit

Date: 2026-07-08

## Safety Baseline

`git status --short` was run before edits. The worktree already had many unrelated modified and untracked files in brand, instagram, dbsearch, media presign, city, app config, and test areas, plus existing untracked floor-plan docs. This task only changes floor-plan grouping code, focused floor-plan grouping tests, and these audit docs.

## Backend Files Inspected

- `src/main/java/com/brandPitara/sfs/project/entity/ProjectFloorPlanEntity.java`
- `src/main/java/com/brandPitara/sfs/project/dto/ProjectFloorPlanUpsertRequest.java`
- `src/main/java/com/brandPitara/sfs/project/dto/ProjectFloorPlanResponse.java`
- `src/main/java/com/brandPitara/sfs/project/dto/ProjectFloorPlanGroupResponse.java`
- `src/main/java/com/brandPitara/sfs/project/mapper/ProjectFloorPlanMapper.java`
- `src/main/java/com/brandPitara/sfs/project/repository/ProjectFloorPlanRepository.java`
- `src/main/java/com/brandPitara/sfs/project/service/ProjectFloorPlanService.java`
- `src/main/java/com/brandPitara/sfs/project/service/impl/ProjectFloorPlanServiceImpl.java`
- `src/main/java/com/brandPitara/sfs/project/service/impl/ProjectDetailComposerImpl.java`
- `src/main/java/com/brandPitara/sfs/project/enums/UnitConfigurationType.java`
- `src/main/java/com/brandPitara/sfs/dashboard/project/controller/DashboardProjectFloorPlanController.java`
- `src/main/java/com/brandPitara/sfs/dashboard/project/controller/DashboardProjectMetadataController.java`
- `src/main/java/com/brandPitara/sfs/dashboard/project/controller/DashboardMobilePreviewController.java`
- `src/main/java/com/brandPitara/sfs/dashboard/project/service/impl/DashboardMobilePreviewServiceImpl.java`
- `src/main/java/com/brandPitara/sfs/project/controller/publicapi/ProjectPublicController.java`
- `src/main/java/com/brandPitara/sfs/project/controller/publicapi/ProjectFloorPlanPublicController.java`
- `src/main/java/com/brandPitara/sfs/project/controller/publicapi/ProjectFloorPlanInsightPublicController.java`
- `src/main/java/com/brandPitara/sfs/dashboard/project/controller/DashboardProjectFloorPlanRoomDimensionController.java`
- `src/main/java/com/brandPitara/sfs/dashboard/project/controller/DashboardProjectFloorPlanInsightController.java`

## Current-State Answers

1. Dashboard creates and updates floor plans through `DashboardProjectFloorPlanController`:
   - `POST /api/dashboard/projects/{projectId}/floor-plans`
   - `PUT /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}`
   Both accept `ProjectFloorPlanUpsertRequest`, run `assertCurrentUserCanEditProject(projectId)`, call `ProjectFloorPlanService`, and audit the action.

2. `ProjectFloorPlanUpsertRequest` already supports `unitConfigurationType`.

3. `ProjectFloorPlanResponse` already exposes `unitConfigurationType` and `unitConfigurationTypeLabel`. `ProjectFloorPlanMapper` maps the enum and label with `UnitConfigurationType.toLabel()`.

4. Public project detail groups by `unitConfigurationType` first in `ProjectDetailComposerImpl.groupFloorPlans`. The response shape is `floorPlanGroups[].groupKey`, `groupLabel`, and `items`.

5. Backend falls back to parsing only when `ProjectFloorPlanResponse.unitConfigurationType == null`. The parser checks `unitLabel`, then `title`, then `floorCode`.

6. Before this fix, fallback parsing supported only whole-number `1 BHK` through `4 BHK` and returned legacy keys such as `3_BHK`. It did not reliably support `1.5`, `2.5`, `3.5`, `4.5`, or `5+ BHK`. The fix normalizes recognized fallback values to enum-style keys such as `BHK_3_5` and labels such as `3.5 BHK`.

7. Dashboard mobile preview uses the same grouping implementation. `DashboardMobilePreviewServiceImpl` calls `ProjectDetailComposerImpl.composeForPreview`, which calls `buildDashboardPreviewFloorPlanGroups`, then `groupFloorPlans`.

8. Backend metadata exposes unit configuration options through `DashboardProjectMetadataController`.

9. Dashboard should call:
   `GET /api/dashboard/project-metadata/unit-configurations`

10. Dashboard should send `unitConfigurationType` in floor-plan create/update payloads:

```json
{
  "title": "3 BHK Type A",
  "floorCode": "3BHK-A",
  "unitConfigurationType": "BHK_3",
  "imageUrl": "https://cdn.squarefootstory.com/projects/123/floorplans/3bhk-a.webp",
  "carpetAreaSqft": 1180,
  "builtUpAreaSqft": 1450,
  "superAreaSqft": 1550,
  "sortOrder": 1,
  "active": true
}
```

## Contract

Mobile/public pills must come from `floorPlanGroups[].groupLabel`, which is derived from `unitConfigurationType` when present. Card title remains `floorPlanGroups[].items[].title`.

Example grouped response:

```json
{
  "floorPlanGroups": [
    {
      "groupKey": "BHK_3",
      "groupLabel": "3 BHK",
      "items": [
        {
          "title": "3 BHK Type A",
          "unitConfigurationType": "BHK_3",
          "unitConfigurationTypeLabel": "3 BHK"
        }
      ]
    }
  ]
}
```

## Rooms And Insights

Existing dashboard endpoints are present and should be reused:

- `GET/POST/PUT/DELETE /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/rooms`
- `GET/POST/PUT/DELETE /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/insights`

Existing public insight detail is:

- `GET /api/projects/{projectId}/floor-plans/{floorPlanId}/insights`

No duplicate rooms or insights module is needed.
