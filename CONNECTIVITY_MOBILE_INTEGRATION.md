# Connectivity Section — Mobile Integration Guide

> For the frontend developer adding the Connectivity Map section to the Project Meter Detail screen (and any other project detail screen).

---

## 1. What this feature is

Each project can have a **Connectivity** section that shows nearby places (transit, schools, hospitals, parks, etc.) stored in the database. These places are saved by the dashboard admin using a backend-only Google Places import tool.

**Critical rule: the mobile app must never call Google Places API directly. It reads only from the backend's saved database.**

---

## 2. Public API endpoint

```
GET /api/projects/{projectId}/connectivity
```

- **Auth:** optional (works for unauthenticated users too, `authMode: "optional"`)
- **Visibility check:** project must be published and active; otherwise returns 404
- **Returns:** full connectivity response including project coordinates, all places grouped by category, and radius config

### Response shape (TypeScript)

```ts
type ProjectConnectivityDto = {
  projectId: number | null;
  title: string | null;           // e.g. "Excellent Connectivity"
  subtitle: string | null;        // e.g. "Near by Connectivity"
  summary: string | null;         // optional marketing copy
  mapImageUrl: string | null;     // fallback static image URL if map unavailable

  projectLatitude: number | null;   // center of the map circle
  projectLongitude: number | null;
  projectAddress: string | null;

  defaultRadiusMeters: number | null; // radius for the map circle (default 3000)
  searchEnabled: boolean;
  active: boolean;                // hide the section entirely when false

  places: ProjectConnectivityPlaceDto[];    // flat list of ALL active places
  categories: ProjectConnectivityCategoryDto[]; // pre-grouped, use this for chips + per-category display
};

type ProjectConnectivityCategoryDto = {
  category: ProjectConnectivityCategory; // enum string
  categoryLabel: string;                 // human-readable label, e.g. "Transit"
  iconKey: string | null;                // lucide/icon key — see §5 below
  count: number;                         // number of places in this category
  places: ProjectConnectivityPlaceDto[]; // sorted: featured first, then sortOrder, then distanceMeters
};

type ProjectConnectivityPlaceDto = {
  id: number;
  projectId: number | null;
  placeName: string;
  placeType: ProjectConnectivityType;    // e.g. "METRO", "SCHOOL"
  placeTypeLabel: string | null;         // e.g. "Metro", "School"
  category: ProjectConnectivityCategory | null;
  categoryLabel: string | null;

  distanceLabel: string | null;          // e.g. "850 m" or "2.3 km"
  distanceMeters: number | null;         // raw number in metres
  durationLabel: string | null;          // e.g. "12 min drive"
  durationSeconds: number | null;

  latitude: number | null;               // for placing a map marker
  longitude: number | null;

  imageUrl: string | null;
  address: string | null;                // formatted address
  externalPlaceId: string | null;        // Google Places ID (reference only, do NOT call Google)
  provider: string | null;               // always "GOOGLE_PLACES" for imported places

  rating: number | null;                 // e.g. 4.2 (from Google at import time)
  userRatingCount: number | null;        // e.g. 1200

  sortOrder: number;
  active: boolean;
  verified: boolean;
  featured: boolean;
};
```

### Example response (abbreviated)

```json
{
  "projectId": 71,
  "title": "Excellent Connectivity",
  "subtitle": "Near by Connectivity",
  "summary": null,
  "mapImageUrl": null,
  "projectLatitude": 28.4595,
  "projectLongitude": 77.0266,
  "projectAddress": "Sector 65, Gurugram, Haryana",
  "defaultRadiusMeters": 3000,
  "searchEnabled": true,
  "active": true,
  "places": [ /* flat list */ ],
  "categories": [
    {
      "category": "TRANSIT",
      "categoryLabel": "Transit",
      "iconKey": "train",
      "count": 4,
      "places": [
        {
          "id": 101,
          "placeName": "Sikanderpur Metro",
          "placeType": "METRO",
          "placeTypeLabel": "Metro",
          "category": "TRANSIT",
          "categoryLabel": "Transit",
          "distanceLabel": "1.2 km",
          "distanceMeters": 1200,
          "durationLabel": null,
          "latitude": 28.4638,
          "longitude": 77.0608,
          "address": "Sikanderpur, Gurugram",
          "rating": 4.3,
          "userRatingCount": 8200,
          "sortOrder": 0,
          "active": true,
          "verified": true,
          "featured": true
        }
      ]
    },
    {
      "category": "SCHOOLS",
      "categoryLabel": "Schools",
      "iconKey": "school",
      "count": 2,
      "places": [ /* ... */ ]
    }
  ]
}
```

---

## 3. Second endpoint — text search (optional, for search bar)

