# Floor Plan Dashboard Configuration Audit

Date: 2026-07-08

## Dashboard Package Search

Dashboard frontend package found at:

`/Users/mac/sfs-dashboard`

Initial backend-repo sibling search did not find it because it is outside `/Users/mac/Desktop/RealEstate`.

Searched expected sibling folders:

- `../sfs-dashboard`
- `../SFS_Dashboard`
- `../dashboard`
- `../RealEstateDashboard`

Also searched nearby `package.json` files up to depth 3. Found:

- `../RealEstate/package.json`: backend API test scripts only, not dashboard UI
- `../Mobile_SFS/SFS_App/package.json`: React Native mobile app, intentionally not touched
- `../SFS_App/package.json`: React Native mobile app, intentionally not touched
- `../eesBackend/frontend/package.json`: `ees-frontend`, not identified as SFS dashboard
- `../eesBackend/package.json`
- `../ai-notes-revision/package.json`
- `../Sofascore-React Native-clone/ScoreVerseWC26/package.json`

## Required Dashboard Behavior

The dashboard Add/Edit Floor Plan form must include a required `Unit Configuration` or `Configuration` selector populated from:

`GET /api/dashboard/project-metadata/unit-configurations`

The selected option value must be sent as `unitConfigurationType` in both create and update payloads.

## Answers

1. The Add Floor Plan form is `src/components/projects/forms/ProjectFloorPlanForm.tsx`.
2. It now calls `GET /api/dashboard/project-metadata/unit-configurations` through `useUnitConfigurations`.
3. Create now sends `unitConfigurationType`.
4. Update now sends `unitConfigurationType`.
5. Edit now prefills the selector from `floorPlan.unitConfigurationType`.
6. The form no longer depends on title/unitLabel for the pill. Title remains the floor-plan variant name.
7. The floor-plan card now shows a configuration badge separately from the title in `src/components/projects/workspace/ProjectFloorPlansTab.tsx`.
8. Multiple cards under the same configuration remain allowed. There is no duplicate `unitConfigurationType` blocking.
9. Room and insight management buttons already existed via `FloorPlanDetailSheet` and remain accessible from each floor-plan card.
10. Changed files:
    - `/Users/mac/sfs-dashboard/src/lib/api/endpoints.ts`
    - `/Users/mac/sfs-dashboard/src/features/projects/projectFloorPlans.types.ts`
    - `/Users/mac/sfs-dashboard/src/features/projects/projectFloorPlans.schemas.ts`
    - `/Users/mac/sfs-dashboard/src/features/projects/projects.types.ts`
    - `/Users/mac/sfs-dashboard/src/features/projects/projects.api.ts`
    - `/Users/mac/sfs-dashboard/src/features/projects/projects.hooks.ts`
    - `/Users/mac/sfs-dashboard/src/components/projects/forms/ProjectFloorPlanForm.tsx`
    - `/Users/mac/sfs-dashboard/src/components/projects/workspace/ProjectFloorPlansTab.tsx`

## Dashboard Payload Requirement

```json
{
  "unitConfigurationType": "BHK_3",
  "title": "3 BHK Type A",
  "floorCode": "3BHK-A",
  "imageUrl": "https://cdn.example.com/floorplans/3bhk-a.webp",
  "carpetAreaSqft": 1180,
  "builtUpAreaSqft": 1450,
  "superAreaSqft": 1550,
  "sortOrder": 1,
  "active": true
}
```

The pill/group is `unitConfigurationType`. The card title is `title`.
