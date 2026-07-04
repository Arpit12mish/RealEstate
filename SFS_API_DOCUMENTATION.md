# SFS (Square Foot Story) — Complete API Documentation

> **Version:** Derived from codebase inspection — June 2026  
> **Base URL:** `https://api.squarefootstory.com` (production) | `http://localhost:8080` (local)  
> **Format:** All requests/responses are `application/json` unless noted.  
> **Documentation is accurate to current backend implementation. Do not rely on any previously distributed version.**

---

## Table of Contents

1. [Overview](#1-overview)
2. [Authentication](#2-authentication)
   - [Mobile / App Authentication (OTP)](#21-mobile--app-authentication-otp)
   - [Dashboard Authentication (Email + Password)](#22-dashboard-authentication-email--password)
3. [Dashboard APIs](#3-dashboard-apis)
   - [Overview / Stats](#31-dashboard-overview)
   - [Builders](#32-dashboard-builders)
   - [Projects](#33-dashboard-projects)
   - [Project Review Workflow](#34-project-review-workflow)
   - [Project Media](#35-project-media)
   - [Project Highlights](#36-project-highlights)
   - [Project Floor Plans](#37-project-floor-plans)
   - [Project Connectivity (Manual)](#38-project-connectivity-manual)
   - [Project Connectivity (Google Places Provider)](#39-project-connectivity-google-places-provider)
   - [Project Meter](#310-project-meter)
   - [Project Metadata Endpoints](#311-project-metadata-endpoints)
   - [Media Upload (Presign)](#312-media-upload-presign)
   - [Cities (Dashboard)](#313-cities-dashboard)
   - [Categories (Dashboard)](#314-categories-dashboard)
   - [Review Issues & History](#315-review-issues--history)
   - [Audit Log](#316-audit-log)
   - [Field Help](#317-field-help)
4. [Admin-Scoped APIs (Non-Dashboard Path)](#4-admin-scoped-apis-non-dashboard-path)
   - [Google / SFS Public Reviews](#41-google--sfs-public-reviews-admin)
   - [Brands](#42-brands-admin)
   - [Distributors](#43-distributors-admin)
   - [Calculators — Admin Data Entry](#44-calculators--admin-data-entry)
   - [Project Meter Snapshot (Admin)](#45-project-meter-snapshot-admin)
5. [Public Mobile / Web APIs](#5-public-mobile--web-apis)
   - [Mobile Auth](#51-mobile-auth-public)
   - [Project Browse & Detail](#52-project-browse--detail)
   - [Project Media](#53-project-media-public)
   - [Project Connectivity](#54-project-connectivity-public)
   - [Project Floor Plan Insights](#55-project-floor-plan-insights-public)
   - [Project Meter](#56-project-meter-public)
   - [Project Reviews (Public)](#57-project-reviews-public)
   - [Submit Authenticated Review](#58-submit-authenticated-review)
   - [My Submitted Reviews](#59-my-submitted-reviews)
   - [Public Review Signal](#510-public-review-signal)
   - [Public Project Meter Cards](#511-public-project-meter-cards)
   - [Cities (Public)](#512-cities-public)
   - [Profile](#513-profile)
   - [Project Favorites](#514-project-favorites)
   - [Calculators (Public)](#515-calculators-public)
6. [Review System — Complete Guide](#6-review-system--complete-guide)
7. [Connectivity Provider Guide](#7-connectivity-provider-guide)
8. [Error Handling](#8-error-handling)
9. [Frontend Integration Notes](#9-frontend-integration-notes)
10. [Environment Variables](#10-environment-variables)
11. [Appendix — Enums Reference](#11-appendix--enums-reference)

---

## 1. Overview

### Auth Types

| Type | Used By | Mechanism |
|------|---------|-----------|
| Dashboard JWT | Dashboard frontend (ADMIN, REVIEWER, DATA_ENTRY) | Email + password login → short-lived access token |
| Mobile JWT | React Native app | Phone + OTP login → access token |
| Public (no auth) | Web / app browsing | No token needed |

### Role Hierarchy

| Role | Context | Capabilities |
|------|---------|-------------|
| `ADMIN` | Dashboard | Full access: create, update, delete, publish, approve/reject, audit |
| `REVIEWER` | Dashboard | Review projects, mark field issues, approve/reject |
| `DATA_ENTRY` | Dashboard | Create & update project/builder data, upload media, submit for review |
| Mobile User | App | Browse, favourite, submit reviews (authenticated) |
| Public | Web/App | Browse, read public data — no login required |

### Common Headers

```
Authorization: Bearer <token>
Content-Type: application/json
```

All protected APIs require the `Authorization` header.  
`OPTIONS` requests are always allowed (CORS preflight).

### Pagination Format

All paginated responses follow Spring `Page<T>`:

```json
{
  "content": [...],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

---

## 2. Authentication

### 2.1 Mobile / App Authentication (OTP)

Token: **Mobile JWT** — stored by React Native app.  
Access token TTL: **12 hours** (43 200 000 ms).  
Refresh token TTL: **1 day**.

---

#### POST `/api/auth/request-otp`

**Access:** Public  
**Description:** Sends an OTP to the given phone number.

**Request Body:**
```json
{
  "phoneNumber": "+919876543210"
}
```

**Response `200`:**
```json
{
  "status": "OTP_SENT",
  "message": "OTP sent successfully",
  "resendAfterSeconds": 30
}
```

**Notes:**
- Phone format: international with country code, e.g., `+91XXXXXXXXXX`
- Rate-limited per phone/IP (to be enforced at infra level)

---

#### POST `/api/auth/verify-otp`

**Access:** Public  
**Description:** Verifies OTP and issues access + refresh tokens. Creates user if new.

**Request Body:**
```json
{
  "phoneNumber": "+919876543210",
  "code": "123456",
  "deviceId": "device-uuid-abc123",
  "fcmToken": "firebase-fcm-token-optional"
}
```

**Response `200`:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1...",
  "refreshToken": "uuid-refresh-token",
  "user": {
    "token": "eyJhbGciOiJIUzI1...",
    "userId": 5,
    "email": null,
    "phoneNumber": "+919876543210",
    "role": "USER",
    "verified": true
  },
  "isNewUser": false,
  "onboardingStatus": "COMPLETED",
  "role": "USER",
  "session": { ... }
}
```

**Errors:**
- `400` with `{ "error": "INVALID_OTP" }` — wrong code

---

#### POST `/api/auth/refresh`

**Access:** Public  
**Description:** Exchanges a valid refresh token for a new access token. Old refresh token is revoked; new one issued.

**Request Body:**
```json
{
  "refreshToken": "uuid-refresh-token"
}
```

**Response `200`:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1...",
  "refreshToken": "new-uuid-refresh-token"
}
```

**Errors:**
- `401` — invalid/expired refresh token

---

#### POST `/api/auth/logout`

**Access:** Public (token in body)  
**Description:** Revokes the given refresh token.

**Request Body:**
```json
{
  "refreshToken": "uuid-refresh-token"
}
```

**Response `200`:** `{ "status": "LOGGED_OUT" }`

---

#### POST `/api/auth/logout-all`

**Access:** Public (token in body)  
**Description:** Revokes ALL refresh tokens for the user (all devices).

**Request Body:** same as `/logout`  
**Response `200`:** `{ "status": "ALL_SESSIONS_REVOKED" }`

---

### 2.2 Dashboard Authentication (Email + Password)

Token: **Dashboard JWT** — separate secret from mobile JWT.  
Access token TTL: **15 minutes** (900 000 ms).  
Refresh token TTL: **15 days**.

> **Security note:** Dashboard tokens are issued only to known email accounts (seeded by admin). There is no self-registration endpoint on the dashboard.

---

#### POST `/api/dashboard/auth/login`

**Access:** Public (no token required)

**Request Body:**
```json
{
  "email": "admin@squarefootstory.com",
  "password": "Admin@12345"
}
```

**Response `200`:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1...",
  "refreshToken": "uuid-refresh-token",
  "tokenType": "Bearer",
  "expiresInMs": 900000,
  "user": {
    "id": 1,
    "name": "SFS Admin",
    "email": "admin@squarefootstory.com",
    "role": "ADMIN",
    "active": true
  }
}
```

**Seeded default credentials (dev/staging):**

| Role | Email | Password |
|------|-------|----------|
| ADMIN | admin@squarefootstory.com | Admin@12345 |
| REVIEWER | reviewer@squarefootstory.com | Reviewer@12345 |
| DATA_ENTRY | data@squarefootstory.com | DataEntry@12345 |

**Errors:**
- `401` — wrong email/password

---

#### POST `/api/dashboard/auth/refresh`

**Access:** Public (token in body)

**Request Body:**
```json
{
  "refreshToken": "uuid-refresh-token"
}
```

**Response `200`:** Same structure as `/login`

---

#### POST `/api/dashboard/auth/logout`

**Access:** Public (token in body)

**Request Body:**
```json
{
  "refreshToken": "uuid-refresh-token"
}
```

**Response `200`:** No body (void)

---

#### GET `/api/dashboard/auth/me`

**Access:** Authenticated dashboard user  
**Header:** `Authorization: Bearer <dashboard-access-token>`

**Response `200`:**
```json
{
  "id": 1,
  "name": "SFS Admin",
  "email": "admin@squarefootstory.com",
  "role": "ADMIN",
  "active": true
}
```

---

## 3. Dashboard APIs

> All dashboard APIs require: `Authorization: Bearer <dashboard-access-token>`  
> All dashboard routes are secured by the `dashboardFilterChain` matching `/api/dashboard/**` and `/api/admin/**`.

---

### 3.1 Dashboard Overview

#### GET `/api/dashboard/overview`

**Access:** ADMIN, REVIEWER, DATA_ENTRY  
**Description:** Returns high-level stats — counts of projects, builders, pending reviews, etc.

**Response `200`:** `DashboardOverviewResponse` (counts summary object)

**curl:**
```bash
curl -H "Authorization: Bearer <token>" \
  https://api.squarefootstory.com/api/dashboard/overview
```

---

### 3.2 Dashboard Builders

#### POST `/api/dashboard/builders`

**Access:** ADMIN, DATA_ENTRY  
**Description:** Create a new builder record.

**Request Body:**
```json
{
  "name": "Prestige Group",
  "slug": "prestige-group",
  "description": "One of India's leading real estate developers.",
  "websiteUrl": "https://prestigeconstructions.com",
  "headquartersCity": "Bangalore",
  "establishedYear": 1986,
  "logoUrl": "https://cdn.sfs.com/builders/prestige-logo.jpg",
  "priority": 10,
  "active": true
}
```

**Response `201`:** `BuilderResponse` object

---

#### PUT `/api/dashboard/builders/{builderId}`

**Access:** ADMIN only  
**Description:** Update full builder record.

**Path:** `{builderId}` — builder ID  
**Request Body:** Same as POST  
**Response `200`:** `BuilderResponse`

---

#### GET `/api/dashboard/builders/{builderId}`

**Access:** ADMIN, REVIEWER, DATA_ENTRY  
**Response `200`:** `BuilderResponse`

---

#### GET `/api/dashboard/builders`

**Access:** ADMIN, REVIEWER, DATA_ENTRY  
**Description:** Paginated list with optional filters.

**Query Params:**

| Param | Type | Required | Default | Notes |
|-------|------|----------|---------|-------|
| `published` | Boolean | No | — | Filter by published status |
| `active` | Boolean | No | — | Filter by active status |
| `page` | int | No | 0 | Page number |
| `size` | int | No | 20 | Max 50 |

**Response `200`:** `Page<BuilderResponse>`

---

#### PATCH `/api/dashboard/builders/{builderId}/logo`

**Access:** ADMIN, DATA_ENTRY  
**Description:** Update only the builder logo URL (after presign upload).

**Request Body:**
```json
{
  "logoUrl": "https://cdn.sfs.com/builders/prestige-logo-v2.jpg"
}
```

**Response `200`:** `BuilderResponse`

---

#### PATCH `/api/dashboard/builders/{builderId}/published`

**Access:** ADMIN only

**Query Param:** `value=true` or `value=false`

**Example:**
```bash
curl -X PATCH \
  "https://api.squarefootstory.com/api/dashboard/builders/7/published?value=true" \
  -H "Authorization: Bearer <token>"
```

**Response `200`:** `BuilderResponse`

---

#### DELETE `/api/dashboard/builders/{builderId}`

**Access:** ADMIN only  
**Description:** Soft-deletes the builder.  
**Response `200`:** No body

---

### 3.3 Dashboard Projects

#### POST `/api/dashboard/builders/{builderId}/projects`

**Access:** ADMIN, DATA_ENTRY  
**Description:** Create a new project under a builder. Created with `published=false`, `deleted=false` (draft state).

**Path:** `{builderId}` — the builder this project belongs to.

**Request Body:**
```json
{
  "name": "Prestige Lakeside Habitat",
  "slug": "prestige-lakeside-habitat",
  "description": "Luxury waterfront apartments in Whitefield.",
  "cityId": 3,
  "addressLine": "Whitefield Main Road, Bangalore 560066",
  "latitude": 12.9698,
  "longitude": 77.7499,
  "priceMin": 7500000,
  "priceMax": 15000000,
  "monthlyEmiMin": 45000,
  "monthlyEmiMax": 90000,
  "averagePricePerSqft": 8500,
  "projectStartDate": "2023-04-01",
  "possessionDate": "2026-12-31",
  "reraNumber": "PRM/KA/RERA/1251/309/PR/2021/123456",
  "status": "UNDER_CONSTRUCTION",
  "propertyTypes": ["APARTMENT"],
  "priority": 5,
  "active": true
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `name` | Required, max 180 chars |
| `slug` | Optional, max 120 chars, regex: `^[a-z0-9]+(?:-[a-z0-9]+)*$` |
| `description` | Optional, max 4000 chars |
| `addressLine` | Optional, max 300 chars |
| `latitude` | Optional, -90.0 to 90.0 |
| `longitude` | Optional, -180.0 to 180.0 |
| `priceMin/Max` | Optional, ≥ 0 |
| `monthlyEmiMin/Max` | Optional, ≥ 0 |
| `averagePricePerSqft` | Optional, ≥ 0 |
| `reraNumber` | Optional, max 50 chars |
| `priority` | Optional, 0–9999 |
| `status` | `UPCOMING`, `UNDER_CONSTRUCTION`, `READY_TO_MOVE` |
| `propertyTypes` | Set of `PropertyType` enum values |

**Response `200`:** `ProjectResponse`

---

#### PUT `/api/dashboard/projects/{projectId}`

**Access:** ADMIN, DATA_ENTRY  
**Description:** Full update of project data. DATA_ENTRY ownership check is enforced.

**Request Body:** Same as POST  
**Response `200`:** `ProjectResponse`

---

#### GET `/api/dashboard/projects/{projectId}`

**Access:** ADMIN, REVIEWER, DATA_ENTRY  
**Response `200`:** `ProjectResponse`

---

#### GET `/api/dashboard/projects/{projectId}/workspace`

**Access:** ADMIN, REVIEWER, DATA_ENTRY  
**Description:** Composite endpoint — returns project + all child sections (media, highlights, floor plans, connectivity, master plan, meter, review status) in a single call. Replaces 10+ individual fetches on the edit/review screen.

**Response `200`:** `DashboardProjectWorkspaceResponse`

---

#### GET `/api/dashboard/projects`

**Access:** ADMIN, REVIEWER, DATA_ENTRY

**Query Params:**

| Param | Type | Required | Default | Notes |
|-------|------|----------|---------|-------|
| `builderId` | Long | No | — | Filter by builder |
| `reviewStatus` | ReviewStatus | No | — | `DRAFT`, `PENDING_REVIEW`, `RECHECK`, `APPROVED`, `REJECTED` |
| `page` | int | No | 0 | |
| `size` | int | No | 20 | Max 50 |

**Default sort:** `priority ASC`, then `id DESC`

**Response `200`:** `Page<ProjectResponse>`

---

#### PATCH `/api/dashboard/projects/{projectId}/published`

**Access:** ADMIN only

**Query Param:** `value=true` or `value=false`

**Response `200`:** `ProjectResponse`

---

#### PATCH `/api/dashboard/projects/{projectId}/active`

**Access:** ADMIN only

**Query Param:** `value=true` or `value=false`

**Response `200`:** `ProjectResponse`

---

#### DELETE `/api/dashboard/projects/{projectId}`

**Access:** ADMIN only  
**Description:** Soft-delete project.  
**Response `200`:** No body

---

### 3.4 Project Review Workflow

> Review status lifecycle: `DRAFT` → `PENDING_REVIEW` → `APPROVED` / `REJECTED` / `RECHECK`

#### GET `/api/dashboard/projects/{projectId}/review-status`

**Access:** ADMIN, REVIEWER, DATA_ENTRY  
**Response `200`:** `DashboardProjectReviewResponse` — current `reviewStatus`, reviewer notes, history

---

#### POST `/api/dashboard/projects/{projectId}/submit-review`

**Access:** ADMIN, DATA_ENTRY  
**Description:** DATA_ENTRY submits project for review. Changes status from `DRAFT`/`RECHECK`/`REJECTED` → `PENDING_REVIEW`.

**Request Body (optional):**
```json
{
  "note": "All fields updated per reviewer feedback."
}
```

**Response `200`:** `DashboardProjectReviewResponse`

---

#### POST `/api/dashboard/projects/{projectId}/approve`

**Access:** ADMIN, REVIEWER  
**Description:** Approve the project review. Sets `reviewStatus = APPROVED`.

**Request Body (optional):**
```json
{
  "note": "Verified. RERA number confirmed."
}
```

**Response `200`:** `DashboardProjectReviewResponse`

---

#### POST `/api/dashboard/projects/{projectId}/reject`

**Access:** ADMIN, REVIEWER  
**Description:** Reject the project. Sets `reviewStatus = REJECTED`.

**Request Body (required):**
```json
{
  "note": "RERA number is incorrect. Please fix."
}
```

**Response `200`:** `DashboardProjectReviewResponse`

---

#### POST `/api/dashboard/projects/{projectId}/reopen`

**Access:** ADMIN, REVIEWER  
**Description:** Reopen the project for corrections. Sets `reviewStatus = RECHECK`.

**Request Body (optional):**
```json
{
  "note": "Price data needs update."
}
```

**Response `200`:** `DashboardProjectReviewResponse`

---

### 3.5 Project Media

#### POST `/api/dashboard/projects/{projectId}/media`

**Access:** ADMIN, DATA_ENTRY  
**Description:** Add a media item (image, video, brochure) to a project. `url` must be the `publicUrl` from a prior presign upload.

**Request Body:**
```json
{
  "mediaType": "IMAGE",
  "url": "https://cdn.sfs.com/projects/123/img1.jpg",
  "caption": "Master bedroom view",
  "sortOrder": 1,
  "active": true
}
```

**`mediaType` values:** `IMAGE`, `VIDEO`, `BROCHURE_PDF`, `THREE_D_TOUR`, `VIRTUAL_TOUR`  
*(check `ProjectMediaType` enum for full list)*

**Response `200`:** `ProjectMediaResponse`

---

#### PUT `/api/dashboard/projects/{projectId}/media/{mediaId}`

**Access:** ADMIN, DATA_ENTRY  
**Request Body:** Same as POST  
**Response `200`:** `ProjectMediaResponse`

---

#### GET `/api/dashboard/projects/{projectId}/media`

**Access:** ADMIN, REVIEWER, DATA_ENTRY  
**Description:** Lists all non-deleted media (including inactive).  
**Response `200`:** `List<ProjectMediaResponse>`

---

#### DELETE `/api/dashboard/projects/{projectId}/media/{mediaId}`

**Access:** ADMIN only  
**Response `200`:** No body

---

### 3.6 Project Highlights

**Base path:** `/api/dashboard/projects/{projectId}/highlights`

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| POST | `/` | ADMIN, DATA_ENTRY | Add highlight |
| PUT | `/{highlightId}` | ADMIN, DATA_ENTRY | Update highlight |
| GET | `/` | ADMIN, REVIEWER, DATA_ENTRY | List highlights |
| DELETE | `/{highlightId}` | ADMIN | Soft-delete |

**Highlight Request Body:**
```json
{
  "title": "Rooftop Infinity Pool",
  "subtitle": "Open 24/7",
  "iconKey": "pool",
  "sortOrder": 1,
  "active": true
}
```

**Validation:**

| Field | Rule |
|-------|------|
| `title` | Required, max 150 |
| `subtitle` | Optional, max 300 |
| `iconKey` | Optional, max 80 |
| `sortOrder` | Optional, 0–9999 |

---

### 3.7 Project Floor Plans

**Base path:** `/api/dashboard/projects/{projectId}/floor-plans`

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| POST | `/` | ADMIN, DATA_ENTRY | Create floor plan |
| GET | `/` | ADMIN, REVIEWER, DATA_ENTRY | List floor plans |
| PUT | `/{floorPlanId}` | ADMIN, DATA_ENTRY | Update floor plan |
| PATCH | `/{floorPlanId}/active` | ADMIN | Toggle active flag (`?active=true/false`) |
| DELETE | `/{floorPlanId}` | ADMIN | Soft-delete |

**Floor Plan Request Body (full intelligence fields):**
```json
{
  "title": "2 BHK - Type A",
  "floorCode": "2BHK-A",
  "imageUrl": "https://cdn.sfs.com/projects/123/floorplan-2bhk-a.jpg",
  "carpetArea": "950 sq.ft.",
  "exclusiveArea": "120 sq.ft.",
  "superArea": "1180 sq.ft.",
  "unitLabel": "2 BHK",
  "description": "Well-ventilated 2 BHK with east-facing balcony.",
  "sortOrder": 1,
  "active": true,
  "unitConfigurationType": "BHK_2",
  "price": 8500000,
  "saleableAreaSqft": 980.50,
  "carpetAreaSqft": 950.00,
  "builtUpAreaSqft": 1050.00,
  "superAreaSqft": 1180.00,
  "floorHeightMeters": 3.05,
  "carpetEfficiencyPercent": 80.5,
  "bedrooms": 2,
  "bathrooms": 2,
  "balconies": 1,
  "facing": "East",
  "directionSummary": "East-facing, opens to garden view",
  "towerName": "Tower A",
  "floorRange": "5th to 15th",
  "keyPlanImageUrl": "https://cdn.sfs.com/projects/123/keyplan.jpg",
  "featured": false
}
```

**Validation:** `title` required, max 150; `imageUrl` required, max 500; all numeric values ≥ 0; `carpetEfficiencyPercent` 0–100; `bedrooms/bathrooms/balconies` 0–99; `sortOrder` 0–9999.

---

### 3.8 Project Connectivity (Manual)

**Base path:** `/api/dashboard/projects/{projectId}/connectivity`

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| PUT | `/` | ADMIN, DATA_ENTRY | Upsert connectivity overview (title, subtitle, map image) |
| GET | `/` | ADMIN, REVIEWER, DATA_ENTRY | Get connectivity overview + places |
| GET | `/places` | ADMIN, REVIEWER, DATA_ENTRY | List nearby places |
| POST | `/places` | ADMIN, DATA_ENTRY | Add a place manually |
| PUT | `/places/{placeId}` | ADMIN, DATA_ENTRY | Update a place |
| DELETE | `/places/{placeId}` | ADMIN | Soft-delete place |

**Place Upsert Request Body:**
```json
{
  "placeName": "Whitefield Railway Station",
  "placeType": "METRO_STATION",
  "category": "TRANSIT",
  "distanceLabel": "1.2 km",
  "distanceMeters": 1200,
  "durationLabel": null,
  "durationSeconds": null,
  "latitude": 12.9701,
  "longitude": 77.7501,
  "imageUrl": null,
  "address": "Whitefield, Bangalore",
  "sortOrder": 1,
  "active": true,
  "verified": true,
  "featured": false
}
```

> **Note:** `durationLabel` and `durationSeconds` are always `null` until the Google Routes API is integrated. Frontend should display distance only until then.

---

### 3.8.1 Project Master Plan

Project Master Plan is a project-level visual section for the mobile project detail screen. It is managed separately from Project Meter and appears as `masterPlan` inside project detail and dashboard mobile preview responses.

**Base path:** `/api/dashboard/projects/{projectId}/master-plan`

> **DATA_ENTRY permission exception:** Master Plan is intentionally relaxed compared with normal project editing. `DATA_ENTRY` users can upload and update Master Plan data for any non-deleted project, including approved projects and projects they did not create. They still cannot activate/deactivate or delete Master Plan records. Other project sections such as basic details, media, floor plans, connectivity, meter, and highlights continue to enforce the normal DATA_ENTRY ownership/status editability policy.

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/` | ADMIN, REVIEWER, DATA_ENTRY | Get the current non-deleted master plan, or `null` if not configured |
| PUT | `/` | ADMIN, DATA_ENTRY | Create or update the single master plan for the project |
| PATCH | `/active?active=true` | ADMIN | Toggle public/mobile visibility |
| DELETE | `/` | ADMIN | Soft-delete the master plan |

**PUT request body:**
```json
{
  "title": "Master Plan",
  "subtitle": "Site layout, towers & open spaces",
  "description": "Approved site layout with towers, parks and internal roads.",
  "masterPlanImageUrl": "https://cdn.sfs.com/projects/42/master-plan.webp",
  "imageCaption": "Approved project master layout",
  "imageAltText": "Master plan layout",
  "totalUnits": 1520,
  "parkAreaValue": 2.70,
  "parkAreaUnit": "ACRE",
  "totalTowers": 18,
  "totalFloors": 19,
  "waterSource": "BWSSB",
  "parkingType": "BASEMENT",
  "openSpacePercent": 65,
  "greenCoveragePercent": 35,
  "verified": true,
  "sourceLabel": "Builder Disclosure",
  "sourceDocumentUrl": "https://cdn.sfs.com/projects/42/approved-layout.pdf",
  "lastVerifiedAt": "2026-06-20T10:30:00Z",
  "remarks": "Internal dashboard note",
  "active": true
}
```

**Enums:**
- `MasterPlanAreaUnit`: `SQ_FT`, `SQ_MT`, `ACRE`, `HECTARE`
- `ParkingType`: `OPEN`, `COVERED`, `BASEMENT`, `STILT`, `MECHANICAL`, `MIXED`, `NOT_DISCLOSED`
- `MasterPlanApprovalStatus`: `DRAFT`, `SUBMITTED`, `VERIFIED`, `NEEDS_REVIEW`, `NOT_AVAILABLE`

**Validation:** title max 150; subtitle max 300; description max 2000; image/source URLs max 500; counts and area values must be non-negative; percentages must be 0–100; waterSource max 120; sourceLabel max 180; remarks max 1000.

Public/mobile output omits dashboard-only fields such as `remarks`, `sourceDocumentUrl`, `active`, and internal IDs.

---

### 3.9 Project Connectivity (Google Places Provider)

These endpoints are under `/api/admin/projects/` (not `/api/dashboard/`).

---

#### GET `/api/admin/projects/connectivity/provider-categories`

**Access:** ADMIN, DATA_ENTRY  
**Description:** Returns all supported connectivity categories with metadata for the search UI.

**Response `200`:** `List<ConnectivityProviderCategoryMetaResponse>`

```json
[
  { "category": "TRANSIT", "label": "Transit", "iconKey": "train", "types": [...] },
  { "category": "SCHOOLS", "label": "Schools", "iconKey": "school", "types": [...] }
]
```

---

#### POST `/api/admin/projects/{projectId}/connectivity/provider-search`

**Access:** ADMIN, DATA_ENTRY  
**Description:** Searches Google Places for nearby places of a given category near the project. Results are **preview-only** — not saved until bulk-save is called.

**Request Body:**
```json
{
  "category": "TRANSIT",
  "radiusMeters": 5000,
  "maxResults": 20
}
```

**Response `200`:** `ConnectivityProviderSearchResponse`

```json
{
  "results": [
    {
      "externalPlaceId": "ChIJXXX",
      "name": "Whitefield Metro Station",
      "address": "Whitefield, Bangalore",
      "latitude": 12.9701,
      "longitude": 77.7501,
      "distanceMeters": 1200,
      "distanceLabel": "1.2 km",
      "durationSeconds": null,
      "durationLabel": null,
      "rating": 4.2,
      "userRatingCount": 1500,
      "alreadySaved": false
    }
  ]
}
```

**`alreadySaved: true`** means this place is already linked to this project. Frontend should show a "saved" badge.

**Limitations:**
- Distance is straight-line Haversine, not driving distance
- `durationSeconds`/`durationLabel` are always `null` (Routes API not yet integrated)
- Max radius: 10 000 m (configured in `app.yaml`)
- Max results: 20 per search

---

#### POST `/api/admin/projects/{projectId}/connectivity/places/bulk`

**Access:** ADMIN, DATA_ENTRY  
**Description:** Saves selected places from provider search to the project. Duplicate prevention: if a place with the same `externalPlaceId` already exists for this project, it is skipped.

**Request Body:**
```json
{
  "places": [
    {
      "externalPlaceId": "ChIJXXX",
      "name": "Whitefield Metro Station",
      "category": "TRANSIT",
      "distanceMeters": 1200,
      "distanceLabel": "1.2 km",
      "latitude": 12.9701,
      "longitude": 77.7501
    }
  ]
}
```

**Response `200`:** `ProjectConnectivityPlaceBulkSaveResponse`

```json
{
  "saved": 2,
  "skipped": 1,
  "total": 3
}
```

**Important rules:**
- `GOOGLE_MAPS_PLACES_ENABLED` env flag must be `true` for provider search to work
- Google API key is backend-only — **never expose to frontend**
- Results are not cached; each provider-search call hits Google API

---

### 3.10 Project Meter

**Base path:** `/api/dashboard/projects/{projectId}/meter`

#### Construction Stages

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/construction-stages` | ADMIN, REVIEWER, DATA_ENTRY | List stages |
| POST | `/construction-stages` | ADMIN, DATA_ENTRY | Create stage |
| PUT | `/construction-stages/{stageId}` | ADMIN, DATA_ENTRY | Update stage |
| DELETE | `/construction-stages/{stageId}` | ADMIN | Delete stage |

**Construction Stage Request:**
```json
{
  "stageName": "Foundation Work",
  "completionPercent": 100,
  "completionDate": "2025-06-30",
  "status": "COMPLETED",
  "description": "RCC foundation laid",
  "sortOrder": 1
}
```

---

#### Compliance Items

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/compliance-items` | ADMIN, REVIEWER, DATA_ENTRY | List items |
| POST | `/compliance-items` | ADMIN, DATA_ENTRY | Create item |
| PUT | `/compliance-items/{itemId}` | ADMIN, DATA_ENTRY | Update item |
| DELETE | `/compliance-items/{itemId}` | ADMIN | Delete item |

**Compliance Item Request:**
```json
{
  "complianceType": "RERA",
  "status": "OBTAINED",
  "description": "RERA registered",
  "certificateUrl": "https://cdn.sfs.com/rera-cert.pdf",
  "validUntil": "2027-12-31"
}
```

---

#### Amenities

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/amenities` | ADMIN, REVIEWER, DATA_ENTRY | List amenities |
| POST | `/amenities` | ADMIN, DATA_ENTRY | Create amenity |
| PUT | `/amenities/{amenityId}` | ADMIN, DATA_ENTRY | Update amenity |
| DELETE | `/amenities/{amenityId}` | ADMIN | Delete amenity |

**Amenity Request (full Intelligence v1 fields):**
```json
{
  "amenityCode": "swimming_pool",
  "amenityLabel": "Swimming Pool",
  "status": "AVAILABLE",
  "progressPercent": 80,
  "weightPercent": 10,
  "displayOrder": 1,
  "remarks": "Olympic-size pool on podium level",
  "verified": true,
  "category": "LIFESTYLE",
  "categoryLabel": "Lifestyle Amenities",
  "iconKey": "pool",
  "rare": false,
  "available": true,
  "publicVisible": true,
  "active": true,
  "categoryDisplayOrder": 1
}
```

**Validation:**

| Field | Rule |
|-------|------|
| `amenityCode` | Required, max 80 |
| `amenityLabel` | Required, max 120 |
| `status` | Required, `ProjectAmenityStatus` enum |
| `progressPercent` | Optional, 0–100 |
| `weightPercent` | Optional, 0–100 |
| `displayOrder` | Required, 0–999 |

> **Important:** Amenities with `available=false` or status `NOT_AVAILABLE` are excluded from the score calculation.

---

#### Price History

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/price-history` | ADMIN, REVIEWER, DATA_ENTRY | List price points |
| POST | `/price-history` | ADMIN, DATA_ENTRY | Create price point |
| PUT | `/price-history/{priceHistoryId}` | ADMIN, DATA_ENTRY | Update price point |
| DELETE | `/price-history/{priceHistoryId}` | ADMIN | Delete price point |

**Request:**
```json
{
  "recordedDate": "2024-01-01",
  "pricePerSqft": 7200,
  "totalPriceMin": 5000000,
  "totalPriceMax": 9000000,
  "notes": "Launch price"
}
```

---

#### Payment Milestones

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/payment-milestones` | ADMIN, REVIEWER, DATA_ENTRY | List milestones |
| POST | `/payment-milestones` | ADMIN, DATA_ENTRY | Create milestone |
| PUT | `/payment-milestones/{milestoneId}` | ADMIN, DATA_ENTRY | Update milestone |
| DELETE | `/payment-milestones/{milestoneId}` | ADMIN | Delete milestone |

**Request:**
```json
{
  "milestoneName": "On Possession",
  "percentageAmount": 20,
  "dueDate": "2026-12-31",
  "description": "Balance 20% due on handover",
  "sortOrder": 5
}
```

---

#### Cost Breakdown

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/cost-breakdown` | ADMIN, REVIEWER, DATA_ENTRY | Get cost breakdown |
| PUT | `/cost-breakdown` | ADMIN, DATA_ENTRY | Upsert cost breakdown |

**Request:**
```json
{
  "basePrice": 8500,
  "stampDutyPercent": 5.0,
  "registrationPercent": 1.0,
  "gstPercent": 5.0,
  "plcCharges": 150000,
  "infrastructureCharges": 200000,
  "maintenanceDeposit": 50000,
  "notes": "GST applicable on under-construction"
}
```

---

#### Land Utilization

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/land-utilization` | ADMIN, REVIEWER, DATA_ENTRY | Get land utilization |
| PUT | `/land-utilization` | ADMIN, DATA_ENTRY | Upsert land utilization |

**Request:**
```json
{
  "totalPlotAreaAcres": 12.5,
  "greenAreaPercent": 70,
  "builtUpAreaPercent": 30,
  "openSpaceAreaPercent": 65,
  "notes": "70% open green space"
}
```

---

#### Location Score

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/location-score` | ADMIN, REVIEWER, DATA_ENTRY | Get location score |
| PUT | `/location-score` | ADMIN, DATA_ENTRY | Upsert location score |

**Request:**
```json
{
  "connectivity": 8,
  "schools": 9,
  "hospitals": 7,
  "markets": 8,
  "parks": 6,
  "businessHubs": 9,
  "futureDevelopment": 7,
  "notes": "Excellent IT hub proximity"
}
```

---

#### Snapshot Recalculate

#### POST `/api/dashboard/projects/{projectId}/meter/snapshot/recalculate`

**Access:** ADMIN, REVIEWER  
**Description:** Triggers recalculation of the project meter snapshot score based on current amenities, construction, and compliance data.

**Response `200`:** `DashboardProjectMeterWriteResponse`

---

### 3.11 Project Metadata Endpoints

These are static/enum metadata endpoints, no database call — fast response.

#### GET `/api/dashboard/project-metadata/property-types`

**Access:** ADMIN, REVIEWER, DATA_ENTRY  
**Response:** `List<PropertyTypeMetaResponse>`

```json
[
  { "value": "APARTMENT", "label": "Apartment", "group": "Residential" },
  { "value": "VILLA", "label": "Villa", "group": "Residential" },
  ...
]
```

---

#### GET `/api/dashboard/project-metadata/unit-configurations`

**Access:** ADMIN, REVIEWER, DATA_ENTRY  
**Response:** `List<UnitConfigurationMetaResponse>`

```json
[
  { "value": "BHK_2", "label": "2 BHK", "group": "Residential" },
  { "value": "STUDIO", "label": "Studio", "group": "Residential" },
  ...
]
```

---

#### GET `/api/dashboard/project-metadata/amenity-categories`

**Access:** ADMIN, REVIEWER, DATA_ENTRY  
**Response:** `List<AmenityCategoryMetaResponse>`

```json
[
  { "value": "LIFESTYLE", "label": "Lifestyle Amenities", "displayOrder": 1 },
  { "value": "SPORTS", "label": "Sports Amenities", "displayOrder": 2 },
  ...
]
```

---

#### GET `/api/dashboard/project-metadata/amenity-suggestions`

**Access:** ADMIN, REVIEWER, DATA_ENTRY  
**Description:** Returns full catalogue of pre-defined amenity suggestions with icon keys and rare badge.

**Response:** `List<AmenitySuggestionResponse>`

```json
[
  {
    "code": "swimming_pool",
    "label": "Swimming Pool",
    "category": "LIFESTYLE",
    "categoryLabel": "Lifestyle Amenities",
    "iconKey": "pool",
    "rare": false
  },
  {
    "code": "rooftop_lounge",
    "label": "Rooftop Lounge",
    "category": "LIFESTYLE",
    "categoryLabel": "Lifestyle Amenities",
    "iconKey": "rooftop",
    "rare": true
  }
]
```

---

### 3.12 Media Upload (Presign)

#### POST `/api/dashboard/media/presign-upload`

**Access:** ADMIN, DATA_ENTRY  
**Description:** Generates a pre-signed S3 URL for direct browser-to-S3 upload. The `publicUrl` and `storageKey` from the response must be saved/passed to the entity creation/update call.

**Request Body:**
```json
{
  "uploadType": "PROJECT_IMAGE",
  "contentType": "image/jpeg",
  "fileSizeBytes": 524288,
  "projectId": 42,
  "builderId": null,
  "cityId": null
}
```

**`uploadType` values:**

| Value | Scope | Notes |
|-------|-------|-------|
| `PROJECT_IMAGE` | Project | Requires `projectId` |
| `FLOOR_PLAN_IMAGE` | Project | Requires `projectId` |
| `MASTER_PLAN_IMAGE` | Project | Requires `projectId`; image only |
| `CONNECTIVITY_MAP` | Project | Requires `projectId` |
| `BROCHURE_PDF` | Project | Requires `projectId`; only PDF allowed |
| `BUILDER_LOGO` | Builder | Requires `builderId` |
| `CITY_COVER_IMAGE` | City | Requires `cityId`; image only |

**Allowed `contentType` values:**
- `image/jpeg`
- `image/jpg`
- `image/png`
- `image/webp`
- `application/pdf` (only for BROCHURE_PDF)

`MASTER_PLAN_IMAGE` uploads are stored under `dashboard/projects/{projectId}/master-plan/{uuid}.{ext}` and use the same 2 MB image limit as other dashboard image uploads.

**Project upload permission rule:**
- `MASTER_PLAN_IMAGE` follows the relaxed Master Plan rule: ADMIN and DATA_ENTRY can presign for any non-deleted project.
- All other project-scoped upload types (`PROJECT_IMAGE`, `FLOOR_PLAN_IMAGE`, `CONNECTIVITY_MAP`, `BROCHURE_PDF`) keep the original project ownership/status checks for DATA_ENTRY users.

**Response `200`:**
```json
{
  "uploadUrl": "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/projects/42/img-uuid.jpg?X-Amz-Signature=...",
  "publicUrl": "https://cdn.sfs.com/projects/42/img-uuid.jpg",
  "storageKey": "projects/42/img-uuid.jpg",
  "expiresInSeconds": 300,
  "requiredHeaders": {
    "Content-Type": "image/jpeg"
  }
}
```

**S3 Upload Flow:**
1. Call presign endpoint → get `uploadUrl` and `requiredHeaders`
2. `PUT` the file bytes directly to `uploadUrl` — must include all `requiredHeaders`
3. Use `publicUrl` as the `imageUrl` / `url` / `logoUrl` / `coverImageUrl` in the entity update

**Common 403 issue:** The `Content-Type` header sent in the S3 PUT must exactly match the `contentType` used in the presign request.

**City cover upload flow:**
1. Create the city without `coverImageUrl`, or select an existing city.
2. Call `POST /api/dashboard/media/presign-upload`:
```json
{
  "uploadType": "CITY_COVER_IMAGE",
  "contentType": "image/webp",
  "fileSizeBytes": 524288,
  "cityId": 7
}
```
3. Upload the file to S3 using `PUT uploadUrl` and all `requiredHeaders`.
4. Save `publicUrl` using `PATCH /api/dashboard/cities/{cityId}/cover-image`, or through `PUT /api/dashboard/cities/{cityId}`.
5. Public APIs expose it as `coverImageUrl` from `GET /api/cities`, `GET /api/public/home`, and `GET /api/public/cities/trending`.

---

### 3.13 Cities (Dashboard)

**Base path:** `/api/dashboard/cities`

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| POST | `/` | ADMIN | Create city |
| PUT | `/{cityId}` | ADMIN | Update city |
| PATCH | `/{cityId}/cover-image` | ADMIN, DATA_ENTRY | Update only city cover image URL after S3 upload |
| DELETE | `/{cityId}` | ADMIN | Delete city |
| GET | `/{cityId}` | ADMIN, REVIEWER, DATA_ENTRY | Get city |
| GET | `/` | ADMIN, REVIEWER, DATA_ENTRY | List cities (optional `?query=` search) |

**City Request:**
```json
{
  "name": "Bangalore",
  "state": "Karnataka",
  "countryCode": "IN",
  "slug": "bangalore",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "coverImageUrl": "https://cdn.sfs.com/cities/bangalore.webp",
  "active": true,
  "homepageFeatured": true,
  "displayOrder": 1,
  "growthPercent": 12.4
}
```

**Update cover image only:**
```http
PATCH /api/dashboard/cities/7/cover-image
```

```json
{
  "coverImageUrl": "https://cdn.squarefootstory.com/dashboard/cities/7/cover/2d41f6f2-8172-4b11-9a4e-650aa45caa7f.webp"
}
```

---

### 3.14 Categories (Dashboard)

**Base path:** `/api/dashboard/categories`

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| POST | `/` | ADMIN | Create category |
| PUT | `/{categoryId}` | ADMIN | Update category |
| DELETE | `/{categoryId}` | ADMIN | Delete category |
| GET | `/{categoryId}` | ADMIN, REVIEWER, DATA_ENTRY | Get category |
| GET | `/` | ADMIN, REVIEWER, DATA_ENTRY | List categories |

---

### 3.15 Review Issues & History

#### POST `/api/dashboard/reviews/field-issues`

**Access:** ADMIN, REVIEWER  
**Description:** Mark a specific field of an entity as WRONG or requiring RECHECK.

**Request Body:**
```json
{
  "entityType": "PROJECT",
  "entityId": 42,
  "fieldKey": "reraNumber",
  "issueType": "WRONG",
  "note": "RERA number format is incorrect"
}
```

**Response `200`:** `FieldReviewIssueResponse`

---

#### GET `/api/dashboard/reviews/field-issues`

**Access:** ADMIN, REVIEWER, DATA_ENTRY

**Query Params:**

| Param | Required | Notes |
|-------|----------|-------|
| `entityType` | Yes | e.g., `PROJECT`, `PROJECT_METER`, etc. |
| `entityId` | Yes | Entity ID |
| `activeOnly` | No (default false) | `true` = only open issues |

**Response `200`:** `List<FieldReviewIssueResponse>`

---

#### DELETE `/api/dashboard/reviews/field-issues/{issueId}`

**Access:** ADMIN, REVIEWER  
**Description:** Delete an issue (for corrections marked by mistake). Audit trail is preserved.

---

#### PATCH `/api/dashboard/reviews/field-issues/{issueId}/fixed`

**Access:** ADMIN, DATA_ENTRY  
**Description:** Mark an issue as fixed after correcting the data.

**Request Body (optional):**
```json
{
  "note": "Updated RERA number as per official portal."
}
```

---

#### GET `/api/dashboard/reviews/history`

**Access:** ADMIN, REVIEWER, DATA_ENTRY

**Query Params:** `entityType`, `entityId`

**Response `200`:** `List<ReviewHistoryResponse>`

---

### 3.16 Audit Log

#### GET `/api/dashboard/audit/projects/{projectId}`

**Access:** ADMIN  
**Query Params:** `page` (default 0), `size` (default 50, max 200)  
**Response `200`:** `Page<DashboardActionAuditEntryDto>`

---

#### GET `/api/dashboard/audit/users/{userId}`

**Access:** ADMIN  
**Query Params:** `page`, `size`  
**Response `200`:** `Page<DashboardActionAuditEntryDto>`

---

#### GET `/api/dashboard/audit`

**Access:** ADMIN  
**Description:** Full audit log with filters.

**Query Params:**

| Param | Type | Required | Notes |
|-------|------|----------|-------|
| `fromDate` | ISO-8601 OffsetDateTime | No | e.g. `2026-01-01T00:00:00Z` |
| `toDate` | ISO-8601 OffsetDateTime | No | |
| `action` | DashboardAuditAction | No | Enum value |
| `entityType` | ReviewEntityType | No | Enum value |
| `userRole` | DashboardRole | No | `ADMIN`, `REVIEWER`, `DATA_ENTRY` |
| `page` | int | No | Default 0 |
| `size` | int | No | Default 50, max 200 |

**Response `200`:** `Page<DashboardActionAuditEntryDto>`

---

### 3.17 Field Help

**Base path:** `/api/dashboard/field-help`

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/?module=PROJECT` | ADMIN, REVIEWER, DATA_ENTRY | List help entries by module |
| GET | `/{module}/{fieldKey}` | ADMIN, REVIEWER, DATA_ENTRY | Get single field help |
| PUT | `/` | ADMIN | Upsert help entry |
| PATCH | `/{id}/active` | ADMIN | Toggle active (`?value=true/false`) |
| DELETE | `/{id}` | ADMIN | Delete entry |

**`DashboardHelpModule` values:** include `PROJECT_BASIC`, `PROJECT_MEDIA`, `PROJECT_FLOOR_PLAN`, `PROJECT_CONNECTIVITY`, `PROJECT_MASTER_PLAN`, project meter modules, `BUILDER`, and others. *(see enum)*

**Upsert Request:**
```json
{
  "module": "PROJECT",
  "fieldKey": "reraNumber",
  "title": "RERA Registration Number",
  "body": "Enter the official RERA number from the state RERA portal. Format: PRM/KA/RERA/...",
  "active": true
}
```

---

## 4. Admin-Scoped APIs (Non-Dashboard Path)

These APIs live under `/api/admin/**` and are protected by the same dashboard filter chain.  
They require a dashboard token but note the path is `/api/admin/` not `/api/dashboard/`.

---

### 4.1 Google / SFS Public Reviews (Admin)

---

#### GET `/api/admin/projects/{projectId}/public-reviews/places/google-search`

**Access:** ADMIN, DATA_ENTRY  
**Description:** Searches Google Places API for project place candidates. Preview only — results are not saved.

**Query Param:** `query` (e.g., `"Prestige Lakeside Habitat Whitefield"`)

**Response `200`:** `GooglePlaceSearchResponse`
```json
{
  "results": [
    {
      "placeId": "ChIJXXX",
      "displayName": "Prestige Lakeside Habitat",
      "address": "Whitefield, Bangalore",
      "rating": 4.2,
      "userRatingCount": 320
    }
  ]
}
```

---

#### POST `/api/admin/projects/{projectId}/public-reviews/places`

**Access:** ADMIN, DATA_ENTRY  
**Description:** Attach a Google Place to this project for review syncing.

**Request Body:**
```json
{
  "googlePlaceId": "ChIJXXXXXXXXXX",
  "placeCategory": "PROJECT_SITE",
  "active": true
}
```

**`placeCategory` values:** `PROJECT_SITE`, `BUILDER_OFFICE`, `SALES_OFFICE`, `SOCIETY`, `CLUBHOUSE`, `OTHER`

**Response `200`:** `PublicReviewPlaceResponse`

---

#### GET `/api/admin/projects/{projectId}/public-reviews/places`

**Access:** ADMIN, DATA_ENTRY  
**Response `200`:** `List<PublicReviewPlaceResponse>`

---

#### POST `/api/admin/projects/{projectId}/public-reviews/places/{reviewPlaceId}/sync-google`

**Access:** ADMIN, DATA_ENTRY  
**Description:** One-time Google review fetch for this place. Protected by `oneTimeFetched` guard — once fetched, this endpoint returns `409 Conflict` to prevent repeated Google API calls.

**Response `200`:** `SyncGooglePublicReviewsResponse`
```json
{
  "newSamples": 5,
  "totalSamples": 5,
  "placeId": "ChIJXXX",
  "syncedAt": "2026-06-01T10:00:00Z"
}
```

**409 Conflict:** Already fetched. Use the existing samples.

---

#### GET `/api/admin/projects/{projectId}/public-reviews/signal`

**Access:** ADMIN, DATA_ENTRY, REVIEWER  
**Description:** Returns admin-visible Google review signal (includes all samples, including hidden ones).

**Response `200`:** `PublicReviewSignalResponse`

---

#### PATCH `/api/admin/public-review-samples/{sampleId}/display-status`

**Access:** ADMIN only  
**Description:** Change the display visibility of a Google review sample.

**Request Body:**
```json
{
  "displayStatus": "APPROVED_PUBLIC"
}
```

**`displayStatus` values:** `INTERNAL_ONLY`, `APPROVED_PUBLIC`, `HIDDEN`

**Response `200`:** `PublicReviewSampleResponse`

---

#### POST `/api/admin/projects/{projectId}/sfs-reviews`

**Access:** ADMIN, DATA_ENTRY  
**Description:** Create an SFS-managed review (site visit, verified buyer, etc.).

**Request Body:**
```json
{
  "rating": 4,
  "reviewerName": "Verified Buyer A",
  "headline": "Excellent construction quality",
  "reviewText": "We visited the site and verified the construction progress personally.",
  "sourceType": "SFS_SITE_VISIT",
  "verificationStatus": "VERIFIED"
}
```

**`sourceType` values:**
- `USER_SUBMITTED` — submitted by app user
- `SFS_VERIFIED_BUYER` — SFS-verified buyer
- `SFS_SITE_VISIT` — SFS site visit report
- `BUILDER_SUBMITTED` — builder-provided review
- `MANUAL_DATA_ENTRY` — manual entry by DATA_ENTRY

**`verificationStatus` values:** `PENDING`, `VERIFIED`, `REJECTED`, `NEEDS_RECHECK`

**Response `201`:** `SfsReviewResponse`

---

#### GET `/api/admin/projects/{projectId}/sfs-reviews`

**Access:** ADMIN, DATA_ENTRY, REVIEWER  
**Description:** Lists all SFS reviews for the project (including pending ones).

**Response `200`:** `List<SfsReviewResponse>`

---

#### PATCH `/api/admin/sfs-reviews/{reviewId}/verification`

**Access:** ADMIN, REVIEWER  
**Description:** Update verification status of an SFS review. Setting `VERIFIED` + appropriate `displayStatus` makes it public.

**Request Body:**
```json
{
  "verificationStatus": "VERIFIED",
  "displayStatus": "APPROVED_PUBLIC",
  "internalNote": "Confirmed via site visit documentation."
}
```

**Response `200`:** `SfsReviewResponse`

---

### 4.2 Brands (Admin)

**Base path:** `/api/admin/brands`  
**Access:** ADMIN only for all write operations

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Create brand |
| PUT | `/{id}` | Update brand |
| PATCH | `/{id}/publish?published=true` | Publish/unpublish |
| DELETE | `/{id}` | Soft-delete |
| GET | `/{id}` | Get brand |
| GET | `/?published=&active=&page=&size=` | List brands (paginated) |

**Brand Request:**
```json
{
  "name": "Asian Paints",
  "slug": "asian-paints",
  "description": "India's leading paint brand.",
  "logoUrl": "https://cdn.sfs.com/brands/asian-paints-logo.jpg",
  "websiteUrl": "https://www.asianpaints.com",
  "priority": 1,
  "active": true
}
```

---

### 4.3 Distributors (Admin)

**Base path:** `/api/admin/distributors`  
**Access:** ADMIN only

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Create distributor |
| PUT | `/{id}` | Update distributor |
| GET | `/{id}` | Get distributor |
| GET | `/?cityId=&active=&page=&size=` | List distributors |
| DELETE | `/{id}` | Soft-delete |

**Distributor Request:**
```json
{
  "name": "Paint World Bangalore",
  "cityId": 3,
  "address": "12th Main, Indiranagar, Bangalore",
  "phone": "+919876543210",
  "active": true
}
```

---

### 4.4 Calculators — Admin Data Entry

These endpoints manage the rule data that backs public calculator APIs.

---

#### Circle Rate Rules

**Base path:** `/api/admin/circle-rates`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Create rule |
| PUT | `/{id}` | Update rule |
| GET | `/` | Get all rules |

---

#### Stamp Duty Rules

**Base path:** `/api/admin/stamp-duty`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Create rule |
| PUT | `/{id}` | Update rule |
| GET | `/` | Get all rules |

---

#### Interior Cost Rules

**Base path:** `/api/admin/interior-cost`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/base-rules` | Create base cost rule |
| POST | `/addon-rules` | Create addon rule |
| GET | `/base-rules` | List base rules |
| GET | `/addon-rules` | List addon rules |

---

### 4.5 Project Meter Snapshot (Admin)

#### POST `/api/admin/project-meter/{projectId}/recalculate-snapshot`

**Access:** Any authenticated dashboard user (no @PreAuthorize — falls back to `authenticated()`)  
**Response `200`:** `"Project meter snapshot recalculated successfully for projectId=42"`

---

#### POST `/api/admin/project-meter/recalculate-all`

**Access:** Same as above  
**Description:** Recalculates snapshots for all published projects.  
**Response `200`:** `"All published project meter snapshots recalculated successfully."`

---

## 5. Public Mobile / Web APIs

These APIs are protected by the `appFilterChain`.  
Most are public (`permitAll`). Authenticated ones require: `Authorization: Bearer <mobile-access-token>`.

---

### 5.1 Mobile Auth (Public)

See [Section 2.1](#21-mobile--app-authentication-otp).

---

### 5.2 Project Browse & Detail

#### GET `/api/projects/{projectId}`

**Access:** Public  
**Description:** Full project detail with aggregated sections (pricing, floor plan groups, connectivity, master plan, amenities, media glimpses).

**Response `200`:** `ProjectPublicResponse` — see structure below.

```json
{
  "id": 42,
  "builderId": 7,
  "builderName": "Prestige Group",
  "builderLogoUrl": "https://cdn.sfs.com/builders/prestige-logo.jpg",
  "name": "Prestige Lakeside Habitat",
  "slug": "prestige-lakeside-habitat",
  "description": "Luxury waterfront apartments...",
  "cityId": 3,
  "cityName": "Bangalore",
  "addressLine": "Whitefield Main Road, Bangalore",
  "latitude": 12.9698,
  "longitude": 77.7499,
  "priceMin": 7500000,
  "priceMax": 15000000,
  "monthlyEmiMin": 45000,
  "monthlyEmiMax": 90000,
  "averagePricePerSqft": 8500,
  "possessionDate": "2026-12-31",
  "reraNumber": "PRM/KA/RERA/...",
  "status": "UNDER_CONSTRUCTION",
  "propertyTypes": ["APARTMENT"],
  "active": true,
  "published": true,
  "priority": 5,
  "coverMediaUrl": "https://cdn.sfs.com/...",
  "coverMediaType": "IMAGE",
  "brochureUrl": null,
  "hasVideo": false,
  "hasImages": true,
  "favoriteCount": 128,
  "isFavorite": false,
  "pricing": { ... },
  "location": { ... },
  "floorPlanGroups": [ ... ],
  "connectivity": { ... },
  "masterPlan": {
    "title": "Master Plan",
    "subtitle": "Site layout, towers & open spaces",
    "description": null,
    "imageUrl": "https://cdn.sfs.com/projects/42/master-plan.webp",
    "imageCaption": "Approved project master layout",
    "imageAltText": "Master plan layout",
    "expandable": true,
    "verified": true,
    "sourceLabel": "Builder Disclosure",
    "lastVerifiedAt": "2026-06-20T10:30:00Z",
    "stats": [
      {
        "key": "TOTAL_UNITS",
        "label": "Total Units",
        "value": "1520",
        "rawValue": 1520,
        "displayOrder": 10
      },
      {
        "key": "PARK_AREA",
        "label": "Park Area",
        "value": "2.7 Acres",
        "rawValue": 2.70,
        "unit": "ACRE",
        "displayOrder": 20
      }
    ]
  },
  "glimpses": [ ... ],
  "amenities": { ... }
}
```

**Notes:**
- Only returns published (`published=true`) projects
- `favoriteCount` is returned for anonymous and authenticated requests.
- `isFavorite` is `false` for anonymous requests and reflects the current mobile user when a valid USER token is sent.
- `masterPlan` is `null` when no active usable master plan exists.
- Frontend should hide the Master Plan section when `masterPlan` is `null`, render `stats` as label/value rows, and open `imageUrl` fullscreen when `expandable=true`.

---

#### GET `/api/projects/browse`

**Access:** Public

**Query Params:**

| Param | Type | Required | Default | Notes |
|-------|------|----------|---------|-------|
| `unitConfigurations` | List<UnitConfigurationType> | No | — | e.g. `unitConfigurations=BHK_2&unitConfigurations=BHK_3` |
| `cityId` | Long | No | — | Filter by city |
| `page` | int | No | 0 | |
| `size` | int | No | 20 | Max 50 |

**Default sort:** `priority ASC`, then `id DESC`

**Response `200`:** `Page<ProjectPublicResponse>`

---

#### GET `/api/projects/feature`

**Access:** Public

**Query Params:**

| Param | Type | Required | Default | Notes |
|-------|------|----------|---------|-------|
| `builderId` | Long | No | — | Filter by builder |
| `page` | int | No | 0 | |
| `size` | int | No | 10 | Max 20 |

**Response `200`:** `Page<ProjectPublicResponse>`

---

### 5.3 Project Media (Public)

#### GET `/api/projects/{projectId}/media`

**Access:** Public  
**Response `200`:** `List<ProjectMediaResponse>`

---

### 5.4 Project Connectivity (Public)

#### GET `/api/projects/{projectId}/connectivity`

**Access:** Public  
**Response `200`:** `ProjectConnectivityResponse` — overview + active, verified places grouped by category.

---

#### GET `/api/projects/{projectId}/connectivity/search`

**Access:** Public

**Query Params:**

| Param | Type | Required | Default | Notes |
|-------|------|----------|---------|-------|
| `query` | String | Yes | — | Search term |
| `radiusMeters` | Integer | No | 5000 | Search radius |

**Response `200`:** `ProjectConnectivitySearchResponse`

---

### 5.5 Project Floor Plan Insights (Public)

#### GET `/api/projects/{projectId}/floor-plans/{floorPlanId}/insights`

**Access:** Public  
**Response `200`:** `ProjectFloorPlanInsightDetailResponse` — full intelligence breakdown for a floor plan.

---

### 5.6 Project Meter (Public)

#### GET `/api/projects/{projectId}/meter-summary`

**Access:** Public  
**Response `200`:** `ProjectMeterSummaryResponse` — aggregated score, key highlights.

---

#### GET `/api/projects/{projectId}/construction-progress`

**Access:** Public  
**Response `200`:** `ProjectConstructionProgressResponse` — construction stages with completion data.

---

#### GET `/api/projects/{projectId}/meter`

**Access:** Public  
**Response `200`:** `ProjectMeterDetailResponse` — full meter detail (construction, compliance, amenities, cost breakdown, land use, location score).

---

### 5.7 Project Reviews (Public)

#### GET `/api/projects/{projectId}/reviews`

**Access:** Public  
**Description:** Returns only SFS reviews with `displayStatus = APPROVED_PUBLIC` and `verificationStatus = VERIFIED`. Pending / rejected / internal-only reviews are hidden.

**Response `200`:** `List<SfsReviewResponse>`

```json
[
  {
    "id": 15,
    "projectId": 42,
    "reviewerName": "Rahul Mehta",
    "rating": 4,
    "headline": "Good project",
    "reviewText": "Premium construction quality.",
    "sourceType": "USER_SUBMITTED",
    "verificationStatus": "VERIFIED",
    "displayStatus": "APPROVED_PUBLIC",
    "submittedByUser": true,
    "createdAt": "2026-05-15T12:00:00Z"
  }
]
```

**Note:** Phone number / user ID is never exposed in this response.

---

### 5.8 Submit Authenticated Review

#### POST `/api/projects/{projectId}/reviews`

**Access:** Authenticated mobile user  
**Header:** `Authorization: Bearer <mobile-access-token>`  
**Response status:** `201 Created`

**Request Body:**
```json
{
  "rating": 4,
  "reviewerName": "Rahul Mehta",
  "headline": "Good project",
  "reviewText": "Premium construction quality."
}
```

**Validation:**

| Field | Rule |
|-------|------|
| `rating` | Required, 1–5 |
| `reviewerName` | Required, max 255 |
| `headline` | Optional, max 300 |
| `reviewText` | Required, max 3000 |

**Do NOT send these fields — backend derives them from token:**
- `userId`
- `phoneNumber`
- `sourceType` (set to `USER_SUBMITTED` automatically)
- `verificationStatus` (set to `PENDING` automatically)
- `displayStatus` (set to `INTERNAL_ONLY` automatically)

**Business Rules:**
- One review per user per project — duplicate returns `409 Conflict`
- New review starts with `verificationStatus = PENDING` and `displayStatus = INTERNAL_ONLY`
- Backend hashes the user's phone number (SHA-256) and stores only the hash — raw phone is never stored in the review table
- Review does NOT appear in public until a REVIEWER/ADMIN sets `verificationStatus = VERIFIED` and `displayStatus = APPROVED_PUBLIC`

**Response `201`:** `SfsReviewResponse`

```json
{
  "id": 99,
  "projectId": 42,
  "reviewerName": "Rahul Mehta",
  "rating": 4,
  "headline": "Good project",
  "reviewText": "Premium construction quality.",
  "sourceType": "USER_SUBMITTED",
  "verificationStatus": "PENDING",
  "displayStatus": "INTERNAL_ONLY",
  "submittedByUser": true,
  "message": "Review submitted. It will appear after moderation.",
  "createdAt": "2026-06-01T10:00:00Z"
}
```

---

### 5.9 My Submitted Reviews

#### GET `/api/profile/submitted-reviews`

**Access:** Authenticated mobile user  
**Header:** `Authorization: Bearer <mobile-access-token>`

**Description:** Returns all reviews submitted by the current user, including pending ones.

**Response `200`:** `List<MySubmittedReviewResponse>`

```json
[
  {
    "reviewId": 99,
    "projectId": 42,
    "projectName": "Prestige Lakeside Habitat",
    "rating": 4,
    "reviewText": "Premium construction quality.",
    "verificationStatus": "PENDING",
    "displayStatus": "INTERNAL_ONLY",
    "submittedAt": "2026-06-01T10:00:00Z"
  }
]
```

---

### 5.10 Public Review Signal

#### GET `/api/projects/{projectId}/public-review-signal`

**Access:** Public  
**Description:** Returns public-facing Google review signal (only `APPROVED_PUBLIC` samples).

**Response `200`:** `PublicReviewSignalResponse`

```json
{
  "targetType": "PROJECT",
  "targetId": 42,
  "sourceType": "GOOGLE",
  "rating": 4.2,
  "userRatingCount": 320,
  "positiveSampleCount": 12,
  "negativeSampleCount": 2,
  "neutralSampleCount": 3,
  "mixedSampleCount": 1,
  "sourceLabel": "Google Reviews",
  "disclaimer": "Reviews sourced from Google. SFS does not endorse them.",
  "lastSyncedAt": "2026-05-20T08:00:00Z",
  "places": [ ... ],
  "samples": [ ... ]
}
```

---

### 5.11 Public Project Meter Cards

#### GET `/api/public/project-meter/cards`

**Access:** Public

**Query Params:**

| Param | Type | Required | Default | Notes |
|-------|------|----------|---------|-------|
| `cityId` | Long | No | — | Filter by city |
| `page` | int | No | 0 | |
| `size` | int | No | 10 | Max 20 |

**Response `200`:** `Page<ProjectMeterCardResponse>`

Each card includes `projectStartDate` and the backward-compatible `startedOn` field, both sourced from core `project.start_date`. Project Meter construction/stage dates remain separate meter data.

---

### 5.12 Cities (Public)

#### GET `/api/cities/**`

**Access:** Public  
**All city GET endpoints are public.**

#### GET `/api/public/cities/trending?limit=10`

**Access:** Public  
Returns image-based active homepage city/location cards. Counts are sourced from approved public projects only.

```json
[
  {
    "id": 7,
    "name": "Mumbai",
    "slug": "mumbai",
    "state": "Maharashtra",
    "countryCode": "IN",
    "coverImageUrl": "https://cdn.sfs.com/cities/mumbai.webp",
    "projectCount": 8420,
    "growthPercent": 12.4,
    "displayOrder": 1,
    "comingSoon": false
  }
]
```

The same cards can be returned inside `GET /api/public/home` as a section:

```json
{
  "type": "TRENDING_CITIES",
  "key": "TRENDING_CITIES",
  "title": "Trending Cities",
  "subtitle": "Hot real estate markets",
  "items": []
}
```

---

### 5.13 Profile

All profile endpoints require **authenticated mobile user**.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/profile` | Get my profile |
| PUT | `/api/profile` | Update my profile |
| POST | `/api/profile/photo/presign` | Get presign URL for profile photo upload |
| POST | `/api/profile/photo/confirm` | Confirm profile photo after S3 upload |
| DELETE | `/api/profile/account` | Delete account (204 No Content) |

**Update Profile Request:**
```json
{
  "displayName": "Rahul Mehta",
  "email": "rahul@example.com",
  "bio": "Real estate enthusiast"
}
```

---

### 5.14 Project Favorites

All favorites endpoints require **authenticated mobile user**.

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/project-favorites/{projectId}/toggle` | Toggle favorite |
| GET | `/api/project-favorites/{projectId}/exists` | Check if project is favorited |
| GET | `/api/project-favorites` | List my favorite projects (paginated) |

**Toggle Response `200`:**
```json
{
  "targetType": "PROJECT",
  "targetId": 42,
  "isFavorite": true,
  "favoriteCount": 129
}
```

#### Public / Mobile Project Card Favorite Contract

All supported public/mobile project-card responses use these exact fields:

```json
{
  "favoriteCount": 0,
  "isFavorite": false
}
```

**Auth behavior:**
- No token: `favoriteCount` is still returned, `isFavorite=false`.
- Valid mobile USER token: `isFavorite=true` only for projects favorited by the current user.
- Invalid or expired token: handled by the normal security filter behavior for that endpoint; public endpoints remain callable without sending a token.
- Favorites are project-only here: backend uses `FavoriteTargetType.PROJECT`.

**Supported public/mobile APIs and project id fields:**

| API / Surface | DTO | Project identifier | Favorite fields | Notes |
|---|---|---:|---|---|
| `GET /api/projects/{projectId}` | `ProjectPublicResponse` | `id` | `favoriteCount`, `isFavorite` | Project detail. |
| `GET /api/projects/browse` | `ProjectPublicResponse` | `id` | `favoriteCount`, `isFavorite` | Paginated project listing. |
| `GET /api/projects/feature` | `ProjectPublicResponse` | `id` | `favoriteCount`, `isFavorite` | Featured projects, optional `builderId`. |
| `GET /api/builders/{builderId}/projects` | `ProjectPublicResponse` | `id` | `favoriteCount`, `isFavorite` | Public builder project listing. |
| `GET /api/public/project-meter/cards` | `ProjectMeterCardResponse` | `projectId` | `favoriteCount`, `isFavorite` | Also used by Home `PROJECT_ANALYTICS`. |
| `GET /api/public/home` section `TOP_PROJECTS` | `ProjectCardDto` | `id` | `favoriteCount`, `isFavorite` | Home project cards. |
| `GET /api/public/home` section `PROJECT_ANALYTICS` | `ProjectMeterCardResponse` | `projectId` | `favoriteCount`, `isFavorite` | Project meter cards. |
| `GET /api/public/home` section `NEARBY_LISTINGS` | `ProjectNearbyListingCardDto` | `projectId` | `favoriteCount`, `isFavorite` | Distance fields are populated only for lat/lng requests. |
| `GET /api/public/home` section `GENERIC_CARDS` | `GenericCardDto` | `refId` only when `itemType=PROJECT` | `favoriteCount`, `isFavorite` | Non-project cards remain `0/false`. |
| `GET /api/public/search?q=m3m` | `SearchItemDto` | `id` only when `entityType=PROJECT` | `favoriteCount`, `isFavorite` | Non-project search items remain `0/false`. |
| `GET /api/public/search/suggest?q=m3m` | `SearchItemDto` inside suggestion sections | `id` only when `entityType=PROJECT` | `favoriteCount`, `isFavorite` | Non-project suggestion items remain `0/false`. |
| `GET /api/public/feed?screen=BUILDER&entityId={builderId}` | `BuilderProjectCardDto` | `id` | `favoriteCount`, `isFavorite` | Builder feed project cards. |
| `GET /api/project-favorites` | `ProjectResponse` | `id` | `favoriteCount`, `isFavorite` | Authenticated user's favorite projects list. |

**Builder feed request contract:**

```http
GET /api/public/feed?screen=BUILDER&entityId=2
```

| Param | Required | Notes |
|---|---:|---|
| `screen` | Yes | Must be `BUILDER`. |
| `entityId` | Yes | Builder id. If missing, backend returns an error: `entityId is required for BUILDER screen`. |
| `cityId` | No | Used for city-specific promo banner rules. |
| `categoryId` | No | Accepted by controller/context; current builder feed sections primarily use `entityId`. |
| `v` | No | Client content version. |

Current builder project feed caveat: the feed config/loader key is `BUILDER_PROJECTS`, but the serialized project section currently uses `type: "TOP_PROJECTS"` and bucket keys such as `PROJECTS_RECENT`, `PROJECTS_LUXURY`, or `PROJECTS_ICONIC` depending on config `param1`.

**Manual examples:**

```bash
curl "http://localhost:8080/api/public/home"
curl "http://localhost:8080/api/public/home?lat=28.6139&lng=77.2090"
curl "http://localhost:8080/api/public/search?q=m3m"
curl "http://localhost:8080/api/public/search/suggest?q=m3m"
curl "http://localhost:8080/api/public/feed?screen=BUILDER&entityId=2"
```

Authenticated favorite toggle:

```bash
curl -X POST "http://localhost:8080/api/project-favorites/29/toggle" \
  -H "Authorization: Bearer <USER_TOKEN>"
```

Then re-call any supported public/mobile card API with the same bearer token:

```bash
curl "http://localhost:8080/api/public/search?q=m3m" \
  -H "Authorization: Bearer <USER_TOKEN>"
```

Expected behavior: the favorited PROJECT item has `isFavorite=true`, and `favoriteCount` reflects the total project favorite count.

**Frontend handling rules:**
- Use `isFavorite=true` to show a filled heart.
- Use `isFavorite=false` to show an outline heart.
- After `POST /api/project-favorites/{projectId}/toggle`, update the local card state optimistically.
- Keep `favoriteCount` in sync by incrementing/decrementing locally, or by applying the toggle response's `favoriteCount`.
- Use the correct project id field for the DTO: `id`, `projectId`, or `refId` as listed above.
- For `SearchItemDto`, only PROJECT items (`entityType=PROJECT`) should show project favorite UI.
- For `GenericCardDto`, only PROJECT items (`itemType=PROJECT`) should show project favorite UI.

**Intentionally unsupported for project favorites:**
- Dashboard/admin responses under `/api/dashboard/**` and `/api/admin/**`.
- Provider portfolio projects.
- Company/architect/designer portfolio projects, including `CompanyProjectCardDto`.
- Project subresources such as floor plans, highlights, connectivity, reviews, meter detail, media, and calculator/config responses.
- Non-project search/generic card items such as builders, companies, brands, cities, categories, and businesses.

**Known caveats:**
- `SearchItemDto` and `GenericCardDto` include `favoriteCount` and `isFavorite` for a stable JSON shape, but non-project items are always `0/false`.
- Builder feed requires `entityId`; `GET /api/public/feed?screen=BUILDER` without `entityId` is invalid.
- Some admin/mobile-preview DTO classes may contain favorite fields because they reuse shared DTO types, but those endpoints are not favorite-aware contracts and should not drive frontend heart UI unless explicitly documented above.

---

### 5.15 Calculators (Public)

---

#### Circle Rate Calculator

**Base path:** `/api/public/circle-rates`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/cities` | Get available cities |
| GET | `/localities?stateName=&cityName=` | Get localities for a city |
| GET | `/property-types?stateName=&cityName=&localityName=` | Get property types for a locality |
| POST | `/calculate` | Calculate circle rate |

**Calculate Request:**
```json
{
  "stateName": "Karnataka",
  "cityName": "Bangalore",
  "localityName": "Whitefield",
  "propertyType": "RESIDENTIAL",
  "areaSqft": 1200.0
}
```

**Calculate Response `200`:** `CircleRateCalculateResponse`

---

#### Stamp Duty Calculator

**Base path:** `/api/public/stamp-duty`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/cities` | Get cities with stamp duty rules |
| GET | `/buyer-types?stateName=&cityName=` | Get buyer types |
| GET | `/property-categories?stateName=&cityName=` | Get property categories |
| POST | `/calculate` | Calculate stamp duty |

**Calculate Request:**
```json
{
  "stateName": "Karnataka",
  "cityName": "Bangalore",
  "propertyValue": 10000000,
  "buyerType": "INDIVIDUAL",
  "propertyCategory": "RESIDENTIAL"
}
```

**Response `200`:** `StampDutyCalculateResponse`

---

#### Interior Cost Calculator

**Base path:** `/api/public/interior-cost`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/cities` | Available cities |
| GET | `/property-types?cityName=` | Property types |
| GET | `/bhk-types?cityName=&propertyType=` | BHK types |
| GET | `/package-types?cityName=&propertyType=&bhkType=` | Package types |
| GET | `/scope-types?cityName=&propertyType=&bhkType=&packageType=` | Scope types |
| GET | `/package-summary?cityName=&propertyType=&bhkType=&scopeType=&area=&areaUnit=` | Package summary |
| POST | `/compare` | Compare packages |
| POST | `/compare-custom` | Custom compare |

**Compare Request:**
```json
{
  "cityName": "Bangalore",
  "propertyType": "APARTMENT",
  "bhkType": "BHK_2",
  "scopeType": "FULL",
  "area": 1000.0,
  "areaUnit": "SQFT",
  "packageTypes": ["BASIC", "STANDARD", "PREMIUM"]
}
```

---

## 6. Review System — Complete Guide

### Review Types

The SFS platform has two separate review systems:

| System | Source | Table | Admin Visible | Public Visible |
|--------|--------|-------|---------------|----------------|
| Google Reviews | Google Places API | `public_review_sample` | Yes | Only `APPROVED_PUBLIC` samples |
| SFS Reviews | Admin-entered / User-submitted | `project_review` | Yes (all) | Only `VERIFIED + APPROVED_PUBLIC` |

---

### SFS Review Lifecycle

```
User submits review → verificationStatus=PENDING, displayStatus=INTERNAL_ONLY
                                    ↓
                         Dashboard shows pending review
                                    ↓
              REVIEWER/ADMIN decides:
              ┌─── VERIFIED → displayStatus=APPROVED_PUBLIC → appears on public API
              ├─── REJECTED → stays INTERNAL_ONLY → hidden from public
              └─── NEEDS_RECHECK → stays INTERNAL_ONLY → returned to queue
```

---

### Google Review Lifecycle

```
Admin attaches Google Place → POST /places
Admin triggers fetch → POST /places/{placeId}/sync-google
                               (one-time only — 409 on repeat)
Backend fetches reviews from Google API
Samples stored with displayStatus=INTERNAL_ONLY
Admin promotes selected samples → PATCH /public-review-samples/{sampleId}/display-status
                                  { "displayStatus": "APPROVED_PUBLIC" }
Public signal shows only APPROVED_PUBLIC samples
```

---

### Phone Privacy Rules

- User's phone number is **hashed (SHA-256)** before storage
- `reviewerPhoneHash` column stores only the hash
- `userPhoneHash` is a secondary hash for duplicate detection
- No API ever returns `phoneNumber` or any hash to the frontend
- Frontend must never send `userId` or `phoneNumber` in review submission

---

### Duplicate Review Rule

- One review per user per project (enforced by database index on `user_id + project_id`)
- Submitting a second review for the same project returns `409 Conflict`
- Frontend should check `GET /api/profile/submitted-reviews` to detect prior submissions

---

### Dashboard Moderation View

- `GET /api/admin/projects/{projectId}/sfs-reviews` returns all reviews including `PENDING`
- `GET /api/admin/projects/{projectId}/public-reviews/signal` returns admin view of Google signal with all samples
- Public endpoints only show approved content

---

## 7. Connectivity Provider Guide

### Architecture

```
React Native / Dashboard
         |
         | POST /api/admin/projects/{id}/connectivity/provider-search
         ↓
SFS Backend → Google Places API (server-to-server)
         |
         | Returns preview results to UI
         ↓
User selects places in dashboard
         |
         | POST /api/admin/projects/{id}/connectivity/places/bulk
         ↓
SFS Backend stores selected places
         |
         | GET /api/projects/{id}/connectivity (public)
         ↓
React Native reads from SFS backend only
```

### Important Rules

1. **Google API key is NEVER sent to frontend.** All Google API calls are server-side.
2. React Native app must call `GET /api/projects/{projectId}/connectivity` from SFS backend — never call Google Maps or Places API directly.
3. Provider search results are preview-only — they are not saved until bulk-save is called.
4. Duplicate prevention: if an `externalPlaceId` is already saved for a project, bulk-save skips it and reports `skipped` count.
5. Distance values are straight-line Haversine distance — not driving distance.
6. `durationSeconds` and `durationLabel` are always `null` in current implementation (Google Routes API integration is pending).
7. Feature flag `GOOGLE_MAPS_PLACES_ENABLED` must be set to `true` in production.
8. Max radius per search: 10 000 meters. Max results: 20.

### Categories

| Category | Label | Icon Key |
|----------|-------|----------|
| `TRANSIT` | Transit | train |
| `SCHOOLS` | Schools | school |
| `COLLEGES` | Colleges | graduation-cap |
| `HOSPITALS` | Hospitals | hospital |
| `PARKS` | Parks | trees |
| `RETAIL` | Retail Shops | store |
| `MALLS` | Malls | shopping-bag |
| `GYMS` | Gyms | dumbbell |
| `OFFICES` | Offices & IT Parks | building |
| `RESTAURANTS` | Restaurants & Cafes | utensils |
| `BANKS` | Banks & ATMs | banknote |
| `DAILY_NEEDS` | Daily Needs | shopping-basket |
| `LIFESTYLE` | Lifestyle | map-pin |
| `SAFETY` | Safety | shield |

---

## 8. Error Handling

All API errors follow this standard `ApiError` format:

```json
{
  "timestamp": "2026-06-01T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/dashboard/projects",
  "requestId": "uuid-or-correlation-id",
  "validationErrors": {
    "name": "must not be blank",
    "latitude": "Latitude must be between -90 and 90"
  }
}
```

`validationErrors` is only present for `400 Validation failed` responses.

### HTTP Status Codes

| Status | Condition |
|--------|-----------|
| `200` | Success |
| `201` | Created (POST that creates a resource) |
| `204` | No Content (DELETE operations) |
| `400` | Validation failed or illegal argument |
| `401` | Not authenticated (missing/expired token) |
| `403` | Authenticated but insufficient role/permission |
| `404` | Resource not found |
| `405` | HTTP method not allowed |
| `409` | Conflict (duplicate record, constraint violation, or one-time fetch already done) |
| `500` | Unexpected server error |

### Common Error Scenarios

| Scenario | HTTP | Message |
|----------|------|---------|
| Missing Authorization header | 401 | "Unauthorized" |
| Expired access token | 401 | "Unauthorized" |
| Wrong role | 403 | "Access denied" |
| Project not found | 404 | "Project not found" |
| Duplicate review | 409 | "Constraint violation" |
| Google sync already done | 409 | "Already synced" |
| Validation failure | 400 | "Validation failed" + field map |

---

## 9. Frontend Integration Notes

### Dashboard Frontend

1. **Token refresh:** Access token expires in 15 minutes. Dashboard should silently refresh using `POST /api/dashboard/auth/refresh` before expiry. Watch for `401` responses and refresh on failure.
2. **Workspace endpoint:** Use `GET /api/dashboard/projects/{id}/workspace` to load the full edit screen in one call instead of multiple individual requests.
3. **Media upload flow:** Always call presign first → upload to S3 with exact headers → use `publicUrl` in entity fields.
4. **Role gating:** Use the role from `GET /api/dashboard/auth/me` to show/hide UI actions (e.g., hide delete buttons from DATA_ENTRY).
5. **Review status badge mapping:**
   - `DRAFT` → grey badge
   - `PENDING_REVIEW` → yellow badge
   - `APPROVED` → green badge
   - `REJECTED` → red badge
   - `RECHECK` → orange badge

### React Native App

1. **Auth:** Store `accessToken` and `refreshToken` in secure storage. Refresh on 401.
2. **Never call Google APIs directly.** All map/places data comes from SFS backend.
3. **Review submission:** Do NOT include userId, phone, or any status fields. Only send `rating`, `reviewerName`, `headline`, `reviewText`.
4. **Favorites:** Supported project card/detail responses always include `favoriteCount` and `isFavorite`. Without a token, `isFavorite=false`; with a valid mobile USER token, it reflects the current user's favorite state.
5. **Public project page:** Load `GET /api/projects/{id}` first (full composite response), then optionally load meter, reviews, and connectivity asynchronously.

### Token Handling

```
Mobile app receives:
  accessToken  — short-lived JWT (12h), send in Authorization header
  refreshToken — long-lived UUID, send in body to /auth/refresh
  
Dashboard app receives:
  accessToken  — very short-lived JWT (15min), refresh often
  refreshToken — long-lived (15 days)
```

---

## 10. Environment Variables

> Variables marked `(secret)` must never be committed to source control or logged.

### JWT

| Variable | Description |
|----------|-------------|
| `jwt.secret` | Mobile/app JWT signing secret `(secret)` |
| `jwt.expiration.ms` | Mobile access token TTL (default: 43200000 — 12h) |
| `jwt.refresh.expiration.days` | Mobile refresh token TTL (default: 1 day) |
| `dashboard.jwt.secret` | Dashboard JWT signing secret `(secret)` |
| `dashboard.jwt.access-expiration-ms` | Dashboard access token TTL (default: 900000 — 15min) |
| `dashboard.refresh.expiration-days` | Dashboard refresh token TTL (default: 15 days) |

### Database

| Variable | Description |
|----------|-------------|
| `spring.datasource.url` | PostgreSQL JDBC URL |
| `spring.datasource.username` | DB user |
| `spring.datasource.password` | DB password `(secret)` |

### Google APIs

| Variable | Description |
|----------|-------------|
| `GOOGLE_PLACES_API_KEY` | Google Places API key `(secret)` — backend-only, never in frontend |
| `GOOGLE_MAPS_PLACES_ENABLED` | Feature flag: `true` enables provider search (default: `false`) |
| `google.maps.places.max-results` | Max places returned per search (default: 20) |
| `google.maps.places.max-radius-meters` | Max search radius (default: 10000) |

> **Security:** The Google API key is never returned to any frontend. All Google API calls are server-to-server.

### AWS / S3

| Variable | Description |
|----------|-------------|
| `aws.credentials.access-key` | AWS access key `(secret)` |
| `aws.credentials.secret-key` | AWS secret key `(secret)` |
| `app.media.s3.bucket` | S3 bucket name (default: `sfs-s3bucket`) |
| `app.media.s3.region` | AWS region (default: `ap-south-1`) |
| `app.media.s3.publicBaseUrl` | Public CDN base URL for media |
| `app.media.s3.presignExpirySeconds` | Presign URL TTL (default: 300s) |

### SMS / OTP

| Variable | Description |
|----------|-------------|
| `twilio.accountSid` | Twilio account SID `(secret)` |
| `twilio.authToken` | Twilio auth token `(secret)` |
| `twilio.verifyServiceSid` | Twilio verify service SID `(secret)` |
| `msg91.authKey` | MSG91 auth key `(secret)` |
| `msg91.widgetId` | MSG91 widget ID |
| `otp.provider` | OTP provider: `twilio` or `msg91` |
| `app.review.fixed-otp` | Fixed OTP for dev/test environments |

### Dashboard Seed Users

| Variable | Description |
|----------|-------------|
| `dashboard.seed.enabled` | Enables seed users on startup |
| `dashboard.seed.admin.email` | Admin seed email |
| `dashboard.seed.admin.password` | Admin seed password `(secret)` |
| `dashboard.seed.reviewer.email` | Reviewer seed email |
| `dashboard.seed.data-entry.email` | DATA_ENTRY seed email |

---

## 11. Appendix — Enums Reference

### ProjectStatus

| Value | Description |
|-------|-------------|
| `UPCOMING` | Pre-launch / announced |
| `UNDER_CONSTRUCTION` | Active construction |
| `READY_TO_MOVE` | Completed, habitable |

---

### ReviewStatus (Dashboard project review state)

| Value | Who can change to this | Description |
|-------|------------------------|-------------|
| `DRAFT` | System (on creation) | Default state — data being prepared |
| `PENDING_REVIEW` | DATA_ENTRY (submit-review) | Submitted for reviewer attention |
| `RECHECK` | ADMIN, REVIEWER (reopen) | Reviewer flagged for corrections |
| `APPROVED` | ADMIN, REVIEWER (approve) | Approved; eligible for publishing |
| `REJECTED` | ADMIN, REVIEWER (reject) | Rejected; needs rework |

---

### SfsReviewVerificationStatus

| Value | Meaning |
|-------|---------|
| `PENDING` | Newly submitted, not yet reviewed |
| `VERIFIED` | Verified by SFS reviewer |
| `REJECTED` | Rejected — will not go public |
| `NEEDS_RECHECK` | Needs additional verification |

---

### PublicReviewDisplayStatus

| Value | Public visibility |
|-------|------------------|
| `INTERNAL_ONLY` | Dashboard only — hidden from public APIs |
| `APPROVED_PUBLIC` | Visible on public review endpoints |
| `HIDDEN` | Explicitly hidden from all views |

---

### SfsReviewSourceType

| Value | Description |
|-------|-------------|
| `USER_SUBMITTED` | Submitted by authenticated app user |
| `SFS_VERIFIED_BUYER` | SFS-verified actual buyer |
| `SFS_SITE_VISIT` | SFS team site visit report |
| `BUILDER_SUBMITTED` | Provided by builder |
| `MANUAL_DATA_ENTRY` | Manually entered by dashboard DATA_ENTRY |

---

### PropertyType

```
Residential: RESIDENTIAL, APARTMENT, STUDIO, VILLA, INDEPENDENT_HOUSE, BUILDER_FLOOR,
             ROW_HOUSE, PENTHOUSE, DUPLEX, FARMHOUSE
Land/Plot:   PLOT, RESIDENTIAL_PLOT, COMMERCIAL_PLOT, AGRICULTURAL_LAND
Commercial:  COMMERCIAL, OFFICE_SPACE, RETAIL_SHOP, SHOWROOM, FOOD_COURT, CO_WORKING_SPACE
Mixed Use:   MIXED_USE, SERVICED_APARTMENT, HOTEL, RESORT
Industrial:  INDUSTRIAL, WAREHOUSE, FACTORY, INSTITUTIONAL
Other:       OTHER
```

---

### UnitConfigurationType

```
Residential: STUDIO, BHK_1, BHK_1_5, BHK_2, BHK_2_5, BHK_3, BHK_3_5, BHK_4, BHK_4_5,
             BHK_5, BHK_5_PLUS
Commercial:  OFFICE, RETAIL
Land:        PLOT
Other:       OTHER
```

---

### ProjectAmenityCategory

```
LIFESTYLE, SPORTS, WELLNESS, SECURITY, CONVENIENCE, COMMUNITY,
KIDS_FAMILY, GREEN_SUSTAINABILITY, NATURAL, PARKING_TRANSPORT,
PET_FRIENDLY, COMMERCIAL_RETAIL, OTHER
```

---

### ProjectConnectivityCategory

```
TRANSIT, SCHOOLS, COLLEGES, HOSPITALS, PARKS, RETAIL, MALLS,
GYMS, OFFICES, RESTAURANTS, BANKS, DAILY_NEEDS, LIFESTYLE, SAFETY, SEARCH
```

---

### DashboardMediaUploadType

| Value | Allowed Content-Type | Scope |
|-------|---------------------|-------|
| `PROJECT_IMAGE` | image/jpeg, image/jpg, image/png, image/webp | Project (`projectId` required) |
| `FLOOR_PLAN_IMAGE` | image/* | Project |
| `CONNECTIVITY_MAP` | image/* | Project |
| `BROCHURE_PDF` | application/pdf | Project |
| `BUILDER_LOGO` | image/* | Builder (`builderId` required) |

---

### PublicReviewPlaceCategory

`PROJECT_SITE`, `BUILDER_OFFICE`, `SALES_OFFICE`, `SOCIETY`, `CLUBHOUSE`, `OTHER`

---

### DashboardRole

`ADMIN`, `REVIEWER`, `DATA_ENTRY`

---

## Quick Reference — All Controller Endpoints

### Dashboard (`/api/dashboard/**`)

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | /api/dashboard/overview | All roles | Dashboard stats |
| POST | /api/dashboard/auth/login | Public | Dashboard login |
| POST | /api/dashboard/auth/refresh | Public | Refresh dashboard token |
| POST | /api/dashboard/auth/logout | Public | Logout |
| GET | /api/dashboard/auth/me | All roles | Current user |
| POST | /api/dashboard/builders | ADMIN, DE | Create builder |
| PUT | /api/dashboard/builders/{id} | ADMIN | Update builder |
| GET | /api/dashboard/builders/{id} | All roles | Get builder |
| GET | /api/dashboard/builders | All roles | List builders |
| PATCH | /api/dashboard/builders/{id}/logo | ADMIN, DE | Update logo |
| PATCH | /api/dashboard/builders/{id}/published | ADMIN | Publish toggle |
| DELETE | /api/dashboard/builders/{id} | ADMIN | Delete |
| POST | /api/dashboard/builders/{id}/projects | ADMIN, DE | Create project |
| PUT | /api/dashboard/projects/{id} | ADMIN, DE | Update project |
| GET | /api/dashboard/projects/{id} | All roles | Get project |
| GET | /api/dashboard/projects/{id}/workspace | All roles | Workspace composite |
| GET | /api/dashboard/projects | All roles | List projects |
| PATCH | /api/dashboard/projects/{id}/published | ADMIN | Publish toggle |
| PATCH | /api/dashboard/projects/{id}/active | ADMIN | Active toggle |
| DELETE | /api/dashboard/projects/{id} | ADMIN | Delete |
| GET | /api/dashboard/projects/{id}/review-status | All roles | Review state |
| POST | /api/dashboard/projects/{id}/submit-review | ADMIN, DE | Submit for review |
| POST | /api/dashboard/projects/{id}/approve | ADMIN, REV | Approve |
| POST | /api/dashboard/projects/{id}/reject | ADMIN, REV | Reject |
| POST | /api/dashboard/projects/{id}/reopen | ADMIN, REV | Reopen |
| POST | /api/dashboard/projects/{id}/media | ADMIN, DE | Add media |
| PUT | /api/dashboard/projects/{id}/media/{mid} | ADMIN, DE | Update media |
| GET | /api/dashboard/projects/{id}/media | All roles | List media |
| DELETE | /api/dashboard/projects/{id}/media/{mid} | ADMIN | Delete media |
| POST | /api/dashboard/projects/{id}/highlights | ADMIN, DE | Add highlight |
| PUT | /api/dashboard/projects/{id}/highlights/{hid} | ADMIN, DE | Update |
| GET | /api/dashboard/projects/{id}/highlights | All roles | List |
| DELETE | /api/dashboard/projects/{id}/highlights/{hid} | ADMIN | Delete |
| POST | /api/dashboard/projects/{id}/floor-plans | ADMIN, DE | Create floor plan |
| GET | /api/dashboard/projects/{id}/floor-plans | All roles | List |
| PUT | /api/dashboard/projects/{id}/floor-plans/{fid} | ADMIN, DE | Update |
| PATCH | /api/dashboard/projects/{id}/floor-plans/{fid}/active | ADMIN | Toggle active |
| DELETE | /api/dashboard/projects/{id}/floor-plans/{fid} | ADMIN | Delete |
| PUT | /api/dashboard/projects/{id}/connectivity | ADMIN, DE | Upsert overview |
| GET | /api/dashboard/projects/{id}/connectivity | All roles | Get connectivity |
| GET | /api/dashboard/projects/{id}/connectivity/places | All roles | List places |
| POST | /api/dashboard/projects/{id}/connectivity/places | ADMIN, DE | Add place |
| PUT | /api/dashboard/projects/{id}/connectivity/places/{pid} | ADMIN, DE | Update |
| DELETE | /api/dashboard/projects/{id}/connectivity/places/{pid} | ADMIN | Delete |
| GET | /api/dashboard/projects/{id}/meter/construction-stages | All roles | List stages |
| POST | /api/dashboard/projects/{id}/meter/construction-stages | ADMIN, DE | Create |
| PUT | /api/dashboard/projects/{id}/meter/construction-stages/{sid} | ADMIN, DE | Update |
| DELETE | /api/dashboard/projects/{id}/meter/construction-stages/{sid} | ADMIN | Delete |
| GET | /api/dashboard/projects/{id}/meter/compliance-items | All roles | List |
| POST | /api/dashboard/projects/{id}/meter/compliance-items | ADMIN, DE | Create |
| PUT | /api/dashboard/projects/{id}/meter/compliance-items/{cid} | ADMIN, DE | Update |
| DELETE | /api/dashboard/projects/{id}/meter/compliance-items/{cid} | ADMIN | Delete |
| GET | /api/dashboard/projects/{id}/meter/amenities | All roles | List amenities |
| POST | /api/dashboard/projects/{id}/meter/amenities | ADMIN, DE | Create |
| PUT | /api/dashboard/projects/{id}/meter/amenities/{aid} | ADMIN, DE | Update |
| DELETE | /api/dashboard/projects/{id}/meter/amenities/{aid} | ADMIN | Delete |
| GET | /api/dashboard/projects/{id}/meter/price-history | All roles | List |
| POST | /api/dashboard/projects/{id}/meter/price-history | ADMIN, DE | Create |
| PUT | /api/dashboard/projects/{id}/meter/price-history/{pid} | ADMIN, DE | Update |
| DELETE | /api/dashboard/projects/{id}/meter/price-history/{pid} | ADMIN | Delete |
| GET | /api/dashboard/projects/{id}/meter/payment-milestones | All roles | List |
| POST | /api/dashboard/projects/{id}/meter/payment-milestones | ADMIN, DE | Create |
| PUT | /api/dashboard/projects/{id}/meter/payment-milestones/{mid} | ADMIN, DE | Update |
| DELETE | /api/dashboard/projects/{id}/meter/payment-milestones/{mid} | ADMIN | Delete |
| GET | /api/dashboard/projects/{id}/meter/cost-breakdown | All roles | Get |
| PUT | /api/dashboard/projects/{id}/meter/cost-breakdown | ADMIN, DE | Upsert |
| GET | /api/dashboard/projects/{id}/meter/land-utilization | All roles | Get |
| PUT | /api/dashboard/projects/{id}/meter/land-utilization | ADMIN, DE | Upsert |
| GET | /api/dashboard/projects/{id}/meter/location-score | All roles | Get |
| PUT | /api/dashboard/projects/{id}/meter/location-score | ADMIN, DE | Upsert |
| POST | /api/dashboard/projects/{id}/meter/snapshot/recalculate | ADMIN, REV | Recalculate |
| GET | /api/dashboard/project-metadata/property-types | All roles | Property types |
| GET | /api/dashboard/project-metadata/unit-configurations | All roles | Unit configs |
| GET | /api/dashboard/project-metadata/amenity-categories | All roles | Amenity cats |
| GET | /api/dashboard/project-metadata/amenity-suggestions | All roles | Suggestions |
| POST | /api/dashboard/media/presign-upload | ADMIN, DE | Presign upload |
| POST | /api/dashboard/cities | ADMIN | Create city |
| PUT | /api/dashboard/cities/{id} | ADMIN | Update city |
| PATCH | /api/dashboard/cities/{id}/cover-image | ADMIN, DE | Update city cover image |
| DELETE | /api/dashboard/cities/{id} | ADMIN | Delete |
| GET | /api/dashboard/cities/{id} | All roles | Get city |
| GET | /api/dashboard/cities | All roles | List cities |
| POST | /api/dashboard/categories | ADMIN | Create category |
| PUT | /api/dashboard/categories/{id} | ADMIN | Update |
| DELETE | /api/dashboard/categories/{id} | ADMIN | Delete |
| GET | /api/dashboard/categories/{id} | All roles | Get |
| GET | /api/dashboard/categories | All roles | List |
| POST | /api/dashboard/reviews/field-issues | ADMIN, REV | Mark issue |
| GET | /api/dashboard/reviews/field-issues | All roles | List issues |
| DELETE | /api/dashboard/reviews/field-issues/{id} | ADMIN, REV | Delete issue |
| PATCH | /api/dashboard/reviews/field-issues/{id}/fixed | ADMIN, DE | Mark fixed |
| GET | /api/dashboard/reviews/history | All roles | Review history |
| GET | /api/dashboard/audit/projects/{id} | ADMIN | Audit by project |
| GET | /api/dashboard/audit/users/{id} | ADMIN | Audit by user |
| GET | /api/dashboard/audit | ADMIN | Full audit |
| GET | /api/dashboard/field-help | All roles | List help |
| GET | /api/dashboard/field-help/{module}/{fieldKey} | All roles | Single help |
| PUT | /api/dashboard/field-help | ADMIN | Upsert help |
| PATCH | /api/dashboard/field-help/{id}/active | ADMIN | Toggle active |
| DELETE | /api/dashboard/field-help/{id} | ADMIN | Delete |

### Admin (`/api/admin/**`)

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | /api/admin/projects/{id}/public-reviews/places/google-search?query= | ADMIN, DE | Search Google Places |
| POST | /api/admin/projects/{id}/public-reviews/places | ADMIN, DE | Attach place |
| GET | /api/admin/projects/{id}/public-reviews/places | ADMIN, DE | List places |
| POST | /api/admin/projects/{id}/public-reviews/places/{pid}/sync-google | ADMIN, DE | One-time Google sync |
| GET | /api/admin/projects/{id}/public-reviews/signal | ADMIN, DE, REV | Admin signal |
| PATCH | /api/admin/public-review-samples/{id}/display-status | ADMIN | Update sample status |
| POST | /api/admin/projects/{id}/sfs-reviews | ADMIN, DE | Create SFS review |
| GET | /api/admin/projects/{id}/sfs-reviews | ADMIN, DE, REV | List SFS reviews |
| PATCH | /api/admin/sfs-reviews/{id}/verification | ADMIN, REV | Update verification |
| GET | /api/admin/projects/connectivity/provider-categories | ADMIN, DE | Provider categories |
| POST | /api/admin/projects/{id}/connectivity/provider-search | ADMIN, DE | Provider search |
| POST | /api/admin/projects/{id}/connectivity/places/bulk | ADMIN, DE | Bulk save places |
| POST | /api/admin/brands | ADMIN | Create brand |
| PUT | /api/admin/brands/{id} | ADMIN | Update brand |
| PATCH | /api/admin/brands/{id}/publish | ADMIN | Publish toggle |
| DELETE | /api/admin/brands/{id} | ADMIN | Delete |
| GET | /api/admin/brands/{id} | ADMIN | Get |
| GET | /api/admin/brands | ADMIN | List |
| POST | /api/admin/distributors | ADMIN | Create distributor |
| PUT | /api/admin/distributors/{id} | ADMIN | Update |
| GET | /api/admin/distributors/{id} | ADMIN | Get |
| GET | /api/admin/distributors | ADMIN | List |
| DELETE | /api/admin/distributors/{id} | ADMIN | Delete |
| POST | /api/admin/circle-rates | ADMIN | Create rule |
| PUT | /api/admin/circle-rates/{id} | ADMIN | Update rule |
| GET | /api/admin/circle-rates | ADMIN | List rules |
| POST | /api/admin/stamp-duty | ADMIN | Create rule |
| PUT | /api/admin/stamp-duty/{id} | ADMIN | Update rule |
| GET | /api/admin/stamp-duty | ADMIN | List rules |
| POST | /api/admin/interior-cost/base-rules | ADMIN | Create base rule |
| POST | /api/admin/interior-cost/addon-rules | ADMIN | Create addon rule |
| GET | /api/admin/interior-cost/base-rules | ADMIN | List base rules |
| GET | /api/admin/interior-cost/addon-rules | ADMIN | List addon rules |
| POST | /api/admin/project-meter/{id}/recalculate-snapshot | Auth | Recalculate |
| POST | /api/admin/project-meter/recalculate-all | Auth | Recalculate all |

---

*Document generated from codebase inspection — June 2026.*  
*Maintainer: SFS Backend Team.*  
*For errors or updates, open a ticket and reference the controller class and line number.*