```
GET /api/projects/{projectId}/connectivity/search?query=metro
```

Searches saved places by name or type. Useful if you want a search bar within the section.

```ts
type ProjectConnectivitySearchResponse = {
  projectId: number;
  query: string;
  projectLatitude: number | null;
  projectLongitude: number | null;
  results: ProjectConnectivityPlaceDto[];  // same place shape as above
};
```

---

## 4. Hide / show logic

Show the section only when **all** of these are true:

| Condition | Value needed |
|---|---|
| `data.active` | `true` |
| `data.projectLatitude` | not null |
| `data.projectLongitude` | not null |
| `data.categories.length` | `> 0` |

If any condition fails, render nothing (no empty card).

---

## 5. Category chips

Use `data.categories` (already filtered, sorted, and grouped by the backend).

- Default selected chip: first category in the array (backend returns them in a fixed order)
- Each chip shows: `iconKey` + `categoryLabel` + `count`
- `iconKey` values map to icon library names (Lucide or similar):

| category | iconKey | label |
|---|---|---|
| TRANSIT | `train` | Transit |
| SCHOOLS | `school` | Schools |
| COLLEGES | `graduation-cap` | Colleges |
| HOSPITALS | `hospital` | Hospitals |
| PARKS | `trees` | Parks |
| RETAIL | `store` | Retail Shops |
| MALLS | `shopping-bag` | Malls |
| GYMS | `dumbbell` | Gyms |
| OFFICES | `building` | Offices & IT Parks |
| RESTAURANTS | `utensils` | Restaurants & Cafes |
| BANKS | `banknote` | Banks & ATMs |
| DAILY_NEEDS | `shopping-basket` | Daily Needs |
| LIFESTYLE | `map-pin` | Lifestyle |
| SAFETY | `shield` | Safety |

---

## 6. Map

- **Center:** `projectLatitude`, `projectLongitude`
- **Radius circle:** `defaultRadiusMeters ?? 3000` metres, drawn around the center
- **Markers:** places in the selected category that have `latitude != null && longitude != null`
- **Project pin:** a distinct pin at the center coordinate (optionally labelled with `projectAddress`)
- **Tap a marker:** show `placeName`, `distanceLabel`, `rating`, `address`

### Fallback

If the map cannot be rendered (e.g. no Maps key on the platform), show `data.mapImageUrl` as a static image instead, if it is not null.

### Coordinate delta

A sensible initial map region delta based on the radius:
```ts
const delta = Math.max((radiusMeters / 111000) * 2.8, 0.01);
// latitudeDelta = delta, longitudeDelta = delta
```

---

## 7. Places list under the map

Under the map, show the places for the selected category (from `activeCategory.places`). Suggest showing 5 and a "+N more" label.

Each row: place name · distance label · rating (star icon + number).

---

## 8. All placeType values

These are the full set of `placeType` strings the backend can return:

```
METRO, BUS_STOP, RAILWAY_STATION, AIRPORT, HIGHWAY,
SCHOOL, COLLEGE, UNIVERSITY,
HOSPITAL, CLINIC, PHARMACY,
BUSINESS_HUB, TECH_PARK, OFFICE_HUB, OFFICE_SPACE,
MALL, SUPERMARKET, RESTAURANT, CAFE, MARKET, RETAIL_SHOP, GROCERY_STORE, CONVENIENCE_STORE,
PARK, GYM, FITNESS_CENTER, STADIUM,
POLICE_STATION, FIRE_STATION,
BANK, ATM,
TEMPLE, CHURCH, MOSQUE,
LANDMARK, OTHER
```

---

## 9. Existing hook (ready to use)

`hooks/useProjectConnectivityQuery.ts` already exists and uses `@tanstack/react-query`:

```ts
import { useProjectConnectivityQuery } from "@/hooks/useProjectConnectivityQuery";

const { data, isLoading } = useProjectConnectivityQuery(numericProjectId, isValidProjectId);
// data: ProjectConnectivityDto | undefined
```

- `staleTime`: 10 minutes (no unnecessary refetches)
- `gcTime`: 1 hour
- Disabled when `projectId` is invalid

---

## 10. Service function

`services/projectConnectivityService.ts` needs its types updated before use. The current file has an old minimal DTO shape that is missing most fields. Replace the types with the full `ProjectConnectivityDto` shape from §2 above. The `fetchProjectConnectivity` function and its URL (`/api/projects/${projectId}/connectivity`) are already correct.

---

## 11. Security

- **Do not call Google Places API from the mobile app.**
- **Do not store or reference any Google API key in mobile code.**
- All place data comes from the backend's database (imported by the dashboard admin).
- The `externalPlaceId` field is a Google Places ID for reference only — do not use it to call any Google API.
