# Dashboard API Documentation

> **Audience:** Frontend developers building the admin dashboard UI.  
> **Base URL:** All paths are relative to your server root (e.g. `https://api.yourdomain.com`).  
> **Auth:** Every dashboard endpoint requires a Bearer token obtained from `/api/dashboard/auth/login`. Include it as `Authorization: Bearer <token>` on all requests.  
> **Content-Type:** `application/json` for all request bodies.

---

## Roles

| Role | Abbreviation | Description |
|------|-------------|-------------|
| `ADMIN` | A | Full access — create, read, update, delete, publish, approve, reject |
| `REVIEWER` | R | Can view everything, approve/reject projects, flag field issues |
| `DATA_ENTRY` | DE | Can create and edit draft content; cannot publish or delete |

Each endpoint below lists which roles are permitted in the **Access** column.

---

## Table of Contents

1. [Authentication](#1-authentication)
2. [Overview (Dashboard Home)](#2-overview-dashboard-home)
3. [Builders](#3-builders)
4. [Projects](#4-projects)
   - [Core Project Data](#41-core-project-data)
   - [Project Media](#42-project-media)
   - [Project Highlights](#43-project-highlights)
   - [Project Floor Plans](#44-project-floor-plans)
   - [Project Connectivity](#45-project-connectivity)
   - [Project Review Workflow](#46-project-review-workflow)
   - [Project Workspace (Composite)](#47-project-workspace-composite)
5. [Project Meter (Analytics)](#5-project-meter-analytics)
   - [Construction Stages](#51-construction-stages)
   - [Compliance Items](#52-compliance-items)
   - [Amenities](#53-amenities)
   - [Price History](#54-price-history)
   - [Payment Milestones](#55-payment-milestones)
   - [Cost Breakdown](#56-cost-breakdown)
   - [Land Utilization](#57-land-utilization)
   - [Location Score](#58-location-score)
6. [Categories](#6-categories)
7. [Cities](#7-cities)
8. [Brands](#8-brands)
   - [Core Brand Data](#81-core-brand-data)
   - [Brand Media](#82-brand-media)
   - [Brand Distributors](#83-brand-distributors)
9. [Distributors](#9-distributors)
   - [Core Distributor Data](#91-core-distributor-data)
   - [Distributor Media](#92-distributor-media)
10. [Calculators](#10-calculators)
    - [Circle Rate Rules](#101-circle-rate-rules)
    - [Stamp Duty Rules](#102-stamp-duty-rules)
    - [Interior Cost Rules](#103-interior-cost-rules)
11. [Media Upload (Presign)](#11-media-upload-presign)
12. [Review & Field Issues](#12-review--field-issues)
13. [Audit Log](#13-audit-log)
14. [Field Help System](#14-field-help-system)
15. [Data Imports (RERA Scraping)](#15-data-imports-rera-scraping)
    - [Search RERA by Number (no persistence)](#151-search-rera-by-number-no-persistence)
    - [Save Scrape Candidate](#152-save-scrape-candidate)
    - [List Candidates](#153-list-candidates)
    - [Get Candidate Detail](#154-get-candidate-detail)
    - [Update Candidate Status](#155-update-candidate-status)
    - [Link Builder to Candidate](#156-link-builder-to-candidate)
    - [Apply Candidate to Project](#157-apply-candidate-to-project)

---

## 1. Authentication

**Base path:** `/api/dashboard/auth`

### 1.1 Login

```
POST /api/dashboard/auth/login
```

**Access:** Public (no token required)

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `email` | string | ✅ | Valid email format, not blank | Dashboard user email |
| `password` | string | ✅ | Not blank | Dashboard user password |

**Example:**
```json
{
  "email": "admin@yourdomain.com",
  "password": "yourpassword"
}
```

**Response:** `DashboardAuthResponse`
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "user": {
    "id": 1,
    "email": "admin@yourdomain.com",
    "role": "ADMIN"
  }
}
```

---

### 1.2 Refresh Token

```
POST /api/dashboard/auth/refresh
```

**Access:** Public

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `refreshToken` | string | ✅ | Valid refresh token from previous login/refresh |

**Response:** `DashboardAuthResponse` (new access + refresh tokens)

---

### 1.3 Logout

```
POST /api/dashboard/auth/logout
```

**Access:** Public (but send the refresh token to invalidate it)

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `refreshToken` | string | ✅ | Token to invalidate |

**Response:** `204 No Content`

---

### 1.4 Get Current User

```
GET /api/dashboard/auth/me
```

**Access:** A, R, DE

**Response:** `DashboardUserResponse`
```json
{
  "id": 1,
  "email": "admin@yourdomain.com",
  "role": "ADMIN",
  "name": "John Doe"
}
```

---

## 2. Overview (Dashboard Home)

```
GET /api/dashboard/overview
```

**Access:** A, R, DE

Returns aggregate counts for the dashboard home screen.

**Response:** `DashboardOverviewResponse`
```json
{
  "totalProjects": 142,
  "pendingReview": 7,
  "totalBuilders": 38,
  "totalBrands": 24,
  "projectsSubmittedThisMonth": 5
}
```

---

## 3. Builders

**Base path:** `/api/dashboard/builders`

Builders are real estate developers who own projects.

### 3.1 Create Builder

```
POST /api/dashboard/builders
```

**Access:** A, DE

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | ✅ | max 150 chars | Builder/developer company name |
| `logoUrl` | string | ❌ | max 500 chars | Full URL to the builder's logo image |
| `description` | string | ❌ | max 4000 chars | About the builder |
| `phone` | string | ❌ | max 20 chars | Primary contact phone |
| `whatsapp` | string | ❌ | max 20 chars | WhatsApp number |
| `email` | string | ❌ | valid email, max 120 chars | Contact email |
| `addressLine` | string | ❌ | max 300 chars | Office address |
| `cityId` | number | ❌ | Valid city ID | City where head office is located |
| `latitude` | number | ❌ | -90.0 to 90.0 | GPS latitude of office |
| `longitude` | number | ❌ | -180.0 to 180.0 | GPS longitude of office |
| `priority` | number | ❌ | 0–9999 | Sort order (lower = higher priority) |
| `active` | boolean | ❌ | — | Whether builder is active (default: `true`) |

**Example:**
```json
{
  "name": "Prestige Group",
  "logoUrl": "https://cdn.yourdomain.com/builders/prestige-logo.png",
  "description": "One of India's leading real estate developers.",
  "phone": "+919876543210",
  "whatsapp": "+919876543210",
  "email": "info@prestige.com",
  "addressLine": "Prestige Trade Tower, Level 15, Palace Road",
  "cityId": 3,
  "latitude": 12.9716,
  "longitude": 77.5946,
  "priority": 1,
  "active": true
}
```

**Response:** `BuilderResponse` with `id`, all fields above, `published`, `createdAt`, `updatedAt`.

---

### 3.2 Update Builder

```
PUT /api/dashboard/builders/{builderId}
```

**Access:** A *(ADMIN only for updates)*

**Path variable:** `builderId` — ID of the builder to update.

**Request Body:** Same fields as Create. All fields are optional on update; only send fields that need to change.

---

### 3.3 Publish / Unpublish Builder

```
PATCH /api/dashboard/builders/{builderId}/published?value=true
```

**Access:** A

**Query param:** `value` (boolean, required) — `true` = publish, `false` = unpublish.

**Response:** Updated `BuilderResponse`.

> **Note:** A builder must be published before their projects appear on the public app.

---

### 3.4 Get Builder

```
GET /api/dashboard/builders/{builderId}
```

**Access:** A, R, DE

---

### 3.5 List Builders

```
GET /api/dashboard/builders?published=true&active=true&page=0&size=20
```

**Access:** A, R, DE

**Query params:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `published` | boolean | ❌ | Filter by published status |
| `active` | boolean | ❌ | Filter by active status |
| `page` | number | ❌ | Page index (default: 0) |
| `size` | number | ❌ | Page size (default: 20) |

**Response:** Paginated `Page<BuilderResponse>`

---

### 3.6 Delete Builder

```
DELETE /api/dashboard/builders/{builderId}
```

**Access:** A

Soft delete — builder is marked as deleted and hidden from all APIs.

---

## 4. Projects

Projects belong to a builder. A project goes through a review workflow before it is published and visible to end users.

**Project lifecycle:**
```
DRAFT → PENDING_REVIEW → APPROVED (published) 
                        ↘ REJECTED → DRAFT (after fix) → resubmit
                        ↘ RECHECK  → DRAFT (after fix) → resubmit
```

---

### 4.1 Core Project Data

**Base path:** `/api/dashboard`

#### Create Project

```
POST /api/dashboard/builders/{builderId}/projects
```

**Access:** A, DE

**Path variable:** `builderId` — the builder this project belongs to.

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | ✅ | max 180 chars | Project name (e.g. "Prestige Lakeside Habitat") |
| `slug` | string | ❌ | max 120 chars, URL-safe pattern | URL slug (e.g. `prestige-lakeside-habitat`). Auto-generated if omitted |
| `description` | string | ❌ | max 4000 chars | Full description / about the project |
| `cityId` | number | ❌ | Valid city ID | City where the project is located |
| `addressLine` | string | ❌ | max 300 chars | Street address of the project |
| `latitude` | number | ❌ | -90.0 to 90.0 | Project GPS latitude |
| `longitude` | number | ❌ | -180.0 to 180.0 | Project GPS longitude |
| `priceMin` | number | ❌ | ≥ 0 | Minimum price (in INR, no decimals) |
| `priceMax` | number | ❌ | ≥ 0 | Maximum price (in INR, no decimals) |
| `possessionDate` | string | ❌ | `YYYY-MM-DD` format | Expected possession / handover date |
| `reraNumber` | string | ❌ | max 50 chars | RERA registration number |
| `status` | string (enum) | ❌ | See values below | Current construction status |
| `propertyTypes` | array of strings | ❌ | See values below | Types of units available |
| `priority` | number | ❌ | 0–9999 | Sort order (lower = shown first) |
| `active` | boolean | ❌ | — | Whether project is active (default: `true`) |

**`status` values:**
```
UPCOMING, UNDER_CONSTRUCTION, READY_TO_MOVE, COMPLETED
```

**`propertyTypes` values:**
```
APARTMENT, VILLA, PLOT, COMMERCIAL, STUDIO, PENTHOUSE, ROW_HOUSE, FARMHOUSE
```

**Example:**
```json
{
  "name": "Prestige Lakeside Habitat",
  "slug": "prestige-lakeside-habitat",
  "description": "A premium lakeside township offering 2, 3 & 4 BHK apartments.",
  "cityId": 3,
  "addressLine": "Whitefield Main Road, Bangalore East",
  "latitude": 12.9698,
  "longitude": 77.7500,
  "priceMin": 8500000,
  "priceMax": 25000000,
  "possessionDate": "2026-12-31",
  "reraNumber": "PRM/KA/RERA/1251/309/PR/171017/002270",
  "status": "UNDER_CONSTRUCTION",
  "propertyTypes": ["APARTMENT"],
  "priority": 1,
  "active": true
}
```

**Response:** `ProjectResponse` with `id`, all fields, `published: false` (always starts unpublished), `reviewStatus: "DRAFT"`, `createdAt`, `updatedAt`.

---

#### Update Project

```
PUT /api/dashboard/projects/{projectId}
```

**Access:** A, DE *(DATA_ENTRY can only edit projects in DRAFT / RECHECK / REJECTED state)*

**Request Body:** Same fields as Create. Send only the fields to change.

---

#### Get Project

```
GET /api/dashboard/projects/{projectId}
```

**Access:** A, R, DE

---

#### List Projects

```
GET /api/dashboard/projects?builderId=5&reviewStatus=PENDING_REVIEW&page=0&size=20
```

**Access:** A, R, DE

**Query params:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `builderId` | number | ❌ | Filter by builder |
| `reviewStatus` | string | ❌ | `DRAFT`, `PENDING_REVIEW`, `APPROVED`, `REJECTED`, `RECHECK` |
| `page` | number | ❌ | Page index (default: 0, max page size: 50) |
| `size` | number | ❌ | Page size (default: 20) |

**Sorted:** by `priority ASC, id DESC` automatically.

---

#### Set Published

```
PATCH /api/dashboard/projects/{projectId}/published?value=true
```

**Access:** A only

> Normally triggered automatically on approval. Direct use is for admin overrides.

---

#### Set Active

```
PATCH /api/dashboard/projects/{projectId}/active?value=false
```

**Access:** A only

---

#### Delete Project

```
DELETE /api/dashboard/projects/{projectId}
```

**Access:** A only. Soft delete — marks as deleted and unpublished.

---

### 4.2 Project Media

**Base path:** `/api/dashboard/projects/{projectId}/media`

Media items are the images, videos, and brochures attached to a project. **Upload files using the Presign endpoint first** (Section 11), then save the returned URL here.

#### Add Media

```
POST /api/dashboard/projects/{projectId}/media
```

**Access:** A, DE

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `mediaType` | string (enum) | ✅ | See values below | Type of media |
| `url` | string | ✅ | max 500 chars | S3 URL returned by the presign upload flow |
| `caption` | string | ❌ | max 300 chars | Display caption / alt text |
| `sortOrder` | number | ❌ | 0–9999 | Display order (lower = first) |
| `active` | boolean | ❌ | — | Whether to show (default: `true`) |

**`mediaType` values:**
```
IMAGE         - Photo of the project
VIDEO         - Video walkthrough (YouTube/Vimeo/CDN URL)
BROCHURE_PDF  - Project brochure PDF
FLOOR_PLAN    - Floor plan image
CONNECTIVITY_MAP - Nearby location map image
```

**Example:**
```json
{
  "mediaType": "IMAGE",
  "url": "https://cdn.yourdomain.com/projects/123/hero.jpg",
  "caption": "Aerial view of the project",
  "sortOrder": 1,
  "active": true
}
```

---

#### Update Media

```
PUT /api/dashboard/projects/{projectId}/media/{mediaId}
```

**Access:** A, DE

**Request Body:** Same as Add Media.

---

#### List Media

```
GET /api/dashboard/projects/{projectId}/media
```

**Access:** A, R, DE

Returns all non-deleted media including inactive items.

---

#### Delete Media

```
DELETE /api/dashboard/projects/{projectId}/media/{mediaId}
```

**Access:** A only

---

### 4.3 Project Highlights

Highlights are short bullet points shown on the project detail screen (e.g. "Zero Stamp Duty", "24/7 Security", "RERA Approved").

**Base path:** `/api/dashboard/projects/{projectId}/highlights`

#### Add Highlight

```
POST /api/dashboard/projects/{projectId}/highlights
```

**Access:** A, DE

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `title` | string | ✅ | max 150 chars | Main highlight text (e.g. "Zero Stamp Duty") |
| `subtitle` | string | ❌ | max 300 chars | Supporting detail (e.g. "For first-time buyers only") |
| `iconKey` | string | ❌ | max 80 chars | Icon identifier from your icon library (e.g. `shield-check`, `home`) |
| `sortOrder` | number | ❌ | 0–9999 | Display order |
| `active` | boolean | ❌ | — | Whether to show (default: `true`) |

**Example:**
```json
{
  "title": "RERA Approved",
  "subtitle": "Registration No. PRM/KA/RERA/1251/309",
  "iconKey": "shield-check",
  "sortOrder": 1,
  "active": true
}
```

---

#### Update Highlight

```
PUT /api/dashboard/projects/{projectId}/highlights/{highlightId}
```

**Access:** A, DE

**Request Body:** Same as Add Highlight.

---

#### List Highlights

```
GET /api/dashboard/projects/{projectId}/highlights
```

**Access:** A, R, DE

---

#### Delete Highlight

```
DELETE /api/dashboard/projects/{projectId}/highlights/{highlightId}
```

**Access:** A only

---

### 4.4 Project Floor Plans

Floor plans represent different unit configurations (2 BHK, 3 BHK, etc.) with layout images and area details.

**Base path:** `/api/dashboard/projects/{projectId}/floor-plans`

#### Create Floor Plan

```
POST /api/dashboard/projects/{projectId}/floor-plans
```

**Access:** A, DE

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `title` | string | ✅ | max 150 chars | Unit name (e.g. "3 BHK - Type A") |
| `floorCode` | string | ❌ | max 50 chars | Internal code (e.g. "3BHK-A") |
| `imageUrl` | string | ✅ | max 500 chars | Floor plan image URL (from presign upload) |
| `carpetArea` | string | ❌ | max 50 chars | Carpet area label (e.g. "1250 sq ft") |
| `exclusiveArea` | string | ❌ | max 50 chars | Exclusive area label |
| `superArea` | string | ❌ | max 50 chars | Super built-up area label |
| `unitLabel` | string | ❌ | max 80 chars | Unit description (e.g. "East-facing, 14th floor") |
| `description` | string | ❌ | max 1000 chars | Additional notes about this floor plan |
| `sortOrder` | number | ❌ | 0–9999 | Display order |
| `active` | boolean | ❌ | — | Whether to show (default: `true`) |

**Example:**
```json
{
  "title": "3 BHK Luxury",
  "floorCode": "3BHK-A",
  "imageUrl": "https://cdn.yourdomain.com/projects/123/floorplan-3bhk.jpg",
  "carpetArea": "1480 sq ft",
  "superArea": "1850 sq ft",
  "unitLabel": "Corner unit, East-facing",
  "description": "Premium 3 BHK with private terrace and city views.",
  "sortOrder": 1,
  "active": true
}
```

---

#### Update Floor Plan

```
PUT /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}
```

**Access:** A, DE

---

#### Toggle Floor Plan Active

```
PATCH /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}/active?active=false
```

**Access:** A only

---

#### List Floor Plans

```
GET /api/dashboard/projects/{projectId}/floor-plans
```

**Access:** A, R, DE

---

#### Delete Floor Plan

```
DELETE /api/dashboard/projects/{projectId}/floor-plans/{floorPlanId}
```

**Access:** A only

---

### 4.5 Project Connectivity

Connectivity describes the project's location relative to landmarks (metro, schools, hospitals, etc.).

**Base path:** `/api/dashboard/projects/{projectId}/connectivity`

#### Upsert Connectivity Overview

Creates or updates the connectivity section header (title, subtitle, map image).

```
PUT /api/dashboard/projects/{projectId}/connectivity
```

**Access:** A, DE

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `title` | string | ❌ | max 150 chars | Section title (e.g. "Connectivity & Location") |
| `subtitle` | string | ❌ | max 300 chars | Subtitle / tagline |
| `mapImageUrl` | string | ❌ | max 500 chars | URL to a map/location image |
| `active` | boolean | ❌ | — | Whether section is enabled |

**Example:**
```json
{
  "title": "Excellent Connectivity",
  "subtitle": "Located 5 minutes from Whitefield Metro Station",
  "mapImageUrl": "https://cdn.yourdomain.com/projects/123/map.jpg",
  "active": true
}
```

---

#### Get Connectivity Overview

```
GET /api/dashboard/projects/{projectId}/connectivity
```

**Access:** A, R, DE

---

#### Add Nearby Place

```
POST /api/dashboard/projects/{projectId}/connectivity/places
```

**Access:** A, DE

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `placeName` | string | ✅ | max 150 chars | Name of the place (e.g. "Whitefield Metro Station") |
| `placeType` | string (enum) | ✅ | See values below | Category of the place |
| `distanceLabel` | string | ❌ | max 80 chars | Display distance (e.g. "2.5 km" or "5 min drive") |
| `imageUrl` | string | ❌ | max 500 chars | Optional image for this place |
| `sortOrder` | number | ❌ | 0–9999 | Display order |
| `active` | boolean | ❌ | — | Whether to show |

**`placeType` values:**
```
METRO, BUS_STOP, RAILWAY, AIRPORT,
SCHOOL, COLLEGE, HOSPITAL, CLINIC,
MALL, SUPERMARKET, RESTAURANT,
PARK, STADIUM, TEMPLE, CHURCH, MOSQUE,
TECH_PARK, OFFICE_HUB, BANK, ATM
```

**Example:**
```json
{
  "placeName": "Phoenix Marketcity",
  "placeType": "MALL",
  "distanceLabel": "3 km",
  "sortOrder": 5,
  "active": true
}
```

---

#### Update Nearby Place

```
PUT /api/dashboard/projects/{projectId}/connectivity/places/{placeId}
```

**Access:** A, DE

---

#### List Nearby Places

```
GET /api/dashboard/projects/{projectId}/connectivity/places
```

**Access:** A, R, DE

---

#### Delete Nearby Place

```
DELETE /api/dashboard/projects/{projectId}/connectivity/places/{placeId}
```

**Access:** A only

---

### 4.6 Project Review Workflow

Projects must be submitted for review before they go live. Reviewers then approve or reject with remarks.

**Base path:** `/api/dashboard/projects/{projectId}`

#### Get Review Status

```
GET /api/dashboard/projects/{projectId}/review-status
```

**Access:** A, R, DE

**Response:** `DashboardProjectReviewResponse`
```json
{
  "projectId": 123,
  "reviewStatus": "PENDING_REVIEW",
  "submittedAt": "2025-03-15T10:30:00Z",
  "submittedByUserId": 7,
  "reviewRemarks": null,
  "reviewedAt": null,
  "reviewedByUserId": null
}
```

---

#### Submit for Review

DATA_ENTRY submits the completed project for a reviewer to check.

```
POST /api/dashboard/projects/{projectId}/submit-review
```

**Access:** A, DE

**Request Body (optional):**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `remarks` | string | ❌ | max 2000 chars | Notes for the reviewer |

---

#### Approve Project

Approving a project sets `reviewStatus = APPROVED` and automatically publishes it.

```
POST /api/dashboard/projects/{projectId}/approve
```

**Access:** A, R

**Request Body (optional):**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `remarks` | string | ❌ | max 2000 chars | Internal review notes |

---

#### Reject Project

Sends the project back to the data entry team with feedback.

```
POST /api/dashboard/projects/{projectId}/reject
```

**Access:** A, R

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `remarks` | string | ❌ | max 2000 chars | Reason for rejection (shown to data entry) |

---

#### Reopen Project

Re-opens a previously rejected/approved project back to DRAFT state.

```
POST /api/dashboard/projects/{projectId}/reopen
```

**Access:** A, R

**Request Body (optional):**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `remarks` | string | ❌ | max 2000 chars | Reason for reopening |

---

### 4.7 Project Workspace (Composite)

Fetches the full project edit screen in a single call — project details + all child sections + review state. Use this instead of making 10+ individual GET calls.

```
GET /api/dashboard/projects/{projectId}/workspace
```

**Access:** A, R, DE

**Response:** `DashboardProjectWorkspaceResponse` — contains:
- Core project fields
- Media list
- Highlights list
- Floor plans list
- Connectivity overview + places
- Project meter snapshot summary
- Current review status + field issues
- Audit trail summary

---

## 5. Project Meter (Analytics)

The Project Meter is a detailed analytics section for each project. It powers trust scores, progress bars, and financial breakdowns in the app.

**Base path:** `/api/dashboard/projects/{projectId}/meter`

---

### 5.1 Construction Stages

Tracks individual construction stages (foundation, structure, finishing, etc.) with progress percentages.

#### Create Stage

```
POST /api/dashboard/projects/{projectId}/meter/construction-stages
```

**Access:** A, DE

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `stageCode` | string (enum) | ✅ | See values below | Stage type |
| `stageLabel` | string | ✅ | max 120 chars | Display name (e.g. "Foundation Work") |
| `displayOrder` | number | ✅ | 0–999 | Order in the timeline |
| `weightPercent` | number | ✅ | 0–100 | Contribution to overall progress (all stages should sum to 100) |
| `progressPercent` | number | ❌ | 0–100 | Current completion percentage |
| `plannedStartDate` | string | ❌ | `YYYY-MM-DD` | Planned start |
| `plannedEndDate` | string | ❌ | `YYYY-MM-DD` | Planned completion |
| `actualStartDate` | string | ❌ | `YYYY-MM-DD` | Actual start (fill once started) |
| `actualEndDate` | string | ❌ | `YYYY-MM-DD` | Actual completion (fill once done) |
| `status` | string (enum) | ❌ | See values below | Stage status |
| `remarks` | string | ❌ | max 500 chars | Notes / explanation |
| `evidenceCount` | number | ❌ | ≥ 0 | Number of inspection photos/docs |
| `verified` | boolean | ❌ | — | Whether a reviewer has verified this stage |

**`stageCode` values:**
```
LAND_ACQUISITION, APPROVALS, EXCAVATION, FOUNDATION,
STRUCTURE, BRICKWORK, PLUMBING, ELECTRICAL,
FLOORING, FINISHING, PAINTING, HANDOVER
```

**`status` values:**
```
NOT_STARTED, IN_PROGRESS, COMPLETED, DELAYED, ON_HOLD
```

**Example:**
```json
{
  "stageCode": "STRUCTURE",
  "stageLabel": "Super Structure Work",
  "displayOrder": 4,
  "weightPercent": 25,
  "progressPercent": 70,
  "plannedStartDate": "2024-01-01",
  "plannedEndDate": "2025-06-30",
  "actualStartDate": "2024-02-01",
  "status": "IN_PROGRESS",
  "evidenceCount": 12,
  "verified": true
}
```

---

#### Update Stage

```
PUT /api/dashboard/projects/{projectId}/meter/construction-stages/{stageId}
```

**Access:** A, DE

---

#### List Stages

```
GET /api/dashboard/projects/{projectId}/meter/construction-stages
```

**Access:** A, R, DE

---

#### Delete Stage

```
DELETE /api/dashboard/projects/{projectId}/meter/construction-stages/{stageId}
```

**Access:** A only

---

### 5.2 Compliance Items

RERA registrations, environmental clearances, fire NOC, OC, CC — any legal/regulatory document.

#### Create Compliance Item

```
POST /api/dashboard/projects/{projectId}/meter/compliance-items
```

**Access:** A, DE

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `itemGroup` | string (enum) | ✅ | See values below | Category of compliance |
| `itemKey` | string | ✅ | max 100 chars | Internal identifier (e.g. `RERA_REGISTRATION`) |
| `itemLabel` | string | ✅ | max 180 chars | Display name (e.g. "RERA Registration") |
| `status` | string (enum) | ✅ | See values below | Current status |
| `valueText` | string | ❌ | max 300 chars | The actual certificate/reference number |
| `documentUrl` | string | ❌ | max 500 chars | Link to the scanned document |
| `remarks` | string | ❌ | max 500 chars | Notes |
| `displayOrder` | number | ✅ | 0–999 | Sort order |
| `verified` | boolean | ❌ | — | Reviewer-verified |

**`itemGroup` values:**
```
RERA, ENVIRONMENTAL, FIRE_SAFETY, STRUCTURAL,
LEGAL_TITLE, UTILITY, OCCUPANCY, OTHER,
LAND_LICENSE, APPROVAL_NOC
```

**`status` values:**
```
OBTAINED, PENDING, NOT_APPLICABLE, EXPIRED
```

**Example:**
```json
{
  "itemGroup": "RERA",
  "itemKey": "RERA_REG",
  "itemLabel": "RERA Registration",
  "status": "OBTAINED",
  "valueText": "PRM/KA/RERA/1251/309/PR/171017/002270",
  "documentUrl": "https://cdn.yourdomain.com/docs/rera-cert.pdf",
  "displayOrder": 1,
  "verified": true
}
```

---

#### Update Compliance Item

```
PUT /api/dashboard/projects/{projectId}/meter/compliance-items/{itemId}
```

**Access:** A, DE

---

#### List Compliance Items

```
GET /api/dashboard/projects/{projectId}/meter/compliance-items
```

**Access:** A, R, DE

---

#### Delete Compliance Item

```
DELETE /api/dashboard/projects/{projectId}/meter/compliance-items/{itemId}
```

**Access:** A only

---

### 5.3 Amenities

Tracks which amenities (swimming pool, gym, clubhouse, etc.) are promised and how complete they are.

#### Add Amenity

```
POST /api/dashboard/projects/{projectId}/meter/amenities
```

**Access:** A, DE

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `amenityCode` | string | ✅ | max 80 chars | Internal code (e.g. `SWIMMING_POOL`) |
| `amenityLabel` | string | ✅ | max 120 chars | Display name (e.g. "Swimming Pool") |
| `status` | string (enum) | ✅ | See values below | Completion status |
| `progressPercent` | number | ❌ | 0–100 | Build progress if under construction |
| `weightPercent` | number | ❌ | 0–100 | Weight in overall amenity score |
| `displayOrder` | number | ✅ | 0–999 | Sort order |
| `remarks` | string | ❌ | max 500 chars | Notes |
| `verified` | boolean | ❌ | — | Reviewer-verified |

**`status` values:**
```
PLANNED, UNDER_CONSTRUCTION, COMPLETED, NOT_AVAILABLE
```

**Example:**
```json
{
  "amenityCode": "CLUBHOUSE",
  "amenityLabel": "Clubhouse (15,000 sq ft)",
  "status": "UNDER_CONSTRUCTION",
  "progressPercent": 60,
  "weightPercent": 15,
  "displayOrder": 2,
  "verified": false
}
```

---

#### Update Amenity

```
PUT /api/dashboard/projects/{projectId}/meter/amenities/{amenityId}
```

**Access:** A, DE

---

#### List Amenities

```
GET /api/dashboard/projects/{projectId}/meter/amenities
```

**Access:** A, R, DE

---

#### Delete Amenity

```
DELETE /api/dashboard/projects/{projectId}/meter/amenities/{amenityId}
```

**Access:** A only

---

### 5.4 Price History

Historical price points showing how the project price has changed over time.

#### Add Price History Point

```
POST /api/dashboard/projects/{projectId}/meter/price-history
```

**Access:** A, DE

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `yearLabel` | string | ✅ | Pattern `YYYY` or `YYYY-MM` (e.g. `"2023"` or `"2023-06"`) | Time period label |
| `projectPrice` | number | ✅ | ≥ 0 | Project's price per sq ft at this point (INR) |
| `averageAreaPrice` | number | ❌ | ≥ 0 | Average locality price per sq ft for comparison |
| `displayOrder` | number | ✅ | 0–999 | Order (chronological, oldest first) |
| `verified` | boolean | ❌ | — | Reviewer-verified |

**Example:**
```json
{
  "yearLabel": "2022",
  "projectPrice": 7200,
  "averageAreaPrice": 6800,
  "displayOrder": 1,
  "verified": true
}
```

---

#### Update Price History Point

```
PUT /api/dashboard/projects/{projectId}/meter/price-history/{priceHistoryId}
```

**Access:** A, DE

---

#### List Price History

```
GET /api/dashboard/projects/{projectId}/meter/price-history
```

**Access:** A, R, DE

---

#### Delete Price History Point

```
DELETE /api/dashboard/projects/{projectId}/meter/price-history/{priceHistoryId}
```

**Access:** A only

---

### 5.5 Payment Milestones

Payment schedule tied to construction stages (e.g. 10% on booking, 20% on foundation completion).

#### Add Payment Milestone

```
POST /api/dashboard/projects/{projectId}/meter/payment-milestones
```

**Access:** A, DE

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `milestoneCode` | string | ✅ | max 80 chars | Internal code (e.g. `ON_BOOKING`) |
| `milestoneLabel` | string | ✅ | max 180 chars | Display name (e.g. "On Booking") |
| `description` | string | ❌ | max 1000 chars | Detailed explanation of this milestone |
| `percentageValue` | number | ✅ | 0–100 | % of total payment due at this milestone |
| `displayOrder` | number | ✅ | 0–999 | Sort order (typically chronological) |
| `linkedStageCode` | string (enum) | ❌ | Same values as `stageCode` above | Tie payment to a construction stage |
| `active` | boolean | ❌ | — | Whether milestone is active |

**Example:**
```json
{
  "milestoneCode": "ON_FOUNDATION",
  "milestoneLabel": "On Foundation Completion",
  "description": "Payment due when foundation work is certified complete.",
  "percentageValue": 15,
  "displayOrder": 2,
  "linkedStageCode": "FOUNDATION",
  "active": true
}
```

---

#### Update Payment Milestone

```
PUT /api/dashboard/projects/{projectId}/meter/payment-milestones/{milestoneId}
```

**Access:** A, DE

---

#### List Payment Milestones

```
GET /api/dashboard/projects/{projectId}/meter/payment-milestones
```

**Access:** A, R, DE

---

#### Delete Payment Milestone

```
DELETE /api/dashboard/projects/{projectId}/meter/payment-milestones/{milestoneId}
```

**Access:** A only

---

### 5.6 Cost Breakdown

Total project cost split into categories (land, construction, infrastructure, other).

#### Upsert Cost Breakdown

This is a single record per project — calling PUT creates it if it doesn't exist, or updates it.

```
PUT /api/dashboard/projects/{projectId}/meter/cost-breakdown
```

**Access:** A, DE

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `landCost` | number | ❌ | ≥ 0 | Land acquisition cost (INR) |
| `constructionCost` | number | ❌ | ≥ 0 | Construction cost (INR) |
| `infrastructureCost` | number | ❌ | ≥ 0 | Infrastructure development cost (INR) |
| `otherCost` | number | ❌ | ≥ 0 | Other miscellaneous costs (INR) |
| `totalCost` | number | ❌ | ≥ 0 | Total project cost (INR) |
| `sourceLabel` | string | ❌ | max 180 chars | Source of the data (e.g. "Builder Disclosure Q1 2025") |
| `remarks` | string | ❌ | max 500 chars | Notes |
| `verified` | boolean | ❌ | — | Reviewer-verified |

**Example:**
```json
{
  "landCost": 500000000,
  "constructionCost": 1200000000,
  "infrastructureCost": 200000000,
  "otherCost": 100000000,
  "totalCost": 2000000000,
  "sourceLabel": "Builder Prospectus 2024",
  "verified": true
}
```

---

#### Get Cost Breakdown

```
GET /api/dashboard/projects/{projectId}/meter/cost-breakdown
```

**Access:** A, R, DE

---

### 5.7 Land Utilization

How the total project land is divided between different uses.

#### Upsert Land Utilization

Single record per project — creates or updates.

```
PUT /api/dashboard/projects/{projectId}/meter/land-utilization
```

**Access:** A, DE

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `totalLandAreaSqm` | number | ❌ | ≥ 0.0 | Total land area in square meters |
| `residentialAreaSqm` | number | ❌ | ≥ 0.0 | Area for residential towers |
| `commercialAreaSqm` | number | ❌ | ≥ 0.0 | Area for commercial units |
| `parksAreaSqm` | number | ❌ | ≥ 0.0 | Parks and gardens area |
| `openAreaSqm` | number | ❌ | ≥ 0.0 | Other open spaces |
| `parkingAreaSqm` | number | ❌ | ≥ 0.0 | Parking area |
| `utilityAreaSqm` | number | ❌ | ≥ 0.0 | Utility/service areas |

**Example:**
```json
{
  "totalLandAreaSqm": 40000,
  "residentialAreaSqm": 18000,
  "commercialAreaSqm": 3000,
  "parksAreaSqm": 8000,
  "openAreaSqm": 5000,
  "parkingAreaSqm": 4000,
  "utilityAreaSqm": 2000
}
```

---

#### Get Land Utilization

```
GET /api/dashboard/projects/{projectId}/meter/land-utilization
```

**Access:** A, R, DE

---

### 5.8 Location Score

Radar chart scores rating the project's location on multiple dimensions (0.0–10.0 per axis).

#### Upsert Location Score

Single record per project — creates or updates.

```
PUT /api/dashboard/projects/{projectId}/meter/location-score
```

**Access:** A, DE

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `metroScore` | number | ❌ | 0.0–10.0 | Proximity/access to metro/rapid transit |
| `educationScore` | number | ❌ | 0.0–10.0 | Proximity to good schools and colleges |
| `healthcareScore` | number | ❌ | 0.0–10.0 | Proximity to hospitals and clinics |
| `retailScore` | number | ❌ | 0.0–10.0 | Proximity to malls, markets, restaurants |
| `jobScore` | number | ❌ | 0.0–10.0 | Proximity to tech parks / employment hubs |
| `leisureScore` | number | ❌ | 0.0–10.0 | Parks, recreation, entertainment |
| `currentStrengthScore` | number | ❌ | 0.0–10.0 | Overall current infrastructure quality |
| `futureGrowthScore` | number | ❌ | 0.0–10.0 | Expected area appreciation potential |
| `finalScore` | number | ❌ | 0.0–10.0 | Composite final location score |
| `appreciationPercent3Y` | number | ❌ | -100.0 to 9999.0 | Expected price appreciation in 3 years (%) |
| `scoreSummary` | string | ❌ | max 1000 chars | Plain-text narrative about the location |
| `verified` | boolean | ❌ | — | Reviewer-verified |

**Example:**
```json
{
  "metroScore": 8.5,
  "educationScore": 7.0,
  "healthcareScore": 7.5,
  "retailScore": 9.0,
  "jobScore": 9.5,
  "leisureScore": 6.5,
  "currentStrengthScore": 8.0,
  "futureGrowthScore": 8.5,
  "finalScore": 8.2,
  "appreciationPercent3Y": 35.0,
  "scoreSummary": "Excellent IT corridor location with strong metro connectivity and top schools within 3 km.",
  "verified": true
}
```

---

#### Get Location Score

```
GET /api/dashboard/projects/{projectId}/meter/location-score
```

**Access:** A, R, DE

---

#### Recalculate Meter Snapshot

Triggers a full recalculation of the project's composite meter score (aggregates all the sub-sections above into a single score used in the app).

```
POST /api/dashboard/projects/{projectId}/meter/snapshot/recalculate
```

**Access:** A, R

Call this after making significant changes to meter data.

---

## 6. Categories

Categories define the type of business or listing (e.g. "Real Estate", "Interior Design", "Architects").

**Base path:** `/api/dashboard/categories`

### Create Category

```
POST /api/dashboard/categories
```

**Access:** A only

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | ✅ | max 150 chars | Display name (e.g. "Interior Designers") |
| `slug` | string | ✅ | max 180 chars, URL-safe | Unique slug (e.g. `interior-designers`) |
| `parentId` | number | ❌ | Valid category ID | Parent category ID for subcategories |
| `priority` | number | ❌ | 0–9999 | Sort order |
| `active` | boolean | ❌ | — | Whether to show (default: `true`) |

**Example:**
```json
{
  "name": "Interior Designers",
  "slug": "interior-designers",
  "parentId": 1,
  "priority": 3,
  "active": true
}
```

---

### Update Category

```
PUT /api/dashboard/categories/{categoryId}
```

**Access:** A only

---

### Get Category

```
GET /api/dashboard/categories/{categoryId}
```

**Access:** A, R, DE

---

### List All Categories

```
GET /api/dashboard/categories
```

**Access:** A, R, DE

Returns the full flat list. Build the tree on the frontend using `parentId`.

---

### Delete Category

```
DELETE /api/dashboard/categories/{categoryId}
```

**Access:** A only. Returns `204 No Content`.

---

## 7. Cities

Cities are used to associate projects, businesses, and providers with a location.

**Base path:** `/api/dashboard/cities`

### Create City

```
POST /api/dashboard/cities
```

**Access:** A only

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | ✅ | max 150 chars | City name (e.g. "Bengaluru") |
| `state` | string | ❌ | max 150 chars | State name (e.g. "Karnataka") |
| `countryCode` | string | ❌ | max 10 chars | ISO country code (e.g. `IN`) |
| `latitude` | number | ❌ | -90.0 to 90.0 | City center GPS latitude |
| `longitude` | number | ❌ | -180.0 to 180.0 | City center GPS longitude |

**Example:**
```json
{
  "name": "Bengaluru",
  "state": "Karnataka",
  "countryCode": "IN",
  "latitude": 12.9716,
  "longitude": 77.5946
}
```

---

### Update City

```
PUT /api/dashboard/cities/{cityId}
```

**Access:** A only

---

### Get City

```
GET /api/dashboard/cities/{cityId}
```

**Access:** A, R, DE

---

### List / Search Cities

```
GET /api/dashboard/cities?query=bang
```

**Access:** A, R, DE

**Query param:** `query` (optional) — partial name search.

---

### Delete City

```
DELETE /api/dashboard/cities/{cityId}
```

**Access:** A only. Returns `204 No Content`.

---

## 8. Brands

Brands are product brands (e.g. paint companies, furniture brands) that have promotions, distributors, and media.

---

### 8.1 Core Brand Data

**Base path:** `/api/admin/brands`

#### Create Brand

```
POST /api/admin/brands
```

**Access:** A only

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | ✅ | max 150 chars | Brand name |
| `logoUrl` | string | ❌ | — | URL to brand logo |
| `description` | string | ❌ | — | About the brand |
| `categoryId` | number | ❌ | Valid category ID | Category this brand belongs to |
| `priority` | number | ❌ | — | Sort order |
| `active` | boolean | ❌ | — | Whether active |
| `promoMediaType` | string (enum) | ❌ | `GIF` or `LOTTIE` | Type of promo animation |
| `promoMediaUrl` | string | ❌ | — | URL to GIF or Lottie JSON file |
| `promoEnabled` | boolean | ❌ | — | Whether to show the promo animation |

**Example:**
```json
{
  "name": "Asian Paints",
  "logoUrl": "https://cdn.yourdomain.com/brands/asian-paints-logo.png",
  "description": "India's largest paint company.",
  "categoryId": 5,
  "priority": 1,
  "active": true,
  "promoMediaType": "LOTTIE",
  "promoMediaUrl": "https://cdn.yourdomain.com/brands/asian-paints-promo.json",
  "promoEnabled": true
}
```

---

#### Update Brand

```
PUT /api/admin/brands/{id}
```

**Access:** A only

---

#### Publish / Unpublish Brand

```
PATCH /api/admin/brands/{id}/publish?published=true
```

**Access:** A only

---

#### Get Brand

```
GET /api/admin/brands/{id}
```

**Access:** A only

---

#### List Brands

```
GET /api/admin/brands?published=true&active=true&page=0&size=20
```

**Access:** A only

---

#### Delete Brand

```
DELETE /api/admin/brands/{id}
```

**Access:** A only

---

### 8.2 Brand Media

Media items displayed in the brand's profile screen (banners, hero images, gallery photos).

**Base path:** `/api/admin/brands/{brandId}/media`

#### Add Brand Media

```
POST /api/admin/brands/{brandId}/media
```

**Access:** A only

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `mediaType` | string (enum) | ✅ | `IMAGE` or `VIDEO` | Type of media |
| `placement` | string (enum) | ✅ | See values below | Where it appears in the UI |
| `url` | string | ✅ | — | Media URL |
| `caption` | string | ❌ | — | Display caption |
| `sortOrder` | number | ❌ | — | Display order |
| `active` | boolean | ❌ | — | Whether to show |
| `actionType` | string | ❌ | — | CTA action type (e.g. `OPEN_URL`, `OPEN_DISTRIBUTORS`) |
| `actionValue` | string | ❌ | — | CTA value (e.g. a URL for `OPEN_URL`) |

**`placement` values:**
```
BANNER   - Full-width promotional banner
HERO     - Hero image at top of brand profile
GALLERY  - Gallery grid images
```

**Example:**
```json
{
  "mediaType": "IMAGE",
  "placement": "BANNER",
  "url": "https://cdn.yourdomain.com/brands/5/summer-sale-banner.jpg",
  "caption": "Summer Sale - 30% Off",
  "sortOrder": 1,
  "active": true,
  "actionType": "OPEN_URL",
  "actionValue": "https://asianpaints.com/offers"
}
```

---

#### List Brand Media by Placement

```
GET /api/admin/brands/{brandId}/media?placement=BANNER
```

**Query param:** `placement` (required) — `BANNER`, `HERO`, or `GALLERY`.

---

#### Delete Brand Media

```
DELETE /api/admin/brands/{brandId}/media/{mediaId}
```

---

### 8.3 Brand Distributors

Links an existing distributor to a brand with offer details.

**Base path:** `/api/admin/brands/{brandId}/distributors`

#### Upsert Brand Distributor

```
POST /api/admin/brands/{brandId}/distributors
```

**Access:** A only

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `distributorId` | number | ✅ | Valid distributor ID | The distributor to link |
| `status` | string (enum) | ❌ | `ACTIVE`, `INACTIVE` | Link status |
| `priority` | number | ❌ | — | Sort order for this brand's distributor list |
| `offerTitle` | string | ❌ | — | Current offer headline (e.g. "Free site visit this weekend") |
| `offerDescription` | string | ❌ | — | Offer details |
| `offerBannerUrl` | string | ❌ | — | Offer banner image URL |
| `validTill` | string | ❌ | ISO 8601 datetime | Offer expiry (e.g. `"2025-12-31T23:59:59Z"`) |
| `active` | boolean | ❌ | — | Whether to show this distributor for this brand |

---

#### List Brand Distributors

```
GET /api/admin/brands/{brandId}/distributors?page=0&size=20
```

---

#### Remove Brand Distributor

```
DELETE /api/admin/brands/{brandId}/distributors/{distributorId}
```

---

## 9. Distributors

Distributors are the physical stores or dealers that sell brand products.

---

### 9.1 Core Distributor Data

**Base path:** `/api/admin/distributors`

#### Create Distributor

```
POST /api/admin/distributors
```

**Access:** A only

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | ✅ | max 200 chars | Store/dealer name |
| `phone` | string | ❌ | — | Primary phone number |
| `whatsapp` | string | ❌ | — | WhatsApp number |
| `email` | string | ❌ | — | Email address |
| `addressLine1` | string | ❌ | — | Street address line 1 |
| `addressLine2` | string | ❌ | — | Street address line 2 |
| `pincode` | string | ❌ | — | PIN code / postal code |
| `cityId` | number | ❌ | Valid city ID | City of the store |
| `latitude` | number | ❌ | — | GPS latitude |
| `longitude` | number | ❌ | — | GPS longitude |
| `active` | boolean | ❌ | — | Whether store is active |

**Example:**
```json
{
  "name": "Color Palace - Indiranagar",
  "phone": "+918023456789",
  "whatsapp": "+918023456789",
  "email": "colorpalace.indiranagar@gmail.com",
  "addressLine1": "100 Feet Road, HAL 2nd Stage",
  "pincode": "560038",
  "cityId": 3,
  "latitude": 12.9784,
  "longitude": 77.6408,
  "active": true
}
```

---

#### Update Distributor

```
PUT /api/admin/distributors/{id}
```

---

#### Get Distributor

```
GET /api/admin/distributors/{id}
```

---

#### List Distributors

```
GET /api/admin/distributors?cityId=3&active=true&page=0&size=20
```

**Query params:** `cityId`, `active`, `page`, `size`

---

#### Delete Distributor

```
DELETE /api/admin/distributors/{id}
```

---

### 9.2 Distributor Media

Photos of the physical store.

**Base path:** `/api/admin/distributors/{id}/media`

#### Add Distributor Media

```
POST /api/admin/distributors/{id}/media
```

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `mediaType` | string (enum) | ✅ | `IMAGE` or `VIDEO` | Type of media |
| `url` | string | ✅ | — | Media URL |
| `caption` | string | ❌ | — | Caption |
| `sortOrder` | number | ❌ | — | Display order |
| `active` | boolean | ❌ | — | Whether to show |

---

#### List Distributor Media

```
GET /api/admin/distributors/{id}/media
```

---

#### Delete Distributor Media

```
DELETE /api/admin/distributors/{id}/media/{mediaId}
```

---

## 10. Calculators

These power the financial calculators in the app. All are admin-only, no dashboard role needed.

---

### 10.1 Circle Rate Rules

Circle rates (government reference rates) used in stamp duty and registration calculations.

**Base path:** `/api/admin/circle-rates`

#### Create Circle Rate Rule

```
POST /api/admin/circle-rates
```

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `stateName` | string | ✅ | — | State name (e.g. `Karnataka`) |
| `cityName` | string | ✅ | — | City name (e.g. `Bengaluru`) |
| `localityName` | string | ✅ | — | Locality / sub-area (e.g. `Whitefield`) |
| `propertyType` | string (enum) | ✅ | `RESIDENTIAL`, `COMMERCIAL`, `PLOT`, `AGRICULTURAL` | Type of property |
| `unitType` | string (enum) | ✅ | `SQ_FT`, `SQ_MT`, `ACRE`, `GUNTA` | Area unit |
| `formulaType` | string (enum) | ✅ | `PER_UNIT`, `FLAT` | How the rate is applied |
| `ratePerUnit` | number | ✅ | > 0 | Government circle rate (INR per unit) |
| `effectiveFrom` | string | ✅ | `YYYY-MM-DD` | Date this rate became effective |
| `effectiveTo` | string | ❌ | `YYYY-MM-DD` | Date this rate expires (null = currently active) |
| `active` | boolean | ✅ | — | Whether to use in calculations |
| `sourceNote` | string | ❌ | — | Source reference (e.g. "Bangalore BDA Circular 2024-25") |

**Example:**
```json
{
  "stateName": "Karnataka",
  "cityName": "Bengaluru",
  "localityName": "Whitefield",
  "propertyType": "RESIDENTIAL",
  "unitType": "SQ_FT",
  "formulaType": "PER_UNIT",
  "ratePerUnit": 5500.00,
  "effectiveFrom": "2024-04-01",
  "active": true,
  "sourceNote": "BBMP Guidance Value 2024-25"
}
```

---

#### Update Circle Rate Rule

```
PUT /api/admin/circle-rates/{id}
```

---

#### List All Circle Rate Rules

```
GET /api/admin/circle-rates
```

---

### 10.2 Stamp Duty Rules

Stamp duty and registration percentages per state/city/buyer combination.

**Base path:** `/api/admin/stamp-duty`

#### Create Stamp Duty Rule

```
POST /api/admin/stamp-duty
```

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `stateName` | string | ✅ | — | State (e.g. `Karnataka`) |
| `cityName` | string | ✅ | — | City (e.g. `Bengaluru`) |
| `buyerType` | string (enum) | ✅ | `MALE`, `FEMALE`, `JOINT`, `COMPANY` | Type of buyer |
| `propertyCategory` | string (enum) | ✅ | `RESIDENTIAL`, `COMMERCIAL`, `PLOT`, `AGRICULTURAL` | Property category |
| `stampDutyPercent` | number | ✅ | ≥ 0.0 | Stamp duty as % of property value |
| `registrationPercent` | number | ✅ | ≥ 0.0 | Registration charge as % of property value |
| `localBodyTaxPercent` | number | ✅ | ≥ 0.0 | Local body tax as % |
| `effectiveFrom` | string | ✅ | `YYYY-MM-DD` | When this rule applies from |
| `effectiveTo` | string | ❌ | `YYYY-MM-DD` | When this rule expires |
| `active` | boolean | ✅ | — | Whether to use in calculator |
| `sourceNote` | string | ❌ | — | Official source reference |

**Example:**
```json
{
  "stateName": "Karnataka",
  "cityName": "Bengaluru",
  "buyerType": "FEMALE",
  "propertyCategory": "RESIDENTIAL",
  "stampDutyPercent": 3.0,
  "registrationPercent": 1.0,
  "localBodyTaxPercent": 0.5,
  "effectiveFrom": "2024-04-01",
  "active": true,
  "sourceNote": "Karnataka Revenue Department Circular 2024"
}
```

---

#### Update Stamp Duty Rule

```
PUT /api/admin/stamp-duty/{id}
```

---

#### List All Stamp Duty Rules

```
GET /api/admin/stamp-duty
```

---

### 10.3 Interior Cost Rules

Pricing rules for interior design cost estimates.

**Base path:** `/api/admin/interior-cost`

#### Create Base Rule

A base rule defines the cost per sq ft for a given scope, package, and property configuration.

```
POST /api/admin/interior-cost/base-rules
```

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `companyId` | number | ✅ | Valid company ID | Interior design company these rates belong to |
| `cityName` | string | ✅ | — | City (e.g. `Bengaluru`) |
| `propertyType` | string (enum) | ✅ | `APARTMENT`, `VILLA`, `PENTHOUSE` | Property type |
| `areaUnit` | string (enum) | ✅ | `SQ_FT`, `SQ_MT` | Area measurement unit |
| `bhkType` | string (enum) | ✅ | `BHK_1`, `BHK_2`, `BHK_3`, `BHK_4_PLUS`, `STUDIO` | BHK configuration |
| `packageType` | string (enum) | ✅ | `BASIC`, `PREMIUM`, `LUXURY` | Design package tier |
| `scopeType` | string (enum) | ✅ | `FULL_HOME`, `MODULAR_KITCHEN`, `BEDROOM`, `LIVING_ROOM` | Scope of interior work |
| `minArea` | number | ✅ | > 0 | Minimum area this rule applies to |
| `maxArea` | number | ✅ | > 0 | Maximum area this rule applies to |
| `baseRatePerUnit` | number | ✅ | > 0 | Base price per area unit (INR) |
| `minimumProjectCost` | number | ✅ | ≥ 0 | Minimum total project cost regardless of area |
| `contingencyPercent` | number | ✅ | ≥ 0 | Buffer % added to estimate |
| `taxPercent` | number | ✅ | ≥ 0 | GST / tax % |
| `effectiveFrom` | string | ✅ | `YYYY-MM-DD` | Rate effective date |
| `effectiveTo` | string | ❌ | `YYYY-MM-DD` | Rate expiry date |
| `active` | boolean | ✅ | — | Whether to use in calculator |
| `sourceNote` | string | ❌ | — | Data source note |

---

#### Create Addon Rule

Addons are per-item extras on top of the base rate (e.g. false ceiling, wallpaper, etc.).

```
POST /api/admin/interior-cost/addon-rules
```

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `companyId` | number | ✅ | Valid company ID | Interior design company |
| `cityName` | string | ✅ | — | City |
| `packageType` | string (enum) | ✅ | `BASIC`, `PREMIUM`, `LUXURY` | Package this addon applies to |
| `addonType` | string (enum) | ✅ | See values below | Type of addon |
| `unitPrice` | number | ✅ | ≥ 0 | Price per unit of this addon (INR) |
| `effectiveFrom` | string | ✅ | `YYYY-MM-DD` | Effective date |
| `effectiveTo` | string | ❌ | `YYYY-MM-DD` | Expiry date |
| `active` | boolean | ✅ | — | Whether active |
| `sourceNote` | string | ❌ | — | Data source |

**`addonType` values:**
```
FALSE_CEILING, WALLPAPER, POOJA_UNIT, TV_UNIT,
WARDROBE, STUDY_TABLE, SHOE_RACK, CROCKERY_UNIT, OTHER
```

---

#### List All Base Rules

```
GET /api/admin/interior-cost/base-rules
```

---

#### List All Addon Rules

```
GET /api/admin/interior-cost/addon-rules
```

---

## 11. Media Upload (Presign)

Use this endpoint to get a pre-signed S3 URL for direct file upload from the browser. This avoids routing large files through your backend.

**Upload flow:**
1. Call `POST /api/dashboard/media/presign-upload` → get `uploadUrl`, `requiredHeaders`, and `publicUrl`
2. `PUT` the file directly to `uploadUrl` from the browser — **include every header in `requiredHeaders` exactly as returned** (no other auth headers needed for S3)
3. Save `publicUrl` (strip any query-string if needed) to the relevant entity (project, builder, etc.)

> **Important:** The presigned URL is signed for a specific set of headers (`Content-Type` and `Cache-Control`). If your PUT request is missing any of the `requiredHeaders` values, S3 will return `403 Forbidden`. Always spread `requiredHeaders` directly into your PUT headers rather than constructing them manually.

```
POST /api/dashboard/media/presign-upload
```

**Access:** A, DE

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `uploadType` | string (enum) | ✅ | See values below | What you're uploading |
| `contentType` | string | ✅ | `image/jpeg`, `image/jpg`, `image/png`, `image/webp`, or `application/pdf` | MIME type of the file |
| `fileSizeBytes` | number | ✅ | ≥ 1 | File size in bytes (used for validation) |
| `projectId` | number | ❌ | Required when uploadType is project-related | Project this upload belongs to |
| `builderId` | number | ❌ | Required when uploadType is `BUILDER_LOGO` | Builder this logo belongs to |

**`uploadType` values and when to use each:**

| Value | When to use | Required extra field |
|-------|-------------|---------------------|
| `PROJECT_IMAGE` | Project gallery photos | `projectId` |
| `FLOOR_PLAN_IMAGE` | Floor plan layout images | `projectId` |
| `CONNECTIVITY_MAP` | Location map image for connectivity section | `projectId` |
| `BROCHURE_PDF` | Project brochure PDF | `projectId` |
| `BUILDER_LOGO` | Builder's logo image | `builderId` |

**Example:**
```json
{
  "uploadType": "PROJECT_IMAGE",
  "contentType": "image/webp",
  "fileSizeBytes": 524288,
  "projectId": 123
}
```

**Response:** `DashboardPresignUploadResponse`
```json
{
  "uploadUrl": "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/dashboard/projects/123/images/abc123.jpg?X-Amz-Algorithm=...",
  "storageKey": "dashboard/projects/123/images/abc123.jpg",
  "publicUrl": "https://cdn.yourdomain.com/dashboard/projects/123/images/abc123.jpg",
  "expiresInSeconds": 300,
  "requiredHeaders": {
    "Content-Type": "image/jpeg",
    "Cache-Control": "public, max-age=31536000, immutable"
  }
}
```

**Performing the S3 PUT:**
```ts
await fetch(uploadUrl, {
  method: "PUT",
  headers: requiredHeaders,   // spread exactly as returned — do not add or omit any header
  body: file,
});
```

After the PUT to `uploadUrl` succeeds, save `publicUrl` (strip query-string with `.split("?")[0]`) as the `url` field in the media/logo endpoint.

---

## 12. Review & Field Issues

These endpoints power the field-level review annotation system. Reviewers can flag individual fields as wrong, and data entry users can mark them fixed.

**Base path:** `/api/dashboard/reviews`

---

### Mark a Field Issue

Reviewer flags a specific field on an entity as incorrect or needing recheck.

```
POST /api/dashboard/reviews/field-issues
```

**Access:** A, R

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `entityType` | string (enum) | ✅ | See values below | Type of entity the field belongs to |
| `entityId` | number | ✅ | — | ID of that entity |
| `fieldKey` | string | ✅ | max 120 chars | Machine-readable field identifier (e.g. `name`, `reraNumber`, `priceMin`) |
| `fieldLabel` | string | ❌ | max 180 chars | Human-readable field name (e.g. "RERA Number") |
| `status` | string (enum) | ✅ | `WRONG` or `RECHECK` | Severity |
| `remarks` | string | ❌ | max 1000 chars | Explanation of the issue |

**`entityType` values:**
```
PROJECT, PROJECT_MEDIA, PROJECT_FLOOR_PLAN,
PROJECT_HIGHLIGHT, PROJECT_CONNECTIVITY,
PROJECT_CONNECTIVITY_PLACE
```

**Example:**
```json
{
  "entityType": "PROJECT",
  "entityId": 123,
  "fieldKey": "reraNumber",
  "fieldLabel": "RERA Number",
  "status": "WRONG",
  "remarks": "RERA number format is incorrect — should be PRM/KA/RERA/... format."
}
```

---

### List Field Issues

```
GET /api/dashboard/reviews/field-issues?entityType=PROJECT&entityId=123&activeOnly=true
```

**Access:** A, R, DE

**Query params:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `entityType` | string | ✅ | Entity type (see values above) |
| `entityId` | number | ✅ | Entity ID |
| `activeOnly` | boolean | ❌ | `true` = open issues only; `false` = full history (default: `false`) |

---

### Mark Issue as Fixed

Data entry marks a flagged field as corrected.

```
PATCH /api/dashboard/reviews/field-issues/{issueId}/fixed
```

**Access:** A, DE

**Request Body (optional):**
```json
{
  "remarks": "Updated RERA number to correct format."
}
```

---

### Delete Field Issue

Reviewer removes an issue they flagged by mistake.

```
DELETE /api/dashboard/reviews/field-issues/{issueId}
```

**Access:** A, R

---

### Review History

Full timeline of review status changes for any entity.

```
GET /api/dashboard/reviews/history?entityType=PROJECT&entityId=123
```

**Access:** A, R, DE

**Response:** Array of `ReviewHistoryResponse`
```json
[
  {
    "id": 1,
    "entityType": "PROJECT",
    "entityId": 123,
    "fromStatus": "DRAFT",
    "toStatus": "PENDING_REVIEW",
    "remarks": "Ready for review",
    "performedByUserId": 7,
    "performedAt": "2025-03-15T10:30:00Z"
  }
]
```

---

## 13. Audit Log

Every write action on the dashboard is automatically recorded. Use this to track who changed what and when.

**Base path:** `/api/dashboard/audit`

> **Access:** A only for all audit endpoints.

### Audit Log for a Project

```
GET /api/dashboard/audit/projects/{projectId}?page=0&size=50
```

---

### Audit Log for a Dashboard User

```
GET /api/dashboard/audit/users/{userId}?page=0&size=50
```

---

### Global Audit Log (with filters)

```
GET /api/dashboard/audit?fromDate=2025-01-01T00:00:00Z&toDate=2025-03-31T23:59:59Z&action=PROJECT_APPROVED&entityType=PROJECT&userRole=REVIEWER&page=0&size=50
```

**Query params:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `fromDate` | ISO 8601 datetime | ❌ | Filter start date |
| `toDate` | ISO 8601 datetime | ❌ | Filter end date |
| `action` | string (enum) | ❌ | Specific action to filter |
| `entityType` | string (enum) | ❌ | `PROJECT`, `PROJECT_MEDIA`, etc. |
| `userRole` | string (enum) | ❌ | `ADMIN`, `REVIEWER`, `DATA_ENTRY` |
| `page` | number | ❌ | Page index (default: 0, max size: 200) |
| `size` | number | ❌ | Page size (default: 50) |

**`action` values:**
```
PROJECT_CREATED, PROJECT_UPDATED, PROJECT_PUBLISHED, PROJECT_UNPUBLISHED,
PROJECT_ACTIVATED, PROJECT_DEACTIVATED, PROJECT_DELETED,
PROJECT_SUBMITTED_FOR_REVIEW, PROJECT_APPROVED, PROJECT_REJECTED, PROJECT_REOPENED,
MEDIA_ADDED, MEDIA_UPDATED, MEDIA_DELETED,
FLOOR_PLAN_CREATED, FLOOR_PLAN_UPDATED, FLOOR_PLAN_ACTIVATED, FLOOR_PLAN_DELETED,
HIGHLIGHT_CREATED, HIGHLIGHT_UPDATED, HIGHLIGHT_DELETED,
CONNECTIVITY_UPSERTED, CONNECTIVITY_PLACE_ADDED, CONNECTIVITY_PLACE_UPDATED, CONNECTIVITY_PLACE_DELETED
```

**Response:** `Page<DashboardActionAuditEntryDto>`
```json
{
  "content": [
    {
      "id": 1,
      "action": "PROJECT_APPROVED",
      "entityType": "PROJECT",
      "entityId": 123,
      "projectId": 123,
      "performedByUserId": 2,
      "performedByUserRole": "REVIEWER",
      "performedAt": "2025-03-15T14:22:00Z"
    }
  ],
  "totalElements": 87,
  "totalPages": 2,
  "number": 0,
  "size": 50
}
```

---

## 14. Field Help System

Backend-driven contextual help text for every dashboard form field. Data-entry users see an `i` icon next to each field label; clicking it opens a popover with guidance on what the field means, why it is needed, where to collect the value, and an example.

Help text is stored in the database and can be updated by an admin at any time without a frontend deployment.

**Base path:** `/api/dashboard/field-help`

---

### 14.1 List Help for a Module

Returns all active help entries for one module, ordered by `displayOrder` ascending.

```
GET /api/dashboard/field-help?module=PROJECT_METER_CONSTRUCTION_STAGE
```

**Access:** A, R, DE

**Query params:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `module` | string (enum) | ✅ | Module identifier — see values below |

**`module` values:**

| Value | Covers |
|-------|--------|
| `PROJECT_BASIC` | Core project fields (name, RERA number, status, dates, price) |
| `PROJECT_MEDIA` | Project gallery media fields |
| `PROJECT_FLOOR_PLAN` | Floor plan fields |
| `PROJECT_HIGHLIGHT` | Highlight / USP fields |
| `PROJECT_CONNECTIVITY` | Connectivity overview and places |
| `PROJECT_METER_CONSTRUCTION_STAGE` | Construction stage form fields |
| `PROJECT_METER_COMPLIANCE` | Compliance item fields |
| `PROJECT_METER_AMENITY` | Amenity fields |
| `PROJECT_METER_PRICE_HISTORY` | Price history fields |
| `PROJECT_METER_PAYMENT_MILESTONE` | Payment milestone fields |
| `PROJECT_METER_COST_BREAKDOWN` | Cost breakdown fields |
| `PROJECT_METER_LAND_UTILIZATION` | Land utilization fields |
| `PROJECT_METER_LOCATION_SCORE` | Location score fields |
| `BUILDER` | Builder profile fields |

**Response:** `DashboardFieldHelpResponse[]`
```json
[
  {
    "id": 1,
    "module": "PROJECT_METER_CONSTRUCTION_STAGE",
    "fieldKey": "weightPercent",
    "fieldLabel": "Weight %",
    "shortHelp": "Importance of this stage in total construction progress.",
    "detailedHelp": "Weight percentage defines how much this stage contributes to the total construction progress score.",
    "whyNeeded": "Used to calculate overall construction progress in Project Meter.",
    "sourceHint": "Use internal construction weighting, project schedule, or engineer estimate.",
    "exampleValue": "20",
    "validationHint": "Enter value between 0 and 100. Total stage weights should ideally equal 100.",
    "active": true,
    "displayOrder": 5
  }
]
```

**Response fields:**

| Field | Type | Description |
|-------|------|-------------|
| `id` | number | Unique ID |
| `module` | string | Module this help belongs to |
| `fieldKey` | string | Machine-readable field identifier matching the form field name |
| `fieldLabel` | string | Human-readable label shown in the popover title |
| `shortHelp` | string | One-sentence summary shown at the top of the popover |
| `detailedHelp` | string \| null | Longer explanation (optional) |
| `whyNeeded` | string \| null | Why this field matters for the product |
| `sourceHint` | string \| null | Where the data entry user should collect this value from |
| `exampleValue` | string \| null | Concrete example value |
| `validationHint` | string \| null | Constraint reminder for the user |
| `active` | boolean | Whether this help entry is currently shown |
| `displayOrder` | number | Ordering within the module |

---

### 14.2 Get Help for a Single Field

```
GET /api/dashboard/field-help/{module}/{fieldKey}
```

**Access:** A, R, DE

**Path params:**

| Param | Description |
|-------|-------------|
| `module` | Module enum value (e.g. `PROJECT_METER_CONSTRUCTION_STAGE`) |
| `fieldKey` | Field key (e.g. `weightPercent`) |

**Response:** Single `DashboardFieldHelpResponse` (same shape as above)

Returns `404` if no entry exists for that module + fieldKey combination.

---

### 14.3 Create or Update Help Text (Upsert)

Creates a new entry or updates an existing one. Identity is determined by `(module, fieldKey)` — if a record exists for that pair it is updated; otherwise a new record is created.

```
PUT /api/dashboard/field-help
```

**Access:** A only

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `module` | string (enum) | ✅ | Valid module value | Module this help belongs to |
| `fieldKey` | string | ✅ | Not blank | Machine-readable field identifier |
| `fieldLabel` | string | ✅ | Not blank | Human-readable label |
| `shortHelp` | string | ✅ | Not blank | One-sentence summary |
| `detailedHelp` | string | ❌ | — | Longer explanation |
| `whyNeeded` | string | ❌ | — | Why the field matters |
| `sourceHint` | string | ❌ | — | Where to collect this data |
| `exampleValue` | string | ❌ | — | Example value string |
| `validationHint` | string | ❌ | — | Constraint reminder |
| `active` | boolean | ❌ | — | Defaults to `true` on create |
| `displayOrder` | number | ❌ | — | Defaults to `0` on create |

**Example:**
```json
{
  "module": "PROJECT_METER_CONSTRUCTION_STAGE",
  "fieldKey": "weightPercent",
  "fieldLabel": "Weight %",
  "shortHelp": "Importance of this stage in total construction progress.",
  "detailedHelp": "Weight percentage defines how much this stage contributes to the total construction progress score.",
  "whyNeeded": "Used to calculate overall construction progress in Project Meter.",
  "sourceHint": "Use internal construction weighting, project schedule, or engineer estimate.",
  "exampleValue": "20",
  "validationHint": "Enter value between 0 and 100. Total stage weights should ideally equal 100.",
  "displayOrder": 5
}
```

**Response:** `DashboardFieldHelpResponse` (the created or updated record)

---

### 14.4 Activate / Deactivate a Help Entry

Toggles visibility of a help entry without deleting it. Deactivated entries are excluded from the list endpoint.

```
PATCH /api/dashboard/field-help/{id}/active?value=false
```

**Access:** A only

**Path params:**

| Param | Description |
|-------|-------------|
| `id` | Help entry ID |

**Query params:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `value` | boolean | ✅ | `true` to activate, `false` to deactivate |

**Response:** `204 No Content`

---

### 14.5 Delete a Help Entry (Soft Delete)

Marks the entry as inactive (`active = false`). The record is retained in the database but will not appear in list responses.

```
DELETE /api/dashboard/field-help/{id}
```

**Access:** A only

**Response:** `204 No Content`

---

### Seeded Help Data

All Project Meter module fields are pre-seeded on database migration (V71 + V72) and immediately available via the list endpoint.

**`PROJECT_METER_CONSTRUCTION_STAGE`** (V71 — 12 fields)

| `fieldKey` | `fieldLabel` |
|------------|-------------|
| `stageCode` | Stage Code |
| `displayOrder` | Display Order |
| `stageLabel` | Stage Label |
| `status` | Status |
| `weightPercent` | Weight % |
| `progressPercent` | Progress % |
| `plannedStartDate` | Planned Start |
| `plannedEndDate` | Planned End |
| `actualStartDate` | Actual Start |
| `actualEndDate` | Actual End |
| `evidenceCount` | Evidence Count |
| `verified` | Verified |

**`PROJECT_METER_COMPLIANCE`** (V72 — 9 fields)

| `fieldKey` | `fieldLabel` |
|------------|-------------|
| `itemGroup` | Group |
| `itemKey` | Item Key |
| `itemLabel` | Item Label |
| `status` | Status |
| `displayOrder` | Display Order |
| `valueText` | Value / Reference |
| `documentUrl` | Document URL |
| `remarks` | Remarks |
| `verified` | Verified |

**`PROJECT_METER_AMENITY`** (V72 — 8 fields)

| `fieldKey` | `fieldLabel` |
|------------|-------------|
| `amenityCode` | Amenity Code |
| `amenityLabel` | Amenity Label |
| `status` | Status |
| `progressPercent` | Progress % |
| `weightPercent` | Weight % |
| `displayOrder` | Order |
| `remarks` | Remarks |
| `verified` | Verified |

**`PROJECT_METER_PRICE_HISTORY`** (V72 — 5 fields)

| `fieldKey` | `fieldLabel` |
|------------|-------------|
| `yearLabel` | Year / Month |
| `projectPrice` | Project Price |
| `averageAreaPrice` | Area Avg Price |
| `displayOrder` | Display Order |
| `verified` | Verified |

**`PROJECT_METER_PAYMENT_MILESTONE`** (V72 — 7 fields)

| `fieldKey` | `fieldLabel` |
|------------|-------------|
| `milestoneCode` | Milestone Code |
| `milestoneLabel` | Milestone Label |
| `percentageValue` | Percentage % |
| `displayOrder` | Display Order |
| `linkedStageCode` | Linked Stage Code |
| `description` | Description |
| `active` | Active |

**`PROJECT_METER_COST_BREAKDOWN`** (V72 — 8 fields)

| `fieldKey` | `fieldLabel` |
|------------|-------------|
| `landCost` | Land Cost |
| `constructionCost` | Construction Cost |
| `infrastructureCost` | Infrastructure Cost |
| `otherCost` | Other Cost |
| `totalCost` | Total Cost (manual override) |
| `sourceLabel` | Source Label |
| `remarks` | Remarks |
| `verified` | Verified |

**`PROJECT_METER_LAND_UTILIZATION`** (V72 — 10 fields)

| `fieldKey` | `fieldLabel` |
|------------|-------------|
| `areaUnit` | Area Unit |
| `totalLandArea` | Total Land Area |
| `builtUpArea` | Built-up Area |
| `openArea` | Open / Green Area |
| `parkingArea` | Parking Area |
| `amenitiesArea` | Amenities Area |
| `otherArea` | Other Area |
| `sourceLabel` | Source Label |
| `remarks` | Remarks |
| `verified` | Verified |

**`PROJECT_METER_LOCATION_SCORE`** (V72 — 10 fields)

| `fieldKey` | `fieldLabel` |
|------------|-------------|
| `connectivityScore` | Connectivity Score |
| `infrastructureScore` | Infrastructure Score |
| `socialInfraScore` | Social Infrastructure Score |
| `appreciationScore` | Appreciation Score |
| `safetyScore` | Safety Score |
| `greeneryScore` | Greenery Score |
| `overallScore` | Overall Score (manual override) |
| `sourceLabel` | Source Label |
| `remarks` | Remarks |
| `verified` | Verified |

Modules not listed above (`PROJECT_BASIC`, `PROJECT_MEDIA`, `PROJECT_FLOOR_PLAN`, `PROJECT_HIGHLIGHT`, `PROJECT_CONNECTIVITY`, `BUILDER`) have no pre-seeded entries — use the upsert endpoint to add help text for those fields.

---

## 15. Data Imports (RERA Scraping)

**Base path:** `/api/dashboard/scraping`

The Data Imports module lets admins search a public RERA portal by registration number, review the extracted data as a *candidate*, and then apply it to a new or existing project draft. Nothing is published automatically — every candidate goes through the normal review workflow.

### RERA Sources Available

| `sourceCode` | Portal | Status |
|---|---|---|
| `HARYANA_RERA` | haryanarera.gov.in | **Active** — district-based DataTable search |
| `UP_RERA` | up-rera.in | **Active** — direct registration-number search, Angular SPA |
| `MAHA_RERA` | maharera.mahaonline.gov.in | Coming soon |
| `KARNATAKA_RERA` | rera.karnataka.gov.in | Coming soon |

### Candidate Status Machine

```
SCRAPED → NEEDS_REVIEW → READY_TO_APPLY → APPLIED (terminal)
                                        ↘ REJECTED (terminal)
FAILED  (set automatically when scrape yields no data)
```

| Status | Description |
|---|---|
| `SCRAPED` | Live scrape ran and data was extracted |
| `NEEDS_REVIEW` | Admin flagged the candidate for closer inspection |
| `READY_TO_APPLY` | Reviewed and approved — ready to import |
| `APPLIED` | Candidate data has been applied to a project (terminal) |
| `REJECTED` | Candidate discarded — will not be applied (terminal) |
| `FAILED` | Scrape ran but returned no usable data |

---

### 15.1 Search RERA by Number (no persistence)

```
POST /api/dashboard/scraping/rera/search-by-number
```

**Access:** ADMIN only

Runs a live headless-browser scrape against the selected RERA portal and returns extracted candidate data. **Nothing is saved to the database.** Use [15.2](#152-save-scrape-candidate) to both scrape and persist in one call.

> Typical response time: **10–60 seconds** (browser launch + portal navigation).

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `sourceCode` | `ReraSourceCode` | ✅ | Valid enum value | RERA authority to search |
| `reraNumber` | string | ✅ | Not blank, max 100 chars | Registration number as shown on the portal |
| `saveEvidence` | boolean | ❌ | Default `true` | Save raw HTML + screenshot to `/tmp/sfs-scrapes/` |
| `includeRaw` | boolean | ❌ | Default `true` | Include `allExtractedKeyValues` map in response |

**Example:**
```json
{
  "sourceCode": "UP_RERA",
  "reraNumber": "UPRERAPRJ12345",
  "saveEvidence": true,
  "includeRaw": false
}
```

**Response:** `ReraNumberSearchResponse`

| Field | Type | Description |
|-------|------|-------------|
| `sourceCode` | string | Echo of request |
| `reraNumber` | string | Echo of request |
| `found` | boolean | Whether data was successfully extracted |
| `captchaDetected` | boolean | Portal returned a CAPTCHA / anti-bot page |
| `requestedAt` | ISO-8601 | Browser navigation start time |
| `sourceSearchUrl` | string | Portal search page URL |
| `sourceDetailUrl` | string | Project detail page URL (null if not navigated) |
| `finalUrl` | string | Browser's final URL after all navigation |
| `title` | string | Page `<title>` |
| `rawHtmlPath` | string | Absolute path of saved HTML file (null if `saveEvidence=false`) |
| `screenshotPath` | string | Absolute path of saved PNG (null if `saveEvidence=false`) |
| `summary` | object | Confidence summary — see below |
| `projectCandidate` | object | Extracted project fields |
| `builderCandidate` | object | Extracted builder / promoter fields |
| `complianceCandidates` | array | Compliance items found (always includes `RERA_REGISTRATION`) |
| `fieldResults` | array | Per-field found / missing detail |
| `missingFields` | array | Only the missing fields (null if all found) |
| `warnings` | array | Non-fatal warnings (null if none) |
| `raw` | object | `{ "allExtractedKeyValues": {...} }` (null if `includeRaw=false`) |

**`summary` object:**

| Field | Type | Description |
|-------|------|-------------|
| `totalExpectedFields` | int | Fixed: 18 |
| `foundFields` | int | Fields where a value was extracted |
| `missingFields` | int | `totalExpectedFields − foundFields` |
| `highConfidenceFields` | int | Found fields with confidence ≥ 80 |
| `lowConfidenceFields` | int | Found fields with confidence < 80 |
| `confidenceScore` | int | `round(foundFields × 100 / totalExpectedFields)` |
| `status` | string | `COMPLETE` / `PARTIAL` / `LOW_CONFIDENCE` / `NOT_FOUND` / `BLOCKED` |

**Success example:**
```json
{
  "sourceCode": "UP_RERA",
  "reraNumber": "UPRERAPRJ12345",
  "found": true,
  "captchaDetected": false,
  "requestedAt": "2026-05-12T10:15:00Z",
  "sourceSearchUrl": "https://www.up-rera.in/Prodetails",
  "sourceDetailUrl": "https://www.up-rera.in/ProjectDetails?id=12345",
  "finalUrl": "https://www.up-rera.in/ProjectDetails?id=12345",
  "title": "Project Details — UP RERA",
  "rawHtmlPath": "/tmp/sfs-scrapes/rera-up_rera-20260512101500-a1b2c3d4.html",
  "screenshotPath": "/tmp/sfs-scrapes/rera-up_rera-20260512101500-a1b2c3d4.png",
  "summary": {
    "totalExpectedFields": 18,
    "foundFields": 10,
    "missingFields": 8,
    "highConfidenceFields": 7,
    "lowConfidenceFields": 3,
    "confidenceScore": 56,
    "status": "PARTIAL"
  },
  "projectCandidate": {
    "name": "Green Valley Residency",
    "cityName": "Lucknow",
    "addressLine": "Sector 12, Gomti Nagar, Lucknow",
    "reraNumber": "UPRERAPRJ12345",
    "possessionDate": "2027-03-31",
    "projectStatus": "UNDER_CONSTRUCTION",
    "propertyTypes": ["APARTMENT"]
  },
  "builderCandidate": {
    "name": "ABC Developers Pvt. Ltd.",
    "phone": "9876543210",
    "email": "contact@abcdev.com",
    "addressLine": "12, Hazratganj, Lucknow"
  },
  "complianceCandidates": [
    {
      "itemGroup": "RERA",
      "itemKey": "RERA_REGISTRATION",
      "itemLabel": "RERA Registration",
      "status": "OBTAINED",
      "valueText": "UPRERAPRJ12345",
      "documentUrl": "https://www.up-rera.in/ProjectDetails?id=12345",
      "remarks": "Extracted from RERA portal",
      "displayOrder": 1,
      "verified": false
    }
  ],
  "warnings": null
}
```

**CAPTCHA / blocked example:**
```json
{
  "sourceCode": "UP_RERA",
  "reraNumber": "UPRERAPRJ12345",
  "found": false,
  "captchaDetected": true,
  "summary": { "status": "BLOCKED", "confidenceScore": 0 },
  "warnings": ["CAPTCHA or anti-bot challenge detected. Scrape aborted."]
}
```

---

### 15.2 Save Scrape Candidate

```
POST /api/dashboard/scraping/candidates
```

**Access:** ADMIN only  
**HTTP Status on success:** `201 Created`

Runs a live scrape (same as 15.1) **and** persists the full result as a candidate record. Always saves — including failed or CAPTCHA-blocked scrapes. Returns the full `ScrapeCandidateDetailResponse`.

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `sourceCode` | `ReraSourceCode` | ✅ | Valid enum value | RERA authority to search |
| `reraNumber` | string | ✅ | Not blank, max 100 chars | Registration number |
| `saveEvidence` | boolean | ❌ | Default `true` | Save raw HTML + screenshot to disk |

**Example:**
```json
{
  "sourceCode": "UP_RERA",
  "reraNumber": "UPRERAPRJ12345",
  "saveEvidence": true
}
```

**Response:** `ScrapeCandidateDetailResponse` — see [15.4](#154-get-candidate-detail) for the full field list.

---

### 15.3 List Candidates

```
GET /api/dashboard/scraping/candidates
```

**Access:** ADMIN, DATA_ENTRY, REVIEWER

Returns a paginated list of candidate summaries. Supports optional filtering by status, source, and RERA number.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | `ScrapeCandidateStatus` | ❌ | Filter by candidate status |
| `sourceCode` | `ReraSourceCode` | ❌ | Filter by RERA source |
| `reraNumber` | string | ❌ | Filter by exact or partial RERA number |
| `page` | int | ❌ | Page number, 0-indexed (default `0`) |
| `size` | int | ❌ | Page size (default `20`) |
| `sort` | string | ❌ | Sort field (default `createdAt,desc`) |

**Example:**
```
GET /api/dashboard/scraping/candidates?status=READY_TO_APPLY&sourceCode=UP_RERA&page=0&size=20
```

**Response:** Spring `Page<ScrapeCandidateSummaryDto>`

Each item in `content`:

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Candidate ID |
| `sourceCode` | string | RERA authority |
| `reraNumber` | string | Registration number searched |
| `projectName` | string | Extracted project name (null if scrape failed) |
| `builderName` | string | Extracted promoter name (null if scrape failed) |
| `found` | boolean | Whether data was extracted |
| `confidenceScore` | int | 0–100 |
| `confidenceStatus` | string | `COMPLETE` / `PARTIAL` / `LOW_CONFIDENCE` / `NOT_FOUND` / `BLOCKED` |
| `foundFields` | int | Number of fields successfully extracted |
| `missingFields` | int | Number of fields not found |
| `status` | string | Candidate workflow status |
| `linkedBuilderId` | long | Builder linked to this candidate (null if not linked) |
| `linkedProjectId` | long | Project linked during apply (null if not applied) |
| `appliedProjectId` | long | Project that candidate data was applied to (null if not applied) |
| `sourceDetailUrl` | string | Portal detail page URL |
| `createdAt` | ISO-8601 | When the scrape was saved |
| `updatedAt` | ISO-8601 | Last status / link change |

---

### 15.4 Get Candidate Detail

```
GET /api/dashboard/scraping/candidates/{id}
```

**Access:** ADMIN, DATA_ENTRY, REVIEWER

Returns the full candidate record including all extracted child data, field-level evidence, and raw values.

**Path Parameter:** `id` — candidate ID from the list endpoint.

**Response:** `ScrapeCandidateDetailResponse`

**Core fields:**

| Field | Type | Description |
|-------|------|-------------|
| `id` | long | Candidate ID |
| `sourceCode` | string | RERA authority |
| `reraNumber` | string | Registration number |
| `found` | boolean | Scrape extracted data |
| `captchaDetected` | boolean | Blocked by CAPTCHA |
| `sourceSearchUrl` | string | Portal search page |
| `sourceDetailUrl` | string | Portal detail page |
| `finalUrl` | string | Browser final URL |
| `pageTitle` | string | Detail page title |
| `rawHtmlPath` | string | Saved HTML file path |
| `screenshotPath` | string | Saved PNG file path |
| `confidenceScore` | int | 0–100 |
| `confidenceStatus` | string | Overall scrape quality |
| `totalExpectedFields` | int | Always 18 |
| `foundFields` | int | Fields extracted |
| `missingFields` | int | Fields not found |
| `status` | string | Candidate workflow status |
| `linkedBuilderId` | long | Linked builder (null if none) |
| `linkedProjectId` | long | Linked project (null if none) |
| `appliedProjectId` | long | Applied project (null if not applied) |
| `remarks` | string | Admin remarks (e.g. rejection reason) |
| `appliedAt` | ISO-8601 | When apply was executed (null if not applied) |
| `createdAt` | ISO-8601 | Scrape timestamp |
| `updatedAt` | ISO-8601 | Last update |

**Nested objects:**

| Field | Type | Description |
|-------|------|-------------|
| `projectCandidate` | object | Extracted project fields (name, city, address, price, dates, RERA number, status, property types) |
| `builderCandidate` | object | Extracted builder fields (name, phone, email, address, city) |
| `complianceCandidates` | array | Compliance items — always includes `RERA_REGISTRATION` |
| `costBreakdownCandidate` | object | Cost breakdown data (null if not found) |
| `landUtilizationCandidate` | object | Land utilization data (null if not found) |
| `documentCandidates` | array | Documents found on the portal page |
| `fieldResults` | array | Per-field found / missing detail with confidence score |
| `missingFieldResults` | array | Only missing fields |
| `rawValues` | array | All raw key-value pairs extracted from the portal HTML |

---

### 15.5 Update Candidate Status

```
PATCH /api/dashboard/scraping/candidates/{id}/status
```

**Access:** ADMIN, DATA_ENTRY

Moves a candidate through the workflow. Use this to mark a candidate as reviewed, ready, or rejected. Remarks are required when setting `REJECTED`.

**Path Parameter:** `id` — candidate ID.

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `status` | `ScrapeCandidateStatus` | ✅ | Not null | Target status |
| `remarks` | string | ❌ (required for `REJECTED`) | max 1000 chars | Admin notes |

**Example — mark ready:**
```json
{
  "status": "READY_TO_APPLY"
}
```

**Example — reject with reason:**
```json
{
  "status": "REJECTED",
  "remarks": "Duplicate of candidate #42 — same project already imported."
}
```

**Response:** Updated `ScrapeCandidateDetailResponse`.

> Terminal statuses (`APPLIED`, `REJECTED`) cannot be changed again.

---

### 15.6 Link Builder to Candidate

```
PATCH /api/dashboard/scraping/candidates/{id}/link-builder
```

**Access:** ADMIN, DATA_ENTRY

Associates an existing builder record with this candidate. Required before applying with `CREATE_NEW_PROJECT` mode if the portal did not extract a builder name that maps to an existing builder. The `builderId` supplied here is used as the default during apply.

**Path Parameter:** `id` — candidate ID.

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `builderId` | long | ✅ | Not null | ID of the existing builder to link |

**Example:**
```json
{
  "builderId": 7
}
```

**Response:** Updated `ScrapeCandidateDetailResponse`.

---

### 15.7 Apply Candidate to Project

```
POST /api/dashboard/scraping/candidates/{id}/apply-to-project
```

**Access:** ADMIN, DATA_ENTRY

Applies extracted candidate data to a project. Two modes are supported:

| `mode` | Behaviour |
|--------|-----------|
| `CREATE_NEW_PROJECT` | Creates a new project draft using candidate data. Requires `builderId` (from request or linked builder). |
| `UPDATE_EXISTING_PROJECT` | Merges candidate data into an existing project. Requires `projectId`. Existing non-null fields are preserved unless `overwrite=true`. |

The resulting project enters the normal `DRAFT → PENDING_REVIEW` workflow — **nothing is published automatically**.

**Path Parameter:** `id` — candidate ID.

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `mode` | `ApplyMode` | ✅ | Not null | `CREATE_NEW_PROJECT` or `UPDATE_EXISTING_PROJECT` |
| `builderId` | long | Conditional | Required for `CREATE_NEW_PROJECT` if candidate has no linked builder | Builder to attach to the new project |
| `projectId` | long | Conditional | Required for `UPDATE_EXISTING_PROJECT` | Project to update |
| `cityId` | long | ❌ | — | City ID mapping (the candidate stores the city name as text; supply the resolved ID here) |
| `overwrite` | boolean | ❌ | Default `false` | When `true`, candidate values overwrite existing non-null project fields |

**Example — create new project:**
```json
{
  "mode": "CREATE_NEW_PROJECT",
  "builderId": 7,
  "cityId": 3,
  "overwrite": false
}
```

**Example — update existing project:**
```json
{
  "mode": "UPDATE_EXISTING_PROJECT",
  "projectId": 105,
  "cityId": 3,
  "overwrite": true
}
```

**Response:** `ApplyToProjectResponse`

| Field | Type | Description |
|-------|------|-------------|
| `createdProject` | boolean | `true` if a new project was created; `false` if an existing one was updated |
| `projectId` | long | ID of the created / updated project |
| `candidateId` | long | ID of the candidate that was applied |
| `fieldsApplied` | int | Number of project fields written from candidate data |
| `fieldsSkipped` | int | Number of fields skipped (already populated and `overwrite=false`) |
| `complianceItemsCreated` | int | New compliance items added to Project Meter |
| `complianceItemsSkipped` | int | Compliance items that already existed |
| `meterSectionsCreated` | array | Names of new Project Meter sections created (e.g. `["LAND_UTILIZATION"]`) |
| `warnings` | array | Non-fatal warnings (null if none) |

**Example response:**
```json
{
  "createdProject": true,
  "projectId": 201,
  "candidateId": 14,
  "fieldsApplied": 8,
  "fieldsSkipped": 0,
  "complianceItemsCreated": 1,
  "complianceItemsSkipped": 0,
  "meterSectionsCreated": ["LAND_UTILIZATION"],
  "warnings": null
}
```

> After apply succeeds the candidate status is automatically set to `APPLIED` (terminal — cannot be changed again).

---

## Quick Reference: Who Can Do What

| Section | ADMIN | REVIEWER | DATA_ENTRY |
|---------|-------|----------|------------|
| Login / Auth | ✅ | ✅ | ✅ |
| Overview | ✅ | ✅ | ✅ |
| Create Builder | ✅ | ❌ | ✅ |
| Update Builder | ✅ | ❌ | ❌ |
| Publish Builder | ✅ | ❌ | ❌ |
| Create Project | ✅ | ❌ | ✅ |
| Update Project | ✅ | ❌ | ✅ |
| Publish Project | ✅ | ❌ | ❌ |
| Delete Project | ✅ | ❌ | ❌ |
| Add/Update Media | ✅ | ❌ | ✅ |
| Delete Media | ✅ | ❌ | ❌ |
| Add Highlights / Floor Plans / Connectivity | ✅ | ❌ | ✅ |
| Delete Highlights / Floor Plans / Connectivity | ✅ | ❌ | ❌ |
| Add/Update Meter Data | ✅ | ❌ | ✅ |
| Recalculate Meter Snapshot | ✅ | ✅ | ❌ |
| Submit Project for Review | ✅ | ❌ | ✅ |
| Approve / Reject Project | ✅ | ✅ | ❌ |
| Flag Field Issues | ✅ | ✅ | ❌ |
| Mark Field Issue Fixed | ✅ | ❌ | ✅ |
| View Review History | ✅ | ✅ | ✅ |
| Audit Log | ✅ | ❌ | ❌ |
| Manage Categories | ✅ | ❌ | ❌ |
| Manage Cities | ✅ | ❌ | ❌ |
| Manage Brands | ✅ | ❌ | ❌ |
| Manage Distributors | ✅ | ❌ | ❌ |
| Manage Calculator Rules | ✅ | ❌ | ❌ |
| Media Upload (presign) | ✅ | ❌ | ✅ |
| View Field Help | ✅ | ✅ | ✅ |
| Manage Field Help (create/update/delete) | ✅ | ❌ | ❌ |
| RERA Search (no persistence) | ✅ | ❌ | ❌ |
| Save Scrape Candidate | ✅ | ❌ | ❌ |
| View Candidates (list + detail) | ✅ | ✅ | ✅ |
| Update Candidate Status / Link Builder | ✅ | ❌ | ✅ |
| Apply Candidate to Project | ✅ | ❌ | ✅ |

---

## Error Responses

All endpoints return standard error shapes:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "name: must not be blank",
  "timestamp": "2025-03-15T10:30:00Z"
}
```

| HTTP Status | Meaning |
|-------------|---------|
| `400` | Validation failed — check the `message` field for which field(s) failed |
| `401` | Missing or expired token |
| `403` | Role not permitted for this action |
| `404` | Entity not found |
| `409` | Conflict (e.g. duplicate slug) |
| `500` | Server error — report to backend team |
