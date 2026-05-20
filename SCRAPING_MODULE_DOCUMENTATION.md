# Scraping Module Documentation

**Project:** Square Foot Story (SFS) Internal Dashboard  
**Module:** `com.brandPitara.sfs.dashboard.scraping`  
**Phases:** S1 — Scraping Feasibility Test · S1B — RERA Number Search Scraper · S2 — Candidate Persistence  
**Status:** S1 Complete · S1B Active (Haryana RERA live · UP RERA live) · S2 Complete  
**Last Updated:** 2026-05-12

---

## Table of Contents

**Phase S1 — Scraping Feasibility Test**
1. [S1 Overview](#1-s1-overview)
2. [S1 Architecture](#2-s1-architecture)
3. [S1 Package Structure](#3-s1-package-structure)
4. [S1 File Reference](#4-s1-file-reference)
5. [S1 API Reference](#5-s1-api-reference)
6. [S1 Request & Response Schemas](#6-s1-request--response-schemas)
7. [S1 Data Flow](#7-s1-data-flow)
8. [SSRF & URL Safety](#8-ssrf--url-safety)
9. [CAPTCHA Detection](#9-captcha-detection)
10. [File Storage](#10-file-storage)
11. [S1 HTML Parsing Logic](#11-s1-html-parsing-logic)
12. [Provider Abstraction](#12-provider-abstraction)

**Phase S1B — RERA Number Search Scraper**

13. [S1B Overview](#13-s1b-overview)
14. [S1B Architecture](#14-s1b-architecture)
15. [S1B Package Structure](#15-s1b-package-structure)
16. [S1B File Reference](#16-s1b-file-reference)
17. [S1B API Reference](#17-s1b-api-reference)
18. [S1B Request & Response Schemas](#18-s1b-request--response-schemas)
19. [S1B Data Flow](#19-s1b-data-flow)
20. [RERA Source Scraper Layer](#20-rera-source-scraper-layer)
21. [ReraDetailKeyValueParser — Extraction Strategies](#21-reradetailkeyvalueparser--extraction-strategies)
22. [ReraProjectCandidateMapper — 18 Expected Fields](#22-reraprojectcandidatemapper--18-expected-fields)
23. [Dashboard Form Alignment](#23-dashboard-form-alignment)
24. [Confidence Scoring](#24-confidence-scoring)
25. [HaryanaReraSourceScraper — Active Implementation](#25-haryanarerasourcescraper--active-implementation)
25.5 [UpReraSourceScraper — Active Implementation](#255-uprerasourcescraper--active-implementation)

**Phase S2 — Candidate Persistence**

33. [S2 Overview](#33-s2-overview)
34. [S2 Architecture](#34-s2-architecture)
35. [S2 Package Structure](#35-s2-package-structure)
36. [S2 Database Schema](#36-s2-database-schema)
37. [S2 Entities](#37-s2-entities)
38. [S2 API Reference](#38-s2-api-reference)
39. [S2 Request & Response Schemas](#39-s2-request--response-schemas)
40. [S2 Data Flow — Save Candidate](#40-s2-data-flow--save-candidate)
41. [S2 Data Flow — Apply to Project](#41-s2-data-flow--apply-to-project)
42. [S2 Status Machine](#42-s2-status-machine)
43. [ScrapeCandidateEntityMapper](#43-scrapecandidateentitymapper)
44. [S2 Testing Guide](#44-s2-testing-guide)

**Common**

26. [Security](#26-security)
27. [Dependencies](#27-dependencies)
28. [Setup & Installation](#28-setup--installation)
29. [Testing Guide](#29-testing-guide)
30. [Error Handling](#30-error-handling)
31. [Limitations](#31-limitations)
32. [Roadmap](#32-roadmap)

---

# PHASE S1 — SCRAPING FEASIBILITY TEST

---

## 1. S1 Overview

Phase S1 introduces a **safe, read-only scraping feasibility endpoint** for the SFS internal dashboard. The goal is to verify that any target public RERA or real-estate page can be:

- Opened in a headless Chromium browser via Playwright Java
- Fully rendered (including JavaScript-driven content)
- Captured as a raw HTML file and a full-page screenshot
- Parsed generically using Jsoup to extract preview records from HTML tables
- Inspected for basic CAPTCHA or anti-bot signals

**This phase does not write to any production database table.** It is a diagnostic and feasibility layer only.

### What Phase S1 proves

| Capability | Verified in S1 |
|---|---|
| Playwright can launch inside Spring Boot | Yes |
| Dynamic JS pages render correctly | Yes |
| Raw evidence (HTML + screenshot) is saved locally | Yes |
| Generic table data can be extracted | Yes |
| CAPTCHA / blocking signals are detectable | Yes |
| URL safety (SSRF protection) is enforced | Yes |
| Provider abstraction exists for paid services | Yes (interface only) |
| Data is written to production tables | No — by design |

---

## 2. S1 Architecture

```
Controller
    └── DashboardScrapingTestServiceImpl (orchestrator)
            ├── ScrapingProvider            ← interface
            │       └── PlaywrightScrapingProvider
            ├── ScrapeFileStorageService    ← interface
            │       └── LocalScrapeFileStorageService
            ├── ReraPreviewParser
            └── ScrapeCaptchaDetector
```

**Design principles applied:**
- **Single Responsibility** — controller, service, browser provider, parser, storage, and CAPTCHA detector are separate classes.
- **Open/Closed** — `ScrapingProvider` and `ScrapeFileStorageService` are interfaces; paid providers (Apify, Zyte) are added without modifying existing code.
- **Dependency Inversion** — `DashboardScrapingTestServiceImpl` depends on interfaces, not implementations.

---

## 3. S1 Package Structure

```
src/main/java/com/brandPitara/sfs/dashboard/scraping/
│
├── controller/
│   └── DashboardScrapingTestController.java
│
├── dto/
│   ├── ScrapeTestUrlRequest.java
│   ├── ScrapeExtractedPreviewDto.java
│   └── ScrapeTestUrlResponse.java
│
├── service/
│   ├── DashboardScrapingTestService.java
│   └── impl/
│       └── DashboardScrapingTestServiceImpl.java
│
├── parser/
│   └── ReraPreviewParser.java
│
├── provider/
│   ├── ScrapingProvider.java           ← interface
│   ├── ScrapeProviderRequest.java
│   ├── ScrapeProviderResult.java
│   └── PlaywrightScrapingProvider.java
│
└── util/
    ├── ScrapeCaptchaDetector.java
    ├── ScrapeFileStorageService.java   ← interface
    ├── LocalScrapeFileStorageService.java
    └── SavedScrapeFile.java
```

---

## 4. S1 File Reference

### Controller

#### `DashboardScrapingTestController`
- **Type:** `@RestController`
- **Mapping:** `POST /api/dashboard/scraping/test-url`
- **Security:** `@PreAuthorize("hasRole('ADMIN')")`
- **Responsibility:** Accept request, delegate to service, return response. No business logic.

### DTOs

#### `ScrapeTestUrlRequest`
| Field | Type | Validation |
|---|---|---|
| `url` | `String` | `@NotBlank`, `@Pattern(^https?://.*)` |

#### `ScrapeExtractedPreviewDto`
| Field | Type | Description |
|---|---|---|
| `projectName` | `String` | Inferred from: `project`, `projectname`, `name` |
| `builderName` | `String` | Inferred from: `builder`, `promoter`, `developer`, `company` |
| `reraNumber` | `String` | Inferred from: `rera`, `registration`, `regno` |
| `cityName` | `String` | Inferred from: `city`, `district`, `location` |
| `statusText` | `String` | Inferred from: `status`, `projectstatus` |
| `sourceUrl` | `String` | First anchor `href` in row (absolute) or base URL |
| `raw` | `Map<String, Object>` | All extracted column values |

#### `ScrapeTestUrlResponse`
| Field | Type | Description |
|---|---|---|
| `requestedUrl` | `String` | Original submitted URL |
| `finalUrl` | `String` | URL after navigation and redirects |
| `title` | `String` | Page `<title>` content |
| `htmlLength` | `Integer` | Rendered HTML character count |
| `captchaDetected` | `Boolean` | CAPTCHA/anti-bot signal found |
| `rawHtmlSaved` | `Boolean` | HTML saved to disk |
| `screenshotSaved` | `Boolean` | Screenshot saved to disk |
| `rawHtmlPath` | `String` | Absolute path of saved HTML file |
| `screenshotPath` | `String` | Absolute path of saved PNG |
| `extractedPreview` | `List<ScrapeExtractedPreviewDto>` | Up to 10 parsed rows |
| `warnings` | `List<String>` | Non-fatal warnings (null if none) |
| `fetchedAt` | `OffsetDateTime` | Navigation start timestamp |

### Provider Layer

#### `ScrapingProvider` (Interface)
```java
ScrapeProviderResult fetchRenderedPage(ScrapeProviderRequest request);
```
Extension point for switching from local Playwright to Apify/Zyte/BrightData.

#### `ScrapeProviderRequest` (Record)
| Field | Type | Value in S1 |
|---|---|---|
| `url` | `String` | Submitted URL |
| `timeoutMs` | `int` | `60_000` |
| `fullPageScreenshot` | `boolean` | `true` |

#### `ScrapeProviderResult` (Lombok `@Builder`)
| Field | Type | Description |
|---|---|---|
| `requestedUrl` | `String` | Input URL |
| `finalUrl` | `String` | Post-navigation URL |
| `title` | `String` | Page title |
| `html` | `String` | Full rendered HTML from `page.content()` |
| `screenshotBytes` | `byte[]` | PNG bytes |
| `fetchedAt` | `OffsetDateTime` | Navigation start |
| `warnings` | `List<String>` | Provider-level warnings |

#### `PlaywrightScrapingProvider`
- Browser: Chromium headless, viewport 1440×1200, Chrome 124 user-agent
- Navigate with `DOMCONTENTLOADED`, then attempt `NETWORKIDLE` (15 s, non-fatal timeout)
- One browser process per request (POC — no singleton pool)
- `TimeoutError` → `IllegalStateException("Scrape navigation timed out")`
- Launch failure → `IllegalStateException` with Chromium install command

### Parser

#### `ReraPreviewParser`
Generic Jsoup table extractor. See [Section 11](#11-s1-html-parsing-logic) for full algorithm.

### Utilities

#### `ScrapeCaptchaDetector`
Case-insensitive scan against 12 signals. See [Section 9](#9-captcha-detection).

#### `ScrapeFileStorageService` / `LocalScrapeFileStorageService`
Interface + `/tmp/sfs-scrapes` implementation. See [Section 10](#10-file-storage).

---

## 5. S1 API Reference

### `POST /api/dashboard/scraping/test-url`

| Property | Value |
|---|---|
| **Auth** | Dashboard JWT — `Authorization: Bearer <token>` |
| **Role** | `ADMIN` only |
| **Content-Type** | `application/json` |
| **Typical response time** | 5–30 seconds |

---

## 6. S1 Request & Response Schemas

### Request
```json
{
  "url": "https://example.com"
}
```

### Success (HTTP 200)
```json
{
  "requestedUrl": "https://example.com",
  "finalUrl": "https://example.com",
  "title": "Example Domain",
  "htmlLength": 1256,
  "captchaDetected": false,
  "rawHtmlSaved": true,
  "screenshotSaved": true,
  "rawHtmlPath": "/tmp/sfs-scrapes/scrape-test-20260508143022-a1b2c3d4.html",
  "screenshotPath": "/tmp/sfs-scrapes/scrape-test-20260508143022-a1b2c3d4.png",
  "extractedPreview": [
    {
      "sourceUrl": "https://example.com",
      "raw": { "textPreview": "Example Domain This domain is for use in..." }
    }
  ],
  "fetchedAt": "2026-05-08T14:30:22Z"
}
```

### CAPTCHA Detected (HTTP 200 — informational)
```json
{
  "requestedUrl": "https://portal.gov.in",
  "captchaDetected": true,
  "rawHtmlSaved": true,
  "screenshotSaved": true,
  "extractedPreview": [{ "raw": { "textPreview": "Just a moment..." } }],
  "warnings": ["CAPTCHA or anti-bot signal detected in rendered HTML."]
}
```

### Validation Error (HTTP 400)
```json
{
  "success": false,
  "status": 400,
  "code": "DASHBOARD_VALIDATION_FAILED",
  "fieldErrors": [{ "field": "url", "message": "URL must start with http:// or https://" }]
}
```

### Navigation Timeout (HTTP 400)
```json
{
  "success": false,
  "status": 400,
  "code": "DASHBOARD_INVALID_WORKFLOW",
  "message": "Scrape navigation timed out for URL: https://slow-site.gov.in"
}
```

---

## 7. S1 Data Flow

```
Client
    │  POST /api/dashboard/scraping/test-url
    ▼
DashboardJwtAuthenticationFilter   (validates dashboard JWT)
    ▼
DashboardScrapingTestController    (@Valid, @PreAuthorize ADMIN)
    ▼
DashboardScrapingTestServiceImpl
    ├─► validateUrlSafety()
    ├─► ScrapingProvider.fetchRenderedPage()
    │       Playwright → Chromium → navigate → NETWORKIDLE → content/screenshot
    ├─► ScrapeFileStorageService.saveHtml()    → /tmp/sfs-scrapes/{prefix}.html
    ├─► ScrapeFileStorageService.saveScreenshot() → /tmp/sfs-scrapes/{prefix}.png
    ├─► ScrapeCaptchaDetector.detect(html)
    ├─► ReraPreviewParser.parse(html, finalUrl)
    └─► Build and return ScrapeTestUrlResponse
    ▼
Client receives JSON
```

---

## 8. SSRF & URL Safety

Applied in `DashboardScrapingTestServiceImpl.validateUrlSafety()`, two layers:

**Layer 1 — `@Pattern` on request DTO:** Blocks non-`http(s)` at validation time.

**Layer 2 — Four-step service check:**

| Step | What is checked |
|---|---|
| 1. URL parse | `java.net.URL` parse — `MalformedURLException` → 400 |
| 2. Protocol | Only `http` / `https` allowed |
| 3. Hostname strings | Blocks: `localhost`, `127.0.0.1`, `0.0.0.0`, `10.x`, `172.16-31.x`, `192.168.x`, `169.254.x` |
| 4. DNS SSRF guard | `InetAddress.getAllByName()` — blocks if resolves to loopback/site-local/link-local (prevents DNS rebinding attacks) |

---

## 9. CAPTCHA Detection

`ScrapeCaptchaDetector` performs a case-insensitive substring scan. Detection is **report-only** — no bypass, no proxy rotation, no stealth plugins.

| Signal | What it indicates |
|---|---|
| `captcha` | Generic CAPTCHA |
| `g-recaptcha` | Google reCAPTCHA |
| `hcaptcha` | hCaptcha |
| `verify you are human` | Interstitial challenge |
| `are you a robot` | Bot check |
| `cloudflare` | Cloudflare protection |
| `ddos-guard` | DDoS-Guard |
| `access denied` | Hard block |
| `blocked` | Site-level block |
| `bot detection` | Anti-bot middleware |
| `unusual traffic` | Rate limit (Google-style) |
| `security check` | Generic challenge |

---

## 10. File Storage

**Location:** `/tmp/sfs-scrapes/` — created by `@PostConstruct` on startup.

**File naming:**
```
scrape-test-{yyyyMMddHHmmss}-{8-char-uuid}.html
scrape-test-{yyyyMMddHHmmss}-{8-char-uuid}.png
rera-{sourcecode}-{yyyyMMddHHmmss}-{8-char-uuid}.html    ← S1B files
rera-{sourcecode}-{yyyyMMddHHmmss}-{8-char-uuid}.png
```

The timestamp + UUID form a **session prefix** shared between HTML and screenshot files, making them trivially correlated.

**On save failure:** Non-fatal. Failure is added to `warnings[]`; `rawHtmlSaved` / `screenshotSaved` is set to `false`; rest of response is still returned.

**Interface:**
```java
public interface ScrapeFileStorageService {
    SavedScrapeFile saveHtml(String filePrefix, String html);
    SavedScrapeFile saveScreenshot(String filePrefix, byte[] bytes);
}
```
`LocalScrapeFileStorageService` is the Phase S1/S1B implementation. Future: `S3ScrapeFileStorageService`.

---

## 11. S1 HTML Parsing Logic

`ReraPreviewParser` uses Jsoup. Strategy is generic and portal-agnostic.

**Algorithm:**
```
For each <table> in document (stop at 10 total records):

  Row 0 — header detection:
    If <th> elements → use as headers
    Else → use first <td> row as headers
  
  Normalize headers:
    trim → lowercase → remove non-alphanumeric
    "RERA Registration No." → "reraregistrationno"
  
  Row 1..N — data rows:
    Map normalized_header → cell.text()
    Capture first <a href> → resolve absolute via absUrl("href")
    Infer semantic fields from keyword hints (see table below)
    Skip fully blank rows

If no tables found → return one fallback record: raw.textPreview = first 1000 chars of body text
```

**Field inference keyword table:**

| Output Field | Keywords Matched |
|---|---|
| `projectName` | `project`, `projectname`, `name` |
| `builderName` | `builder`, `promoter`, `developer`, `company` |
| `reraNumber` | `rera`, `registration`, `regno`, `registrationno` |
| `cityName` | `city`, `district`, `location` |
| `statusText` | `status`, `projectstatus` |

---

## 12. Provider Abstraction

`ScrapingProvider` is the extension point for upgrading from local Playwright:

```
ScrapingProvider (interface)
    └── PlaywrightScrapingProvider (@Component)  ← S1 / S1B active
    └── ApifyScrapingProvider (future S5)
    └── ZyteScrapingProvider (future S5)
```

To add a paid provider: implement `ScrapingProvider`, annotate `@Component @Primary`, no other changes needed.

| Provider | Cost | JS Rendering | Proxy Rotation | CAPTCHA Solving |
|---|---|---|---|---|
| `PlaywrightScrapingProvider` | Free | Yes (local Chromium) | No | No |
| `ApifyScrapingProvider` (future) | Paid | Yes | Yes | Optional |
| `ZyteScrapingProvider` (future) | Paid | Yes | Yes | Yes |
| `BrightDataProvider` (future) | Paid | Yes | Yes (residential) | Optional |

---

# PHASE S1B — RERA NUMBER SEARCH SCRAPER

---

## 13. S1B Overview

Phase S1B introduces a **RERA number-based structured search endpoint**. The user submits a RERA registration number and a source code (e.g. `HARYANA_RERA`). The backend:

1. Opens the appropriate RERA authority's portal using Playwright
2. Fills and submits the search form with the RERA number
3. Navigates to the project detail page
4. Saves raw HTML and screenshot as evidence
5. Extracts all visible key-value pairs using three parsing strategies
6. Maps extracted values to typed candidate DTOs aligned with existing dashboard forms
7. Returns a field-by-field found/missing summary with confidence scores

**This phase does not write to any production table.** It produces candidate data for human review before any import.

### What Phase S1B adds

| Capability | S1B |
|---|---|
| Search by RERA number (not arbitrary URL) | Yes |
| Form interaction (district select + JS submit) via Playwright | Yes |
| Source-specific scraper per RERA authority | Yes (Haryana RERA + UP RERA — **both active and live**) |
| DataTable pagination filter before row scan | Yes |
| Structured project/builder candidate extraction | Yes |
| Field-level confidence scoring | Yes |
| Compliance candidate auto-generated | Yes (RERA_REGISTRATION always produced) |
| Multi-strategy HTML key-value extraction | Yes (5 strategies) |
| Aligned with all existing dashboard DTOs/enums | Yes |
| Writes to project/builder/project_meter tables | No — candidates go through S2 review workflow |

---

## 14. S1B Architecture

```
DashboardReraSearchController
    └── ReraNumberSearchServiceImpl (orchestrator)
            ├── List<ReraSourceScraper>         ← finds matching scraper by sourceCode
            │       ├── HaryanaReraSourceScraper
            │       │       ├── Playwright (browser interaction)
            │       │       ├── ReraDetailKeyValueParser (HTML extraction)
            │       │       └── ScrapeCaptchaDetector (blocking check)
            │       └── UpReraSourceScraper
            │               ├── Playwright (browser interaction)
            │               ├── ReraDetailKeyValueParser (HTML extraction)
            │               └── ScrapeCaptchaDetector (blocking check)
            ├── ScrapeFileStorageService         ← reused from S1
            │       └── LocalScrapeFileStorageService
            └── ReraProjectCandidateMapper
                    ├── Produces ScrapedProjectCandidateDto
                    ├── Produces ScrapedBuilderCandidateDto
                    ├── Produces List<ScrapedComplianceCandidateDto>
                    ├── Produces List<ScrapeFieldResultDto>
                    ├── Produces List<ScrapeMissingFieldDto>
                    └── Produces ReraSearchSummaryDto
```

**Key design decisions:**
- `ReraSourceScraper` is a strategy interface — one implementation per RERA authority, discovered by `supports(sourceCode)`.
- The scraper returns `ReraRawSearchResult` (generic, source-agnostic). The mapper is entirely source-independent.
- `ScrapeFileStorageService` is reused from S1 — no duplication of storage logic.
- `ScrapeCaptchaDetector` is reused from S1 — no duplication of detection logic.
- `PlaywrightScrapingProvider` (S1) is **not** reused in S1B — the source scraper uses Playwright directly because form interaction (fill + click + wait) cannot be handled by the S1 URL-only navigation provider.

---

## 15. S1B Package Structure

```
src/main/java/com/brandPitara/sfs/dashboard/scraping/
│
├── enums/                                          ← NEW in S1B
│   ├── ReraSourceCode.java
│   ├── ScrapeFieldSection.java
│   └── ScrapeFieldConfidenceStatus.java
│
├── dto/                                            ← S1B additions
│   ├── ReraNumberSearchRequest.java
│   ├── ReraNumberSearchResponse.java
│   ├── ReraSearchSummaryDto.java
│   ├── ScrapedProjectCandidateDto.java
│   ├── ScrapedBuilderCandidateDto.java
│   ├── ScrapedComplianceCandidateDto.java
│   ├── ScrapeFieldResultDto.java
│   └── ScrapeMissingFieldDto.java
│
├── source/                                         ← NEW in S1B
│   ├── ReraSourceScraper.java           ← interface
│   ├── ReraRawSearchResult.java
│   ├── HaryanaReraSourceScraper.java    ← ACTIVE
│   └── UpReraSourceScraper.java         ← ACTIVE
│
├── parser/
│   ├── ReraPreviewParser.java           ← S1
│   └── ReraDetailKeyValueParser.java    ← NEW in S1B (5 strategies)
│
├── mapper/                                         ← NEW in S1B
│   └── ReraProjectCandidateMapper.java
│
├── candidate/                                      ← NEW in S2
│   ├── controller/
│   │   └── ScrapeCandidateController.java
│   ├── dto/
│   │   ├── SaveScrapeCandidateRequest.java
│   │   ├── ScrapeCandidateSummaryDto.java
│   │   ├── ScrapeCandidateDetailResponse.java
│   │   ├── ScrapeCandidateProjectDto.java
│   │   ├── ScrapeCandidateBuilderDto.java
│   │   ├── ScrapeCandidateComplianceItemDto.java
│   │   ├── ScrapeCandidateFieldResultDto.java
│   │   ├── ScrapeCandidateRawValueDto.java
│   │   ├── ScrapeCandidateCostBreakdownDto.java
│   │   ├── ScrapeCandidateLandUtilizationDto.java
│   │   ├── ScrapeCandidateDocumentDto.java
│   │   ├── LinkBuilderRequest.java
│   │   ├── UpdateCandidateStatusRequest.java
│   │   ├── ApplyToProjectRequest.java
│   │   └── ApplyToProjectResponse.java
│   ├── entity/
│   │   ├── DashboardScrapeCandidateEntity.java
│   │   ├── DashboardScrapeCandidateProjectEntity.java
│   │   ├── DashboardScrapeCandidateBuilderEntity.java
│   │   ├── DashboardScrapeCandidateComplianceItemEntity.java
│   │   ├── DashboardScrapeCandidateFieldResultEntity.java
│   │   ├── DashboardScrapeCandidateRawValueEntity.java
│   │   ├── DashboardScrapeCandidateCostBreakdownEntity.java
│   │   ├── DashboardScrapeCandidateLandUtilizationEntity.java
│   │   └── DashboardScrapeCandidateDocumentEntity.java
│   ├── enums/
│   │   ├── ScrapeCandidateStatus.java
│   │   ├── ScrapeCandidateDocumentType.java
│   │   └── ApplyMode.java
│   ├── mapper/
│   │   └── ScrapeCandidateEntityMapper.java
│   ├── repository/
│   │   ├── ScrapeCandidateRepository.java
│   │   ├── ScrapeCandidateProjectRepository.java
│   │   ├── ScrapeCandidateBuilderRepository.java
│   │   ├── ScrapeCandidateComplianceItemRepository.java
│   │   ├── ScrapeCandidateFieldResultRepository.java
│   │   ├── ScrapeCandidateRawValueRepository.java
│   │   ├── ScrapeCandidateCostBreakdownRepository.java
│   │   ├── ScrapeCandidateLandUtilizationRepository.java
│   │   └── ScrapeCandidateDocumentRepository.java
│   └── service/
│       ├── ScrapeCandidateService.java
│       ├── ScrapeCandidateApplyService.java
│       ├── ScrapeCandidateValidationService.java
│       └── impl/
│           ├── ScrapeCandidateServiceImpl.java
│           ├── ScrapeCandidateApplyServiceImpl.java
│           └── ScrapeCandidateValidationServiceImpl.java
│
├── service/
│   ├── DashboardScrapingTestService.java  ← S1
│   ├── ReraNumberSearchService.java       ← NEW
│   └── impl/
│       ├── DashboardScrapingTestServiceImpl.java  ← S1
│       └── ReraNumberSearchServiceImpl.java       ← NEW
│
├── controller/
│   ├── DashboardScrapingTestController.java  ← S1
│   └── DashboardReraSearchController.java    ← NEW
│
├── provider/                              ← S1 (unchanged)
│   ├── ScrapingProvider.java
│   ├── ScrapeProviderRequest.java
│   ├── ScrapeProviderResult.java
│   └── PlaywrightScrapingProvider.java
│
└── util/                                  ← S1 (reused in S1B)
    ├── ScrapeCaptchaDetector.java
    ├── ScrapeFileStorageService.java
    ├── LocalScrapeFileStorageService.java
    └── SavedScrapeFile.java
```

---

## 16. S1B File Reference

### Enums

#### `ReraSourceCode`
```java
HARYANA_RERA   // Active — district-based search, DataTable filter, JS form submit
MAHA_RERA      // Returns "Source not implemented yet"
UP_RERA        // Active — direct registration-number search, 3-strategy table filter, Angular SPA row-click fallback
KARNATAKA_RERA // Returns "Source not implemented yet"
OTHER          // Returns "Source not implemented yet"
```

#### `ScrapeFieldSection`
Maps to dashboard form sections:

| Enum Value | Dashboard Area |
|---|---|
| `PROJECT_BASIC` | Project create/edit form |
| `BUILDER` | Builder create/edit form |
| `PROJECT_MEDIA` | Project media uploads |
| `PROJECT_FLOOR_PLAN` | Floor plan management |
| `PROJECT_CONNECTIVITY` | Connectivity & places |
| `PROJECT_METER_CONSTRUCTION_STAGE` | Construction stages form |
| `PROJECT_METER_COMPLIANCE` | Compliance items form |
| `PROJECT_METER_AMENITY` | Amenities form |
| `PROJECT_METER_PRICE_HISTORY` | Price history chart |
| `PROJECT_METER_PAYMENT_MILESTONE` | Payment milestone form |
| `PROJECT_METER_COST_BREAKDOWN` | Cost breakdown form |
| `PROJECT_METER_LAND_UTILIZATION` | Land utilization form |
| `PROJECT_METER_LOCATION_SCORE` | Location score form |

#### `ScrapeFieldConfidenceStatus`
| Value | Condition |
|---|---|
| `COMPLETE` | `confidenceScore >= 85` |
| `PARTIAL` | `50 <= confidenceScore < 85` |
| `LOW_CONFIDENCE` | `1 <= confidenceScore < 50` |
| `NOT_FOUND` | `found = false` OR `foundFields = 0` |
| `BLOCKED` | `captchaDetected = true` |

### DTOs

#### `ReraNumberSearchRequest`
| Field | Type | Validation |
|---|---|---|
| `sourceCode` | `ReraSourceCode` | `@NotNull` |
| `reraNumber` | `String` | `@NotBlank`, `@Size(max=100)` |
| `saveEvidence` | `boolean` | Default `true` |
| `includeRaw` | `boolean` | Default `true` |

#### `ReraNumberSearchResponse`
| Field | Type | Description |
|---|---|---|
| `sourceCode` | `ReraSourceCode` | Echo of request |
| `reraNumber` | `String` | Echo of request |
| `found` | `boolean` | Whether data was extracted |
| `captchaDetected` | `boolean` | Blocking signal found |
| `requestedAt` | `OffsetDateTime` | Browser navigation start time |
| `sourceSearchUrl` | `String` | RERA portal search page URL |
| `sourceDetailUrl` | `String` | Project detail page URL (if navigated) |
| `finalUrl` | `String` | Browser final URL |
| `title` | `String` | Page title |
| `rawHtmlPath` | `String` | Saved HTML absolute path |
| `screenshotPath` | `String` | Saved PNG absolute path |
| `summary` | `ReraSearchSummaryDto` | Confidence summary |
| `projectCandidate` | `ScrapedProjectCandidateDto` | Project data candidate |
| `builderCandidate` | `ScrapedBuilderCandidateDto` | Builder data candidate |
| `complianceCandidates` | `List<ScrapedComplianceCandidateDto>` | Compliance items found |
| `fieldResults` | `List<ScrapeFieldResultDto>` | Per-field found/missing detail |
| `missingFields` | `List<ScrapeMissingFieldDto>` | Only the missing fields |
| `warnings` | `List<String>` | Non-fatal warnings (null if none) |
| `raw` | `Map<String, Object>` | `allExtractedKeyValues` map (null if `includeRaw=false`) |

#### `ReraSearchSummaryDto`
| Field | Type | Description |
|---|---|---|
| `totalExpectedFields` | `int` | Fixed: 18 (defined in mapper) |
| `foundFields` | `int` | Fields where `found = true` |
| `missingFields` | `int` | `totalExpectedFields - foundFields` |
| `highConfidenceFields` | `int` | Found fields with `confidence >= 80` |
| `lowConfidenceFields` | `int` | Found fields with `confidence < 80` |
| `confidenceScore` | `int` | `round(foundFields * 100.0 / totalExpectedFields)` |
| `status` | `ScrapeFieldConfidenceStatus` | Overall scrape quality |

#### `ScrapedProjectCandidateDto`
Aligned with `ProjectUpsertRequest`. Uses `cityName` (String) instead of `cityId` (Long) since the numeric ID is resolved during the import review step.

| Field | Type | Source |
|---|---|---|
| `name` | `String` | Scraped from RERA page |
| `description` | `String` | Scraped (rarely available) |
| `cityName` | `String` | Scraped from district/city field |
| `addressLine` | `String` | Scraped from address field |
| `latitude` | `Double` | Scraped if available (rarely) |
| `longitude` | `Double` | Scraped if available (rarely) |
| `priceMin` | `Long` | Scraped if available (rarely) |
| `priceMax` | `Long` | Scraped if available (rarely) |
| `possessionDate` | `LocalDate` | Parsed from completion date field |
| `reraNumber` | `String` | Always set from request |
| `status` | `ProjectStatus` | Normalized from RERA status text |
| `propertyTypes` | `Set<PropertyType>` | Normalized from project type text |

#### `ScrapedBuilderCandidateDto`
Aligned with `BuilderUpsertRequest`. Uses `cityName` instead of `cityId`.

| Field | Type | Source |
|---|---|---|
| `name` | `String` | Scraped from promoter/builder field |
| `description` | `String` | Not typically on RERA portals |
| `phone` | `String` | Scraped from contact number |
| `whatsapp` | `String` | Not typically on RERA portals |
| `email` | `String` | Scraped from email field |
| `addressLine` | `String` | Scraped from promoter address |
| `cityName` | `String` | Scraped from promoter city |
| `latitude` | `Double` | Not typically on RERA portals |
| `longitude` | `Double` | Not typically on RERA portals |
| `logoUrl` | `String` | Not on RERA portals |

#### `ScrapedComplianceCandidateDto`
Aligned with `DashboardProjectComplianceItemRequest` and existing enums:
- `ProjectComplianceGroup`: `RERA`, `ENVIRONMENTAL`, `FIRE_SAFETY`, `STRUCTURAL`, `LEGAL_TITLE`, `UTILITY`, `OCCUPANCY`, `OTHER`, `LAND_LICENSE`, `APPROVAL_NOC`
- `ProjectComplianceStatus`: `OBTAINED`, `PENDING`, `NOT_APPLICABLE`, `EXPIRED`

**Always auto-produced:**
```json
{
  "itemGroup": "RERA",
  "itemKey": "RERA_REGISTRATION",
  "itemLabel": "RERA Registration",
  "status": "OBTAINED",
  "valueText": "<reraNumber from request>",
  "documentUrl": "<sourceDetailUrl if available>",
  "remarks": "Extracted from RERA portal",
  "displayOrder": 1,
  "verified": false
}
```

#### `ScrapeFieldResultDto`
| Field | Type | Description |
|---|---|---|
| `section` | `ScrapeFieldSection` | Which dashboard form area |
| `fieldKey` | `String` | Field identifier (matches candidate DTO field name) |
| `fieldLabel` | `String` | Human-readable label |
| `found` | `boolean` | Whether value was extracted |
| `value` | `Object` | Normalized value (typed: LocalDate, ProjectStatus, Set, Long, Double, or String) |
| `sourceLabel` | `String` | Where found (e.g. "RERA detail page") |
| `confidence` | `Integer` | 0–100 |
| `reason` | `String` | Why missing (null if found) |

#### `ScrapeMissingFieldDto`
Subset of `ScrapeFieldResultDto` for fields where `found = false`.

### Source Layer

#### `ReraSourceScraper` (Interface)
```java
public interface ReraSourceScraper {
    boolean supports(ReraSourceCode sourceCode);
    ReraRawSearchResult searchByReraNumber(String reraNumber);
}
```

#### `ReraRawSearchResult` (Lombok `@Builder`)
Intermediate result between scraper and mapper. Source-agnostic.

| Field | Type | Description |
|---|---|---|
| `found` | `boolean` | Extraction yielded data |
| `captchaDetected` | `boolean` | Blocking signal in HTML |
| `sourceSearchUrl` | `String` | Search page URL |
| `sourceDetailUrl` | `String` | Detail page URL (null if not navigated) |
| `finalUrl` | `String` | Browser final URL |
| `title` | `String` | Page title |
| `html` | `String` | Full rendered HTML |
| `screenshotBytes` | `byte[]` | Full-page PNG |
| `extractedKeyValues` | `Map<String, String>` | Normalized key → raw string value |
| `fetchedAt` | `OffsetDateTime` | Navigation start |
| `warnings` | `List<String>` | Mutable — service appends file-save warnings |

### Service

#### `ReraNumberSearchServiceImpl`
**Orchestration sequence:**
1. Find matching `ReraSourceScraper` by `sourceCode` → 400 if not found
2. Call `scraper.searchByReraNumber(reraNumber)` → `ReraRawSearchResult`
3. If `saveEvidence=true`: save HTML and screenshot via `ScrapeFileStorageService`
4. Call `candidateMapper.map(rawResult, reraNumber, sourceCode, includeRaw)`
5. Set `rawHtmlPath` / `screenshotPath` on response
6. Return response

File prefix format: `rera-{sourcecode_lowercase}-{yyyyMMddHHmmss}-{shortUuid}`

---

## 17. S1B API Reference

### `POST /api/dashboard/scraping/rera/search-by-number`

| Property | Value |
|---|---|
| **Auth** | Dashboard JWT — `Authorization: Bearer <token>` |
| **Role** | `ADMIN` only |
| **Content-Type** | `application/json` |
| **Typical response time** | 10–60 seconds (browser launch + form interaction + navigation) |

---

## 18. S1B Request & Response Schemas

### Request — Haryana RERA
```json
{
  "sourceCode": "HARYANA_RERA",
  "reraNumber": "GGM/872/604/2024/99",
  "saveEvidence": true,
  "includeRaw": true
}
```

### Request — UP RERA
```json
{
  "sourceCode": "UP_RERA",
  "reraNumber": "UPRERAPRJ12345",
  "saveEvidence": true,
  "includeRaw": true
}
```

### Full Success (HTTP 200)
```json
{
  "sourceCode": "HARYANA_RERA",
  "reraNumber": "GGM/872/604/2024/99",
  "found": true,
  "captchaDetected": false,
  "requestedAt": "2026-05-09T14:30:22Z",
  "sourceSearchUrl": "https://haryanarera.gov.in/assistancecontrol/project_search_public/2",
  "sourceDetailUrl": "https://haryanarera.gov.in/assistancecontrol/project_detail_public/GGM-872-604-2024-99",
  "finalUrl": "https://haryanarera.gov.in/assistancecontrol/project_detail_public/GGM-872-604-2024-99",
  "title": "Project Detail — Haryana RERA",
  "rawHtmlPath": "/tmp/sfs-scrapes/rera-haryana_rera-20260508143022-a1b2c3d4.html",
  "screenshotPath": "/tmp/sfs-scrapes/rera-haryana_rera-20260508143022-a1b2c3d4.png",
  "summary": {
    "totalExpectedFields": 18,
    "foundFields": 11,
    "missingFields": 7,
    "highConfidenceFields": 8,
    "lowConfidenceFields": 3,
    "confidenceScore": 61,
    "status": "PARTIAL"
  },
  "projectCandidate": {
    "name": "Sunrise Heights Phase 2",
    "cityName": "Gurugram",
    "addressLine": "Sector 72A, Gurugram, Haryana",
    "possessionDate": "2026-12-31",
    "reraNumber": "GGM/872/604/2024/99",
    "status": "UNDER_CONSTRUCTION",
    "propertyTypes": ["APARTMENT"]
  },
  "builderCandidate": {
    "name": "ABC Realty Pvt Ltd",
    "phone": "9876543210",
    "email": "contact@abcrealty.com"
  },
  "complianceCandidates": [
    {
      "itemGroup": "RERA",
      "itemKey": "RERA_REGISTRATION",
      "itemLabel": "RERA Registration",
      "status": "OBTAINED",
      "valueText": "GGM/872/604/2024/99",
      "documentUrl": "https://hrera.gov.in/project/GGM-872-604-2024-99",
      "remarks": "Extracted from RERA portal",
      "displayOrder": 1,
      "verified": false
    }
  ],
  "fieldResults": [
    {
      "section": "PROJECT_BASIC",
      "fieldKey": "name",
      "fieldLabel": "Project Name",
      "found": true,
      "value": "Sunrise Heights Phase 2",
      "sourceLabel": "RERA detail page",
      "confidence": 90
    },
    {
      "section": "PROJECT_BASIC",
      "fieldKey": "priceMin",
      "fieldLabel": "Minimum Price",
      "found": false,
      "confidence": 0,
      "reason": "Not found on RERA page"
    }
  ],
  "missingFields": [
    {
      "section": "PROJECT_BASIC",
      "fieldKey": "priceMin",
      "fieldLabel": "Minimum Price",
      "reason": "Not found on RERA page"
    }
  ],
  "raw": {
    "allExtractedKeyValues": {
      "projectname": "Sunrise Heights Phase 2",
      "promotername": "ABC Realty Pvt Ltd",
      "district": "Gurugram",
      "proposedcompletiondate": "31/12/2026",
      "projectstatus": "Under Construction"
    }
  }
}
```

### Source Not Implemented (HTTP 400)
```json
{
  "success": false,
  "status": 400,
  "code": "DASHBOARD_INVALID_WORKFLOW",
  "message": "Source not implemented yet: MAHA_RERA"
}
```

### CAPTCHA on RERA Portal (HTTP 200 — informational)
```json
{
  "sourceCode": "HARYANA_RERA",
  "reraNumber": "GGM/872/604/2024/99",
  "found": false,
  "captchaDetected": true,
  "rawHtmlPath": "/tmp/sfs-scrapes/rera-haryana_rera-20260508143022-a1b2c3d4.html",
  "screenshotPath": "/tmp/sfs-scrapes/rera-haryana_rera-20260508143022-a1b2c3d4.png",
  "summary": {
    "totalExpectedFields": 18,
    "foundFields": 0,
    "missingFields": 18,
    "confidenceScore": 0,
    "status": "BLOCKED"
  },
  "warnings": ["CAPTCHA or anti-bot challenge detected. Scrape aborted."]
}
```

---

## 19. S1B Data Flow

The flow below uses Haryana RERA as the concrete example. UP RERA follows the same top-level path; the only difference is which scraper is selected and what happens inside it (see [Section 25.5](#255-uprerasourcescraper--active-implementation) for the UP RERA-specific interaction sequence).

```
Client
    │  POST /api/dashboard/scraping/rera/search-by-number
    │  { "sourceCode": "HARYANA_RERA", "reraNumber": "GGM/872/604/2024/99" }
    │  — or —
    │  { "sourceCode": "UP_RERA",      "reraNumber": "UPRERAPRJ12345" }
    ▼
DashboardJwtAuthenticationFilter   (validates dashboard JWT)
    ▼
DashboardReraSearchController      (@Valid, @PreAuthorize ADMIN)
    ▼
ReraNumberSearchServiceImpl
    │
    ├─► Find ReraSourceScraper supporting HARYANA_RERA  → HaryanaReraSourceScraper
    │   Find ReraSourceScraper supporting UP_RERA       → UpReraSourceScraper
    │
    │   [Haryana RERA path]
    ├─► HaryanaReraSourceScraper.searchByReraNumber("GGM/872/604/2024/99")
    │               │
    │               ├── Playwright.create() + chromium.launch(headless)
    │               ├── page.navigate(SEARCH_URL, DOMCONTENTLOADED)
    │               ├── captchaDetector.detect(searchPageHtml)  → abort if blocked
    │               │
    │               ├── Resolve district code from RERA prefix
    │               │       "GGM" → "62" (Gurugram)   "PKL" → "70" (Panchkula)
    │               │       unknown prefix → "999" (All districts)
    │               │
    │               ├── submitDistrictSearch(page, districtCode)
    │               │       page.evaluate() — JS injection:
    │               │         select.value = districtCode
    │               │         dispatch "change" + jQuery trigger
    │               │         inject hidden input[name=basic_search]
    │               │         form.submit()
    │               │       page.waitForLoadState(DOMCONTENTLOADED, 30s)
    │               │
    │               ├── filterResultTableByReraNumber(page, reraNumber)
    │               │       Try: jQuery('#compliant_hearing').DataTable().search(n).draw()
    │               │       Fallback: dispatch input + keyup on #compliant_hearing_filter input
    │               │       page.waitForTimeout(1500ms)
    │               │
    │               ├── logVisibleRowsAfterFilter(page, reraNumber)  ← debug logging
    │               │
    │               ├── Scan "#compliant_hearing tbody tr" rows:
    │               │       Normalize cell text + reraNumber (uppercase, strip non-alnum/slash)
    │               │       Match: regNum.includes(target) || target.includes(regNum)
    │               │       JS_FIND_DETAIL_URL → extract href from matching row
    │               │       JS_FIND_FORM_AH_URL → extract Form A-H link from matching row
    │               │
    │               ├── Navigate to detail page (same-tab navigate)
    │               ├── waitForLoadState(DOMCONTENTLOADED)
    │               ├── captchaDetector.detect(detailHtml)      → abort if blocked
    │               ├── JS_FIND_FORM_AH_URL on detail page      → save form URL
    │               ├── page.content() + page.screenshot(fullPage)
    │               ├── keyValueParser.parse(html, finalUrl)    → Map<String,String>
    │               └── return ReraRawSearchResult
    │
    │   [UP RERA path — runs instead of Haryana when sourceCode=UP_RERA]
    ├─► UpReraSourceScraper.searchByReraNumber("UPRERAPRJ12345")
    │               │
    │               ├── Playwright.create() + chromium.launch(headless)
    │               ├── page.navigate(https://www.up-rera.in/Prodetails, DOMCONTENTLOADED)
    │               ├── page.waitForTimeout(3000ms)           ← Angular SPA settle
    │               ├── page.waitForLoadState(NETWORKIDLE, 20s)
    │               ├── captchaDetector.detect(html)          → abort if blocked
    │               │
    │               ├── filterTableByReraNumber(page, reraNumber)
    │               │       Try 1: jQuery DataTables API  → dt.search(n).draw()
    │               │       Try 2: DataTables filter input → dispatch input/keyup/change
    │               │       Try 3: Angular/generic input  → dispatch input/keyup
    │               │       page.waitForTimeout(2000ms)
    │               │
    │               ├── waitForSelector("table tbody tr", 20s)
    │               │       → none found → return noResultFound()
    │               │
    │               ├── logVisibleRowsAfterFilter(page, reraNumber)  ← debug logging
    │               │
    │               ├── JS_FIND_DETAIL_URL(page, reraNumber)
    │               │       normalize+includes() scan across all td cells
    │               │       → null              → return noResultFound()
    │               │       → "__CLICK_ROW__"   → JS_CLICK_MATCHING_ROW + waitNetworkIdle
    │               │       → href string       → page.navigate(detailUrl)
    │               │
    │               ├── captchaDetector.detect(detailHtml)    → abort if blocked
    │               ├── page.content() + page.screenshot(fullPage)
    │               ├── keyValueParser.parse(html, finalUrl)  → Map<String,String>
    │               └── return ReraRawSearchResult
    │
    ├─► ScrapeFileStorageService.saveHtml()       → /tmp/sfs-scrapes/rera-{sourcecode}-{ts}-{id}.html
    ├─► ScrapeFileStorageService.saveScreenshot() → /tmp/sfs-scrapes/rera-{sourcecode}-{ts}-{id}.png
    │
    └─► ReraProjectCandidateMapper.map(rawResult, reraNumber, sourceCode, includeRaw)
            ├── For each of 18 expected fields:
            │       look up synonyms in extractedKeyValues
            │       if found → normalize value → ScrapeFieldResultDto(found=true)
            │       if not   → ScrapeFieldResultDto(found=false) + ScrapeMissingFieldDto
            ├── Build ScrapedProjectCandidateDto  (normalize date, status, propertyType, area)
            ├── Build ScrapedBuilderCandidateDto
            ├── Build ScrapedComplianceCandidateDto (RERA_REGISTRATION always present)
            ├── Build ReraSearchSummaryDto (counts + confidenceScore + status)
            └── return ReraNumberSearchResponse
    ▼
ReraNumberSearchServiceImpl sets rawHtmlPath + screenshotPath
    ▼
Client receives complete structured JSON
```

---

## 20. RERA Source Scraper Layer

### Interface

```java
public interface ReraSourceScraper {
    boolean supports(ReraSourceCode sourceCode);
    ReraRawSearchResult searchByReraNumber(String reraNumber);
}
```

Spring auto-discovers all `@Component` implementations. `ReraNumberSearchServiceImpl` iterates `List<ReraSourceScraper>` (auto-injected) and calls `supports()` to find the right one.

### Adding a new source (Phase S3+)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class MahaReraSourceScraper implements ReraSourceScraper {

    private static final String SEARCH_URL          = "https://maharera.mahaonline.gov.in/...";
    private static final String RERA_INPUT_SELECTOR = "input#reraNo";
    // ... other selectors
    private static final boolean SELECTORS_CONFIGURED = true;

    private final ReraDetailKeyValueParser keyValueParser;
    private final ScrapeCaptchaDetector captchaDetector;

    @Override
    public boolean supports(ReraSourceCode sourceCode) {
        return ReraSourceCode.MAHA_RERA == sourceCode;
    }

    @Override
    public ReraRawSearchResult searchByReraNumber(String reraNumber) {
        // Playwright interaction specific to MahaRERA portal
    }
}
```

No changes needed in controller, service, or mapper. Spring picks up the new `@Component` automatically.

### Scraper lifecycle — per implementation

#### `HaryanaReraSourceScraper`

```
searchByReraNumber(reraNumber)
    │
    └── scrapeWithPlaywright(reraNumber)
            │
            ├── launchBrowser()
            │       → PlaywrightException  → 400 with Chromium install command
            │
            ├── navigateTo(SEARCH_URL)
            │       → TimeoutError         → 400 "Timeout navigating to..."
            │
            ├── detect CAPTCHA on search page
            │       → captchaDetected=true → return captchaBlockedResult()
            │
            ├── resolveDistrictCode(reraNumber)
            │       → extract prefix (GGM/PKL/…), look up RERA_PREFIX_TO_DISTRICT
            │       → fallback "999" (All districts)
            │
            ├── submitDistrictSearch(page, districtCode)
            │       → page.evaluate() — JS sets select.value, dispatches events, form.submit()
            │       → page.waitForLoadState(DOMCONTENTLOADED, 30s)
            │
            ├── filterResultTableByReraNumber(page, reraNumber)
            │       → Try DataTables API, fallback to filter input events
            │       → page.waitForTimeout(1500ms)
            │
            ├── findDetailUrl(page, reraNumber)
            │       → JS_FIND_DETAIL_URL evaluates normalize()+includes() scan
            │       → returns null → return noResultFound()
            │
            ├── navigate to detailUrl
            │       → page.waitForLoadState(DOMCONTENTLOADED)
            │
            ├── detect CAPTCHA on detail page
            │       → captchaDetected=true → return captchaBlockedResult()
            │
            ├── captureScreenshot()
            │       → Exception → warning added, null bytes
            │
            └── keyValueParser.parse(html, finalUrl)
                    → return ReraRawSearchResult(found=true)
```

#### `UpReraSourceScraper`

```
searchByReraNumber(reraNumber)
    │
    └── scrapeWithPlaywright(reraNumber)
            │
            ├── launchBrowser()
            │       → PlaywrightException  → 400 with Chromium install command
            │
            ├── navigateTo(https://www.up-rera.in/Prodetails)
            │       → TimeoutError         → 400 "Timeout navigating to..."
            │
            ├── page.waitForTimeout(3000ms)  ← Angular SPA settle
            ├── page.waitForLoadState(NETWORKIDLE, 20s)
            │
            ├── detect CAPTCHA on search page
            │       → captchaDetected=true → return captchaBlockedResult()
            │
            ├── filterTableByReraNumber(page, reraNumber)
            │       → 3-strategy filter (DataTables API, filter input, Angular input)
            │       → page.waitForTimeout(2000ms)
            │
            ├── waitForSelector("table tbody tr", 20s)
            │       → not found → return noResultFound()
            │
            ├── JS_FIND_DETAIL_URL(page, reraNumber)
            │       → null              → return noResultFound()
            │       → "__CLICK_ROW__"  → JS_CLICK_MATCHING_ROW + waitNetworkIdle
            │       → href             → navigateTo(href)
            │
            ├── detect CAPTCHA on detail page
            │       → captchaDetected=true → return captchaBlockedResult()
            │
            ├── captureScreenshot()
            │       → Exception → warning added, null bytes
            │
            └── keyValueParser.parse(html, finalUrl)
                    → return ReraRawSearchResult(found=true)
```

### Key constants

```java
// HaryanaReraSourceScraper
private static final String SEARCH_URL           = "https://haryanarera.gov.in/assistancecontrol/project_search_public/2";
private static final String RESULTS_ROW_SELECTOR = "#compliant_hearing tbody tr";
private static final Map<String, String> RERA_PREFIX_TO_DISTRICT = Map.of("GGM", "62", "PKL", "70");

// UpReraSourceScraper
private static final String SEARCH_URL            = "https://www.up-rera.in/Prodetails";
private static final String RESULTS_ROW_SELECTOR  = "table tbody tr";
private static final int    ANGULAR_SETTLE_MS      = 3_000;
```

### Normalized row matching

Both the RERA number from the request and the cell content from each result row are normalized before comparison:

```javascript
const normalize = (value) => (value || "")
    .toUpperCase()
    .replace(/\s+/g, "")
    .replace(/[^A-Z0-9/]/g, "");
const target = normalize(reraNumber);
if (regNum.includes(target) || target.includes(regNum)) { ... }
```

This handles spacing, punctuation, and prefix-format variations between the submitted RERA number and what the portal displays in the table.

---

## 21. ReraDetailKeyValueParser — Extraction Strategies

Five strategies are applied in order. Earlier strategies win for duplicate keys.

### Strategy A — Two-Column Tables

For each `<table>`:
- For each `<tr>` with exactly **2 cells** (`<td>` or `<th>`):
  - `key = normalize(cell[0].text)`
  - `value = cell[1].text`

Handles typical RERA portal label-value table layouts:
```html
<tr><td>Project Name</td><td>Sunrise Heights</td></tr>
<tr><td>Promoter Name</td><td>ABC Realty</td></tr>
```

### Strategy B — Definition Lists

For each `<dl>`:
- `<dt>` element → key
- Following consecutive `<dd>` elements → value (joined with `, `)

Handles modern portal layouts using semantic HTML:
```html
<dl>
  <dt>Registration Number</dt>
  <dd>GGM/872/604/2024/99</dd>
</dl>
```

### Strategy C — Label-Colon Pattern

For each element whose `ownText()` ends with `:`:
- `key = normalize(text without trailing colon)`
- `value = nextElementSibling().text()`

Handles inline label-value layouts:
```html
<span>Project Status :</span><span>Under Construction</span>
```

### Strategy D — Horizontal Header Tables

For each `<table>` that has a `<thead>` with `<th>` cells:
- Headers come from all `<th>` elements in the first `<thead>` row
- Values come from the first `<tbody>` row `<td>` cells, zipped by column index

Handles HRERA Form A-H layout where column headers are labels and a single data row contains values:
```html
<thead><tr><th>Project Name</th><th>District</th><th>Promoter</th></tr></thead>
<tbody><tr><td>Sunrise Heights</td><td>Gurugram</td><td>ABC Realty</td></tr></tbody>
```

### Strategy E — Three-Column Label/Value Tables

For each `<tr>` with exactly **3 cells** (`<td>` or `<th>`):
- If col[0] is non-blank and col[1] is blank → `key = normalize(col[0])`, `value = col[2]`
- If col[0] is blank and col[1] is non-blank → `key = normalize(col[1])`, `value = col[2]`

Handles HRERA's 3-column tables where the label alternates between column 0 and column 1 with the value always in column 2:
```html
<tr><td>Project Name</td><td></td><td>Sunrise Heights</td></tr>
<tr><td></td><td>Promoter Name</td><td>ABC Realty</td></tr>
```

### Key normalization

All keys are normalized identically across all five strategies:
```
"RERA Registration No."  → "reraregistrationno"
"Promoter Name :"        → "promotername"
"Proposed Completion"    → "proposedcompletion"
```

Formula: `trim → toLowerCase → replaceAll("[^a-z0-9]", "")`

---

## 22. ReraProjectCandidateMapper — 18 Expected Fields

The mapper defines **18 expected fields** — the fixed denominator for confidence scoring. Each field has a list of normalized-key synonyms checked in order.

### Project Basic (12 fields)

| `fieldKey` | `fieldLabel` | Synonyms checked | Base confidence |
|---|---|---|---|
| `name` | Project Name | `projectname`, `nameoftheproject`, `projecttitle`, `schemename` | 90 |
| `cityName` | City / District | `city`, `district`, `districtname`, `taluka` | 80 |
| `addressLine` | Project Address | `address`, `projectaddress`, `siteaddress`, `projectlocation`, `siteoflocation` | 75 |
| `reraNumber` | RERA Registration Number | `reranumber`, `registrationno`, `reraregistrationno`, `projectregistrationno` | 100 |
| `status` | Project Status | `projectstatus`, `status`, `currentstatus`, `statusofproject` | 80 |
| `possessionDate` | Possession / Completion Date | `proposedcompletiondate`, `completiondate`, `possessiondate`, `projectcompletiondate`, `likelydateofcompletingtheproject`, `iilikelydateofcompletingtheproject`, `likelydateofcompletionoftheproject`, `initialdateofcompletionoftheproject`, `10initialdateofcompletionoftheproject`, `11likelydateofcompletionoftheproject` | 80 |
| `propertyTypes` | Property Type | `projecttype`, `propertytype`, `typeofproject`, `natureofproject`, `apartments`, `aapartments`, `itotalnumberofapartments`, `iitotalnumberofapartments`, `plots`, `bplots`, `iiitotalnumberofplots` | 75 |
| `description` | Project Description | `projectdescription`, `description`, `briefdescription` | 60 |
| `latitude` | Latitude | `latitude`, `lat` | 70 |
| `longitude` | Longitude | `longitude`, `lng` | 70 |
| `priceMin` | Minimum Price | `pricemin`, `minimumprice`, `startingprice`, `pricerangefrom` | 60 |
| `priceMax` | Maximum Price | `pricemax`, `maximumprice`, `pricerangeto` | 60 |

### Builder (5 fields)

| `fieldKey` | `fieldLabel` | Synonyms checked | Base confidence |
|---|---|---|---|
| `builderName` | Builder / Promoter Name | `promotername`, `buildername`, `developer`, `applicantname`, `nameofpromoter` | 90 |
| `builderPhone` | Builder Phone | `contactnumber`, `mobilenumber`, `phone`, `promotercontact` | 80 |
| `builderEmail` | Builder Email | `emailid`, `email`, `emailaddress`, `promoteremail` | 80 |
| `builderAddress` | Builder / Promoter Address | `promoteraddress`, `builderaddress`, `officeaddress`, `registeredaddress`, `annexacopyinfoldera`, `registeredaddressofthecompany`, `1nameandregisteredaddressofthecompany` | 70 |
| `builderCity` | Builder City | `promotercity`, `buildercity`, `promoterdistrict` | 60 |

### Project Meter (1 field)

| `fieldKey` | `fieldLabel` | Synonyms checked | Base confidence |
|---|---|---|---|
| `totalLandAreaSqm` | Total Land Area (sqm) | `totallandarea`, `landarea`, `totalareainsqm`, `plottedarea`, `totalplotarea`, `landareaoftheproject`, `1landareaoftheproject`, `itotalareaoftheproject`, `totallicensedarea`, `4totallicensedareaifthelandareaofthepresentprojectisapartthereof` | 75 |

### Value normalizers

| Field | Raw input | Normalized output |
|---|---|---|
| `possessionDate` | `"31/12/2026"`, `"31 Dec 2026"`, `"2026-12-31"` | `LocalDate` or raw String if unparseable |
| `status` | `"under construction"`, `"ongoing"` | `ProjectStatus.UNDER_CONSTRUCTION` |
| `status` | `"completed"`, `"ready to move"` | `ProjectStatus.READY_TO_MOVE` |
| `status` | `"upcoming"`, `"proposed"`, `"approved"` | `ProjectStatus.UPCOMING` |
| `propertyTypes` | `"apartment"`, `"group housing"` | `Set{APARTMENT}` |
| `propertyTypes` | `"plotted"`, `"plot"` | `Set{PLOT}` |
| `propertyTypes` | `"commercial"` | `Set{COMMERCIAL}` |
| `propertyTypes` | `"villa"` | `Set{VILLA}` |
| `latitude`, `longitude` | `"28.4595"` | `Double` |
| `priceMin`, `priceMax` | `"5000000"` | `Long` |
| Unknown | Any raw string | Kept as `String` in `value`, low/no additional penalty |

**Date formats tried (in order):**
`yyyy-MM-dd`, `dd/MM/yyyy`, `d/M/yyyy`, `dd-MM-yyyy`, `d-M-yyyy`, `dd MMM yyyy`, `d MMM yyyy`, `dd MMMM yyyy`, `MMMM dd, yyyy`, `MM/dd/yyyy`

**Date pre-processing:** Before attempting format matching, the raw value is cleaned:
- The suffix `(date)` and standalone `date` text are stripped (e.g. `"31/12/2026 (date)"` → `"31/12/2026"`)
- Leading/trailing whitespace is trimmed

**Land area unit conversion:** The `totalLandAreaSqm` field runs `parseAreaToSqm()` which detects the unit suffix in the raw value and converts automatically:

| Detected suffix | Conversion |
|---|---|
| `sqm`, `sq.m`, `sq m`, `sqmtr`, `squaremeter` | No conversion (already sqm) |
| `acre`, `acres` | × 4046.8564224 |
| `hectare`, `ha` | × 10,000 |
| `sqft`, `sq.ft`, `sqfeet` | × 0.0929 |
| (no unit or unknown) | Assumes sqm |

Result is rounded to two decimal places.

### RERA number injection

The RERA number from the search request is **always** injected into `fieldResults` with `confidence=100` and `sourceLabel="Search request (verified by RERA page)"`. If the RERA page itself also shows the number (found in extractedKeyValues), the page-found version replaces the injected one.

---

## 23. Dashboard Form Alignment

This section shows exactly which candidate fields map to which dashboard form fields.

### Project create/edit form (`ProjectUpsertRequest`)

| Dashboard field | Candidate field | Available from RERA |
|---|---|---|
| `name` | `projectCandidate.name` | Yes — high confidence |
| `description` | `projectCandidate.description` | Rarely |
| `cityId` | Resolved from `projectCandidate.cityName` | City lookup needed at import |
| `addressLine` | `projectCandidate.addressLine` | Yes |
| `latitude` | `projectCandidate.latitude` | Rarely |
| `longitude` | `projectCandidate.longitude` | Rarely |
| `priceMin` | `projectCandidate.priceMin` | Rarely |
| `priceMax` | `projectCandidate.priceMax` | Rarely |
| `possessionDate` | `projectCandidate.possessionDate` | Yes |
| `reraNumber` | `projectCandidate.reraNumber` | Always (from request) |
| `status` | `projectCandidate.status` | Yes — normalized |
| `propertyTypes` | `projectCandidate.propertyTypes` | Yes — normalized |
| `slug` | Not scraped | Set manually or auto-generated |
| `priority` | Not scraped | Set manually |
| `active` | Not scraped | Defaults at import |

### Builder create/edit form (`BuilderUpsertRequest`)

| Dashboard field | Candidate field | Available from RERA |
|---|---|---|
| `name` | `builderCandidate.name` | Yes — high confidence |
| `phone` | `builderCandidate.phone` | Yes |
| `email` | `builderCandidate.email` | Sometimes |
| `addressLine` | `builderCandidate.addressLine` | Sometimes |
| `cityId` | Resolved from `builderCandidate.cityName` | City lookup needed |
| `logoUrl` | Not scraped | Not on RERA portals |
| `description` | Not scraped | Not on RERA portals |
| `whatsapp` | Not scraped | Not on RERA portals |

### Compliance items (`DashboardProjectComplianceItemRequest`)

| Compliance item | When produced |
|---|---|
| `RERA > RERA_REGISTRATION` | Always — produced from request `reraNumber` |

Future parsers (Phase S3) can produce additional compliance candidates when the portal shows occupancy certificate status, fire NOC status, etc.

### Land utilization (`DashboardProjectLandUtilizationRequest`)

| Dashboard field | Candidate field | Available from RERA |
|---|---|---|
| `totalLandAreaSqm` | `fieldResults` where `fieldKey=totalLandAreaSqm` | Often (as "Total Area") |
| Other area breakdowns | Not scraped in S1B | Not typically on RERA portals |

### Cost breakdown (`DashboardProjectCostBreakdownRequest`)

HRERA Form A-H contains cost fields in Lakhs. The S2 `ScrapeCandidateEntityMapper` auto-derives these from raw key-values:

| Dashboard field | HRERA Form A-H key (normalized) | Conversion |
|---|---|---|
| `landCostRupees` | `landcost` | × 100,000 |
| `constructionCostRupees` | `constructioncost` | × 100,000 |
| `infrastructureCostRupees` | `infrastructurecost` | × 100,000 |
| `financeCostRupees` | `financecost` | × 100,000 |
| `otherCostRupees` | `othercost` | × 100,000 |

Values are stored as `BigDecimal` in the candidate and written to `project_cost_breakdown` during the apply step.

### Land utilization (`DashboardProjectLandUtilizationRequest`)

| Dashboard field | Candidate field | Available from RERA |
|---|---|---|
| `totalLandAreaSqm` | `fieldResults` where `fieldKey=totalLandAreaSqm` | Often (with auto unit conversion) |
| Other area breakdowns | Not scraped | Not typically on RERA portals |

### Fields NOT scraped in S1B (manual data entry required)

The following dashboard sections are entirely manual — RERA portals do not publish this data:
- **Project media** (photos, videos, brochures)
- **Floor plans** (unit types, carpet areas, configurations)
- **Connectivity** (nearby places, transport distances)
- **Construction stage percentages**
- **Amenity progress**
- **Price history chart**
- **Payment milestones**
- **Location scores**

---

## 24. Confidence Scoring

### Formula

```
confidenceScore = round( foundFields × 100.0 / totalExpectedFields )
totalExpectedFields = 18 (fixed)
```

### Status thresholds

| Score range | Status |
|---|---|
| CAPTCHA detected | `BLOCKED` (regardless of score) |
| `foundFields = 0` | `NOT_FOUND` |
| `>= 85` | `COMPLETE` |
| `50 – 84` | `PARTIAL` |
| `1 – 49` | `LOW_CONFIDENCE` |

### Expected real-world scores for RERA portals

| Scenario | Expected score | Status |
|---|---|---|
| Good RERA detail page (all key fields visible) | 55–72% | `PARTIAL` |
| Minimal RERA page (only name + builder + RERA no.) | 22–33% | `LOW_CONFIDENCE` |
| CAPTCHA blocked | 0% | `BLOCKED` |
| No result for RERA number | 0% | `NOT_FOUND` |

A `PARTIAL` result is the **expected and correct outcome** for RERA portals. Fields like price, coordinates, and description are intentionally not on RERA registrations — they are entered manually by the data-entry team during Phase S4 import review.

### Per-field confidence assignment

| Field | Base confidence | Rationale |
|---|---|---|
| `reraNumber` | 100 | Verified — this is what we searched for |
| `name`, `builderName` | 90 | Direct labeled fields on RERA page |
| `status`, `possessionDate` | 80 | Direct labeled fields |
| `cityName`, `builderPhone`, `builderEmail` | 75–80 | Usually present |
| `addressLine`, `propertyTypes`, `totalLandAreaSqm` | 75 | Present on most portals |
| `builderAddress` | 70 | Sometimes present |
| `latitude`, `longitude` | 70 | Rarely present, but high confidence if found |
| `description`, `priceMin`, `priceMax`, `builderCity` | 60 | Rarely on RERA portals |

---

## 25. HaryanaReraSourceScraper — Active Implementation

The Haryana RERA scraper is **fully active**. There is no `SELECTORS_CONFIGURED` flag — the scraper runs on every `HARYANA_RERA` request.

### Portal details

| Property | Value |
|---|---|
| Search URL | `https://haryanarera.gov.in/assistancecontrol/project_search_public/2` |
| Form name | `search_form` (POST, same-page submit) |
| District select | `select[name='district']` — visually hidden, set via JS |
| Result table | `#compliant_hearing` (jQuery DataTables plugin) |
| Result rows | `#compliant_hearing tbody tr` |

### District code mapping

The RERA number prefix determines which district to filter by before scanning 1,700+ results:

| Prefix | District code | District name |
|---|---|---|
| `GGM` | `62` | Gurugram |
| `PKL` | `70` | Panchkula |
| (any other) | `999` | All districts |

### Why JavaScript injection (not `page.selectOption`)

The `<select name="district">` element exists in the DOM but is visually hidden — Playwright's `page.selectOption()` requires visible elements and throws a timeout. The scraper instead uses `page.evaluate()` to:

1. Set `select.value` directly
2. Dispatch `change` and `input` DOM events
3. Trigger `jQuery.trigger('change')` if jQuery is available
4. Inject a hidden `<input name="basic_search">` submit signal
5. Call `form.submit()` directly

### Why DataTable search filter

The HRERA results table shows 1,730+ Gurugram projects paginated at 10 rows per page. The scraper must find one specific RERA number without clicking through 173 pages. Before scanning rows, the scraper injects the RERA number as a DataTable search filter:

```javascript
// Primary: DataTables API
jQuery('#compliant_hearing').DataTable().search(reraNumber).draw();

// Fallback: dispatch input events on filter textbox
const filterInput = document.querySelector('#compliant_hearing_filter input');
filterInput.value = reraNumber;
filterInput.dispatchEvent(new Event('input', {bubbles: true}));
filterInput.dispatchEvent(new KeyboardEvent('keyup', {bubbles: true}));
```

After the filter, only matching rows remain visible and the row-scan finds the target instantly.

### Playwright interaction sequence (active)

```
1.  page.navigate(SEARCH_URL)              ← Opens district search page
2.  captchaDetector.detect(html)           ← Abort if blocked
3.  resolveDistrictCode(reraNumber)        ← "GGM" → "62", "PKL" → "70", else "999"
4.  submitDistrictSearch(page, code)       ← JS injection: set select, form.submit()
5.  waitForLoadState(DOMCONTENTLOADED)     ← Wait for results table to render
6.  filterResultTableByReraNumber(page)    ← DataTable search filter (JS injection)
7.  waitForTimeout(1500ms)                 ← Allow DataTable to redraw
8.  JS_FIND_DETAIL_URL(page, reraNumber)   ← Scan visible rows, normalize+includes match
9.  JS_FIND_FORM_AH_URL(page, reraNumber)  ← Also extract Form A-H link if present
10. page.navigate(detailUrl)               ← Navigate to project detail page
11. waitForLoadState(DOMCONTENTLOADED)
12. captchaDetector.detect(detailHtml)     ← Abort if blocked
13. page.content() + page.screenshot()    ← Capture evidence
14. keyValueParser.parse(html, finalUrl)   ← Extract key-values (5 strategies)
```

---

## 25.5 UpReraSourceScraper — Active Implementation

The UP RERA scraper is **fully active**. It handles every `UP_RERA` request with no configuration flags.

### Portal details

| Property | Value |
|---|---|
| Search URL | `https://www.up-rera.in/Prodetails` |
| Portal type | Angular SPA (client-side routing) |
| Result container | `table tbody tr` (generic — no fixed table ID) |
| Navigation mode | Anchor `href` if present; Angular row-click fallback if not |
| Extra settle wait | 3 000 ms after `DOMCONTENTLOADED` to allow Angular bootstrap |

### Registration number format

| Format | Example | Notes |
|---|---|---|
| Standard | `UPRERAPRJ12345` | Alphanumeric only, no separators |

The normalization function strips all non-alphanumeric characters before comparison, so minor formatting differences (spaces, hyphens) between what is submitted and what the portal displays are handled automatically.

### Why no district mapping

Unlike Haryana RERA, the UP RERA portal does not require a district pre-filter. Projects are listed in a single table that is filtered directly by registration number. This removes the district-prefix lookup step entirely.

### 3-strategy table filter

The filter runs three strategies in order and returns on first success:

| Strategy | Mechanism | When it applies |
|---|---|---|
| 1 — DataTables API | `jQuery('table').DataTable().search(n).draw()` | Portal uses jQuery DataTables |
| 2 — Filter input | Dispatch `input`/`keyup`/`change` on `[type="search"]` or `.dataTables_filter input` | DataTables rendered search box is present |
| 3 — Angular input | Dispatch `input`/`keyup` on `input[placeholder*="search" i]`, `[formcontrolname]`, or `[ng-model]` | Angular Material or custom filter component |

If no strategy matches, the scraper proceeds without filtering (slower — scans all visible rows).

### Row navigation modes

The UP RERA portal may render project rows in two ways:

| Mode | Signal | Scraper action |
|---|---|---|
| Anchor link | `JS_FIND_DETAIL_URL` returns an `href` string | `page.navigate(href)` directly |
| Angular row-click | `JS_FIND_DETAIL_URL` returns `"__CLICK_ROW__"` | `JS_CLICK_MATCHING_ROW` + `waitForLoadState(NETWORKIDLE)` |

### Playwright interaction sequence (active)

```
1.  page.navigate(SEARCH_URL)              ← Opens project listing page
2.  page.waitForTimeout(3000ms)            ← Angular SPA settle
3.  page.waitForLoadState(NETWORKIDLE)
4.  captchaDetector.detect(html)           ← Abort if blocked
5.  filterTableByReraNumber(page)          ← 3-strategy filter
6.  page.waitForTimeout(2000ms)            ← Allow table to redraw
7.  waitForSelector("table tbody tr")      ← Confirm rows are present
8.  JS_FIND_DETAIL_URL(page, reraNumber)   ← Normalize+includes() scan all td cells
9a. [href found]  page.navigate(detailUrl)
9b. [click mode]  JS_CLICK_MATCHING_ROW + waitForLoadState(NETWORKIDLE)
10. captchaDetector.detect(detailHtml)     ← Abort if blocked
11. page.content() + page.screenshot()    ← Capture evidence
12. keyValueParser.parse(html, finalUrl)   ← Extract key-values (5 strategies)
```

---

# PHASE S2 — CANDIDATE PERSISTENCE

---

## 33. S2 Overview

Phase S2 introduces a **candidate staging layer** between the RERA scrape output and the live production tables. Scraped data is never written directly to `project`, `builder`, or `project_meter`. Instead it lands in 9 `dashboard_scrape_candidate_*` tables where it can be reviewed, linked, and approved before import.

### Workflow

```
RERA Scrape (S1B)
    ↓
POST /api/dashboard/scraping/candidates          ← saves candidate to DB
    ↓
ADMIN/DATA_ENTRY reviews candidate in dashboard
    ├── PATCH /{id}/link-builder                ← associate existing builder
    ├── PATCH /{id}/status                      ← advance workflow status
    └── POST  /{id}/apply-to-project            ← write to project/project_meter tables
            ↓
        DRAFT project enters normal review workflow
        (DRAFT → PENDING_REVIEW → APPROVED — separate process)
```

### What Phase S2 adds

| Capability | S2 |
|---|---|
| Persist scrape results to DB (9 tables) | Yes |
| List/filter candidates with pagination | Yes |
| Full candidate detail with all child data | Yes |
| Link an existing builder to a candidate | Yes |
| Status workflow with transition validation | Yes |
| Apply candidate → create new project draft | Yes |
| Apply candidate → update existing project draft | Yes |
| Compliance items applied from RERA candidate | Yes |
| Cost breakdown applied from HRERA Form A-H | Yes |
| Land utilization applied with unit conversion | Yes |
| Publishes project automatically | No — DRAFT only |

---

## 34. S2 Architecture

```
ScrapeCandidateController
    ├── ScrapeCandidateService          ← save, list, detail, linkBuilder, updateStatus
    │       ├── ReraNumberSearchService (S1B — called during save)
    │       ├── ScrapeCandidateEntityMapper
    │       ├── ScrapeCandidateValidationService
    │       └── 9 Repositories
    │
    └── ScrapeCandidateApplyService     ← applyToProject
            ├── ScrapeCandidateValidationService
            ├── ProjectService                    ← create / get project
            ├── DashboardProjectOwnershipService  ← edit permission check
            ├── DashboardProjectMeterWriteService ← compliance/cost/land
            └── ScrapeCandidateRepository         ← markApplied()
```

**Design decisions:**
- No cascade on child entities — `ScrapeCandidateServiceImpl.persistChildren()` saves them explicitly in dependency order.
- The entity mapper (`ScrapeCandidateEntityMapper`) creates child entities **without** a candidate reference; the service sets the reference after the root candidate is saved and has a DB-generated ID.
- `ScrapeCandidateServiceImpl.loadOrThrow()` and `buildDetailResponse()` are package-visible — `ScrapeCandidateApplyServiceImpl` (in the same package) uses them to avoid duplication.

---

## 35. S2 Package Structure

See [Section 15](#15-s1b-package-structure) — the `candidate/` subtree is documented there inline.

**Flyway migration:** `src/main/resources/db/migration/V73__create_dashboard_scrape_candidate_tables.sql`

---

## 36. S2 Database Schema

### Root table: `dashboard_scrape_candidate`

| Column | Type | Description |
|---|---|---|
| `id` | `bigserial PK` | Auto-generated |
| `source_code` | `varchar(50) NOT NULL` | `HARYANA_RERA`, `MAHA_RERA`, … |
| `rera_number` | `varchar(150) NOT NULL` | RERA registration number |
| `found` | `boolean NOT NULL` | Scrape returned data |
| `captcha_detected` | `boolean NOT NULL` | Portal blocked the request |
| `source_search_url` | `text` | Portal search page URL |
| `source_detail_url` | `text` | Portal project detail URL |
| `final_url` | `text` | Browser final URL |
| `page_title` | `text` | Page `<title>` |
| `raw_html_path` | `text` | Evidence HTML file path |
| `screenshot_path` | `text` | Evidence PNG file path |
| `confidence_score` | `int` | 0–100 |
| `confidence_status` | `varchar(30)` | `COMPLETE`, `PARTIAL`, `LOW_CONFIDENCE`, `NOT_FOUND`, `BLOCKED` |
| `total_expected_fields` | `int` | Fixed: 18 |
| `found_fields` | `int` | Count of extracted fields |
| `missing_fields` | `int` | `total - found` |
| `status` | `varchar(30) NOT NULL` | Workflow status (see S2 Status Machine) |
| `linked_builder_id` | `bigint` | FK to `builder` (optional) |
| `linked_project_id` | `bigint` | FK to `project` (optional) |
| `applied_project_id` | `bigint` | Set after apply |
| `applied_at` | `timestamptz` | When apply completed |
| `applied_by_dashboard_user_id` | `bigint` | Who triggered apply |
| `created_by_dashboard_user_id` | `bigint` | Who triggered save |
| `remarks` | `text` | Required when REJECTED |
| `created_at` | `timestamptz NOT NULL` | `BaseEntity` |
| `updated_at` | `timestamptz NOT NULL` | `BaseEntity` |

### Child tables (all have `candidate_id` FK with ON DELETE CASCADE)

| Table | Key column(s) | Notes |
|---|---|---|
| `dashboard_scrape_candidate_project` | — | One-to-one, `propertyTypes` as CSV |
| `dashboard_scrape_candidate_builder` | — | One-to-one |
| `dashboard_scrape_candidate_compliance_item` | `item_key`, `display_order` | Multiple per candidate |
| `dashboard_scrape_candidate_field_result` | `field_key`, `found` | 18 rows per candidate |
| `dashboard_scrape_candidate_raw_value` | `raw_key` | One row per extracted key |
| `dashboard_scrape_candidate_cost_breakdown` | — | UNIQUE(candidate_id) — one-to-one |
| `dashboard_scrape_candidate_land_utilization` | — | UNIQUE(candidate_id) — one-to-one |
| `dashboard_scrape_candidate_document` | `document_type` | Multiple per candidate |

---

## 37. S2 Entities

All entities extend `BaseEntity` (provides `createdAt`, `updatedAt` as `OffsetDateTime`).  
All use `@GeneratedValue(strategy = GenerationType.IDENTITY)`.  
Text fields use `@Column(columnDefinition = "text")`.  
Child entities use `@ManyToOne(fetch = FetchType.LAZY)` referencing `DashboardScrapeCandidateEntity`.

| Entity | Notable fields |
|---|---|
| `DashboardScrapeCandidateEntity` | Root — all summary + status fields |
| `DashboardScrapeCandidateProjectEntity` | `propertyTypes` as comma-separated string; `projectStatus` as enum name |
| `DashboardScrapeCandidateBuilderEntity` | Builder contact fields |
| `DashboardScrapeCandidateComplianceItemEntity` | `itemGroup`, `itemKey`, `itemLabel`, `status`, `valueText`, `displayOrder` |
| `DashboardScrapeCandidateFieldResultEntity` | `fieldKey`, `found`, `rawValue`, `normalizedValue`, `confidence`, `section` |
| `DashboardScrapeCandidateRawValueEntity` | `rawKey`, `rawValue` — one row per extracted key-value pair |
| `DashboardScrapeCandidateCostBreakdownEntity` | 5 cost fields as `BigDecimal` (rupees) |
| `DashboardScrapeCandidateLandUtilizationEntity` | `totalLandAreaSqm` as `BigDecimal` |
| `DashboardScrapeCandidateDocumentEntity` | `documentType` (`ScrapeCandidateDocumentType`), `url`, `label` |

---

## 38. S2 API Reference

Base path: `/api/dashboard/scraping/candidates`

| Method | Path | Role | Description |
|---|---|---|---|
| `POST` | `/` | `ADMIN` | Run scrape + persist as new candidate. Returns HTTP 201. |
| `GET` | `/` | `ADMIN`, `DATA_ENTRY`, `REVIEWER` | Paginated candidate list with optional filters. |
| `GET` | `/{id}` | `ADMIN`, `DATA_ENTRY`, `REVIEWER` | Full candidate detail with all child data. |
| `PATCH` | `/{id}/link-builder` | `ADMIN`, `DATA_ENTRY` | Associate an existing builder with this candidate. |
| `PATCH` | `/{id}/status` | `ADMIN`, `DATA_ENTRY` | Advance or change workflow status. |
| `POST` | `/{id}/apply-to-project` | `ADMIN`, `DATA_ENTRY` | Apply candidate to new or existing project draft. |

### GET `/` query parameters

| Param | Type | Description |
|---|---|---|
| `status` | `ScrapeCandidateStatus` | Filter by workflow status |
| `sourceCode` | `ReraSourceCode` | Filter by RERA source |
| `reraNumber` | `String` | Partial match filter on RERA number |
| `page`, `size`, `sort` | Pageable | Default: size=20, sort=createdAt DESC |

---

## 39. S2 Request & Response Schemas

### `POST /candidates` — SaveScrapeCandidateRequest

```json
{
  "sourceCode": "HARYANA_RERA",
  "reraNumber": "GGM/872/604/2024/99",
  "saveEvidence": true
}
```

Returns `ScrapeCandidateDetailResponse` (HTTP 201).

### `PATCH /{id}/link-builder` — LinkBuilderRequest

```json
{ "builderId": 42 }
```

### `PATCH /{id}/status` — UpdateCandidateStatusRequest

```json
{
  "status": "REJECTED",
  "remarks": "Duplicate of project #1234"
}
```

`remarks` is required when `status = REJECTED`.

### `POST /{id}/apply-to-project` — ApplyToProjectRequest

```json
{
  "mode": "CREATE_NEW_PROJECT",
  "builderId": 42,
  "cityId": 7,
  "overwrite": false
}
```

```json
{
  "mode": "UPDATE_EXISTING_PROJECT",
  "projectId": 99,
  "cityId": 7,
  "overwrite": true
}
```

| Field | Required | Description |
|---|---|---|
| `mode` | Yes | `CREATE_NEW_PROJECT` or `UPDATE_EXISTING_PROJECT` |
| `builderId` | Required for CREATE if not set on candidate | Resolved from candidate's `linkedBuilderId` if omitted |
| `projectId` | Required for UPDATE | Target project to update |
| `cityId` | Recommended | Long FK to `CityEntity` — passed through to `ProjectUpsertRequest` |
| `overwrite` | Default `false` | If `true`, candidate values overwrite non-null existing project fields |

### `ApplyToProjectResponse`

```json
{
  "candidateId": 17,
  "projectId": 99,
  "mode": "CREATE_NEW_PROJECT",
  "warnings": ["reraNumber truncated to 50 chars: GGM/872/604/2024/99..."]
}
```

---

## 40. S2 Data Flow — Save Candidate

```
Client
    │  POST /api/dashboard/scraping/candidates
    │  { "sourceCode": "HARYANA_RERA", "reraNumber": "GGM/872/604/2024/99" }
    ▼
ScrapeCandidateController.save()
    ▼
ScrapeCandidateServiceImpl.save()
    │
    ├─► ReraNumberSearchService.searchByReraNumber(request with includeRaw=true)
    │       → runs full S1B scrape + parse (see Section 19)
    │       → returns ReraNumberSearchResponse
    │
    ├─► ScrapeCandidateEntityMapper.toEntityGraph(response, userId)
    │       → builds DashboardScrapeCandidateEntity (root)
    │       → builds project/builder/compliance/fieldResult/rawValue/cost/land/document entities
    │       → child entities have candidate=null at this point
    │
    ├─► candidateRepository.save(candidate)   ← root saved first, gets DB ID
    │
    ├─► persistChildren(saved, graph)
    │       → sets candidate reference on each child
    │       → saves each child repository in order
    │
    └─► buildDetailResponse(saved)
            → loads all children from DB
            → returns ScrapeCandidateDetailResponse
    ▼
HTTP 201
```

---

## 41. S2 Data Flow — Apply to Project

```
Client
    │  POST /api/dashboard/scraping/candidates/{id}/apply-to-project
    │  { "mode": "CREATE_NEW_PROJECT", "builderId": 42, "cityId": 7 }
    ▼
ScrapeCandidateApplyServiceImpl.applyToProject()
    │
    ├─► loadOrThrow(id)                          ← load candidate root
    ├─► load project + compliance children
    ├─► validationService.validateApplyable()    ← status, found, name, builderId checks
    │
    ├─► [CREATE_NEW_PROJECT]
    │       resolveBuilderId (request → candidate.linkedBuilderId)
    │       buildUpsertFromCandidate()           ← map candidate fields to ProjectUpsertRequest
    │       projectService.create(builderId, upsertRequest)  ← creates DRAFT
    │
    ├─► [UPDATE_EXISTING_PROJECT]
    │       projectService.getProjectEntityOrThrow(projectId)
    │       ownershipService.assertCurrentUserCanEditProject()  ← permission check
    │       buildUpsertFromCandidate(existing)   ← merge: candidate wins if non-null + (overwrite OR existing null)
    │       projectService.update(projectId, upsertRequest)
    │
    ├─► applyComplianceItems(projectId, complianceCandidates)
    │       load all existing items for project
    │       for each candidate item:
    │           dedup in-memory by itemKey
    │           if exists → meterService.updateComplianceItem()
    │           else      → meterService.createComplianceItem()
    │
    ├─► applyCostBreakdown(projectId, candidate, overwrite)
    │       try meterService.getCostBreakdown() → merge or create
    │       catch NotFoundException             → create new
    │
    ├─► applyLandUtilization(projectId, candidate, overwrite)
    │       same pattern as cost breakdown
    │
    └─► markApplied(candidate, projectId, userId)
            → status = APPLIED
            → appliedProjectId = projectId
            → appliedAt = now()
            → save candidate
    ▼
ApplyToProjectResponse { candidateId, projectId, mode, warnings }
```

---

## 42. S2 Status Machine

### `ScrapeCandidateStatus` values

| Status | Meaning |
|---|---|
| `SCRAPED` | Scrape completed, not yet reviewed |
| `NEEDS_REVIEW` | Flagged for manual review |
| `READY_TO_APPLY` | Reviewed and approved for import |
| `APPLIED` | Applied to project — **terminal** |
| `REJECTED` | Rejected — requires remarks |
| `FAILED` | Scrape returned found=false |

### Allowed transitions

| From | Allowed → |
|---|---|
| `SCRAPED` | `NEEDS_REVIEW`, `READY_TO_APPLY`, `REJECTED` |
| `NEEDS_REVIEW` | `SCRAPED`, `READY_TO_APPLY`, `REJECTED` |
| `READY_TO_APPLY` | `SCRAPED`, `NEEDS_REVIEW`, `REJECTED` |
| `FAILED` | `NEEDS_REVIEW` |
| `REJECTED` | `NEEDS_REVIEW` |
| `APPLIED` | *(none — terminal)* |

### Additional validation rules

- `REJECTED` requires non-blank `remarks`
- `READY_TO_APPLY` requires `found = true`
- `apply-to-project` requires `status.isApplyable()` — `SCRAPED`, `NEEDS_REVIEW`, or `READY_TO_APPLY`
- `apply-to-project` requires `found = true` and a non-blank project name in the candidate

---

## 43. ScrapeCandidateEntityMapper

Converts `ReraNumberSearchResponse` → `CandidateEntityGraph` (a record holding all 9 entity types).

### Cost breakdown derivation

Reads 5 raw keys from `allExtractedKeyValues`, converts from Lakhs to rupees (× 100,000):

| Raw key (normalized) | Entity field |
|---|---|
| `landcost` | `landCostRupees` |
| `constructioncost` | `constructionCostRupees` |
| `infrastructurecost` | `infrastructureCostRupees` |
| `financecost` | `financeCostRupees` |
| `othercost` | `otherCostRupees` |

### Land utilization derivation

Checks a priority-ordered list of area keys, calls `parseAreaToSqm()` on the first non-blank value found. Conversion constants: `1 acre = 4046.8564224 sqm`, `1 hectare = 10,000 sqm`.

### Compliance item derivation

- Always produces `RERA_REGISTRATION` item (`status=OBTAINED`, `valueText=reraNumber`)
- Scans 13 Yes/No raw keys for plan approvals, utility NOCs, etc., producing `APPROVAL_NOC` or `UTILITY` items when the value matches "yes"

### Document derivation

Creates `RERA_DETAIL_PAGE` document entry pointing to `sourceDetailUrl`. If a Form A-H URL was captured, creates a `FORM_AH` document entry.

---

## 44. S2 Testing Guide

### Test S2-1 — Save a candidate (ADMIN)

```
POST http://localhost:8080/api/dashboard/scraping/candidates
Authorization: Bearer {{adminAccessToken}}
Content-Type: application/json

{
  "sourceCode": "HARYANA_RERA",
  "reraNumber": "GGM/872/604/2024/99",
  "saveEvidence": true
}
```

Expected: HTTP 201, `id` in response, `status: "SCRAPED"`.

### Test S2-2 — List candidates

```
GET http://localhost:8080/api/dashboard/scraping/candidates?status=SCRAPED&size=10
Authorization: Bearer {{adminAccessToken}}
```

Expected: HTTP 200, paginated list with `projectName` and `builderName` populated.

### Test S2-3 — Get candidate detail

```
GET http://localhost:8080/api/dashboard/scraping/candidates/{{candidateId}}
Authorization: Bearer {{adminAccessToken}}
```

Expected: HTTP 200, full detail with `fieldResults`, `rawValues`, `complianceCandidates`.

### Test S2-4 — Status transitions

```
PATCH http://localhost:8080/api/dashboard/scraping/candidates/{{candidateId}}/status
{ "status": "NEEDS_REVIEW" }
→ Expected: 200

PATCH .../status
{ "status": "REJECTED" }          ← no remarks
→ Expected: 400 "remarks are required when rejecting"

PATCH .../status
{ "status": "REJECTED", "remarks": "Duplicate project" }
→ Expected: 200, status=REJECTED

PATCH .../status
{ "status": "APPLIED" }           ← cannot manually set terminal status
→ Expected: 400 transition error
```

### Test S2-5 — Apply to project (CREATE_NEW_PROJECT)

```
POST http://localhost:8080/api/dashboard/scraping/candidates/{{candidateId}}/apply-to-project
{
  "mode": "CREATE_NEW_PROJECT",
  "builderId": 42,
  "cityId": 7,
  "overwrite": false
}
```

Expected: HTTP 200, `projectId` in response. Verify in `GET /api/dashboard/projects/{{projectId}}`:
- `status: DRAFT`
- `reraNumber` set (truncated if > 50 chars)
- `name` populated from candidate

### Test S2-6 — Apply to existing project (UPDATE_EXISTING_PROJECT)

```
POST .../apply-to-project
{
  "mode": "UPDATE_EXISTING_PROJECT",
  "projectId": 99,
  "overwrite": true
}
```

Expected: HTTP 200. Verify project fields updated from candidate.

### Test S2-7 — Reviewer read-only (role check)

```
PATCH .../status   Authorization: Bearer {{reviewerToken}}
→ Expected: 403 DASHBOARD_ACCESS_DENIED

GET .../candidates  Authorization: Bearer {{reviewerToken}}
→ Expected: 200 (read is allowed)
```

---

# COMMON SECTIONS

---

## 26. Security

### Authentication — two layers

1. **`DashboardJwtAuthenticationFilter`** (Order 1 chain, matcher `/api/dashboard/**`):
   - Validates dashboard JWT from `Authorization: Bearer <token>`
   - Populates `SecurityContextHolder` with `DashboardUserDetails`
   - Unauthenticated → `DashboardAuthenticationEntryPoint` → HTTP 401

2. **`@PreAuthorize`** annotations per controller/method:

| Controller | Method | Allowed roles |
|---|---|---|
| `DashboardScrapingTestController` | POST `/test-url` | `ADMIN` |
| `DashboardReraSearchController` | POST `/rera/search-by-number` | `ADMIN` |
| `ScrapeCandidateController` | POST `/candidates` (save) | `ADMIN` |
| `ScrapeCandidateController` | GET `/candidates`, GET `/candidates/{id}` | `ADMIN`, `DATA_ENTRY`, `REVIEWER` |
| `ScrapeCandidateController` | PATCH `/candidates/{id}/link-builder` | `ADMIN`, `DATA_ENTRY` |
| `ScrapeCandidateController` | PATCH `/candidates/{id}/status` | `ADMIN`, `DATA_ENTRY` |
| `ScrapeCandidateController` | POST `/candidates/{id}/apply-to-project` | `ADMIN`, `DATA_ENTRY` |

Non-allowed roles → `DashboardAccessDeniedHandler` → HTTP 403.

**`SecurityConfig.java` was not modified.** All endpoints inherit the existing `/api/dashboard/**` security chain.

### Role restriction rationale

- **`ADMIN` only for save/scrape**: Launching a real browser and writing to disk is a privileged server-side action.
- **`DATA_ENTRY` can review and apply**: Data-entry staff are responsible for verifying candidate data and importing it into the project workflow.
- **`REVIEWER` read-only on candidates**: Reviewers can inspect what was scraped without modifying state.

### What the scraping+candidate endpoints do NOT do

- The S1/S1B endpoints do not write to any production table
- The S2 `apply-to-project` endpoint creates a **DRAFT** project — it does not publish anything
- No endpoints bypass authentication or expose raw stack traces
- No CAPTCHA bypass or proxy rotation

---

## 27. Dependencies

### Added to `pom.xml`

```xml
<!-- Playwright - headless browser for dynamic page rendering and form interaction -->
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.56.0</version>
</dependency>

<!-- Jsoup - HTML parser for scraped content -->
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.18.3</version>
</dependency>
```

### Pre-existing dependencies used

| Dependency | Used for |
|---|---|
| `spring-boot-starter-web` | REST controllers, `ResponseEntity` |
| `spring-boot-starter-security` | `@PreAuthorize`, security chain |
| `spring-boot-starter-validation` | `@Valid`, `@NotBlank`, `@Pattern`, `@NotNull` |
| `lombok` | `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j` |
| `jackson-databind` | JSON serialization, `@JsonInclude` |

### Playwright browser binary

Playwright downloads a pinned Chromium binary separately from the JAR. Must be installed once per environment:

```bash
mvn exec:java -e \
  -Dexec.mainClass=com.microsoft.playwright.CLI \
  -Dexec.args="install chromium"
```

Default locations: `~/.cache/ms-playwright/` (Linux/macOS), `%USERPROFILE%\AppData\Local\ms-playwright\` (Windows). Size: ~150 MB.

---

## 28. Setup & Installation

### Prerequisites

- Java 17+
- Maven 3.8+
- Internet access (for initial Chromium download)
- Write access to `/tmp/sfs-scrapes/` (created automatically on startup)

### Step 1 — Compile

```bash
mvn clean compile
```

### Step 2 — Install Chromium

```bash
mvn exec:java -e \
  -Dexec.mainClass=com.microsoft.playwright.CLI \
  -Dexec.args="install chromium"
```

Run once per machine. Re-run only when upgrading the Playwright version.

### Step 3 — Start application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

On startup you should see:
```
INFO  LocalScrapeFileStorageService - Scrape file storage directory ready: /tmp/sfs-scrapes
```

---

## 29. Testing Guide

### Obtain ADMIN JWT

```
POST http://localhost:8080/api/dashboard/auth/login
{ "username": "admin@sfs.internal", "password": "..." }
```

---

### S1 Tests — Feasibility Test Endpoint

#### Test 1 — Baseline with example.com

```
POST http://localhost:8080/api/dashboard/scraping/test-url
Authorization: Bearer {{adminAccessToken}}

{ "url": "https://example.com" }
```

| Field | Expected |
|---|---|
| `title` | `"Example Domain"` |
| `htmlLength` | `> 0` |
| `captchaDetected` | `false` |
| `rawHtmlSaved` | `true` |
| `screenshotSaved` | `true` |

Verify on disk:
```bash
ls -lh /tmp/sfs-scrapes/
```

#### Test 2 — SSRF protection (all must return HTTP 400)

```json
{ "url": "http://localhost:8080/actuator" }
{ "url": "http://127.0.0.1/admin" }
{ "url": "http://192.168.1.1" }
{ "url": "http://10.0.0.1" }
{ "url": "ftp://some-server.com" }
{ "url": "" }
```

#### Test 3 — Non-ADMIN role (must return HTTP 403)

```
POST /api/dashboard/scraping/test-url
Authorization: Bearer {{reviewerToken}}
→ Expected: 403 DASHBOARD_ACCESS_DENIED
```

---

### S1B Tests — RERA Number Search Endpoint

#### Test 4 — Live Haryana RERA search

```
POST http://localhost:8080/api/dashboard/scraping/rera/search-by-number
Authorization: Bearer {{adminAccessToken}}

{ "sourceCode": "HARYANA_RERA", "reraNumber": "GGM/872/604/2024/99", "saveEvidence": true, "includeRaw": true }
```

Expected: HTTP 200, `found: true`, `summary.status: "PARTIAL"` (55–72% typical for RERA portals).

**What to check:**
- `projectCandidate.name` populated
- `builderCandidate.name` populated
- `complianceCandidates[0].itemKey` = `"RERA_REGISTRATION"`
- `raw.allExtractedKeyValues` contains HRERA field names as normalized keys
- `rawHtmlPath` + `screenshotPath` exist on disk

#### Test 5 — Live UP RERA search

```
POST http://localhost:8080/api/dashboard/scraping/rera/search-by-number
Authorization: Bearer {{adminAccessToken}}

{ "sourceCode": "UP_RERA", "reraNumber": "UPRERAPRJ12345", "saveEvidence": true, "includeRaw": true }
```

Expected: HTTP 200. What to check:
- `found: true` (if RERA number exists on the portal)
- `summary.status` is `PARTIAL` or `LOW_CONFIDENCE` (UP RERA pages typically expose fewer fields than HRERA)
- `projectCandidate.name` and `builderCandidate.name` populated
- `complianceCandidates[0].itemKey` = `"RERA_REGISTRATION"`
- `rawHtmlPath` + `screenshotPath` exist on disk under `/tmp/sfs-scrapes/rera-up_rera-*/`
- `warnings` — check for filter-strategy fallback messages if the portal structure changed

#### Test 5B — Unimplemented source

```json
{ "sourceCode": "MAHA_RERA", "reraNumber": "P51800012345" }
```

Expected: HTTP 400 `"Source not implemented yet: MAHA_RERA"`

#### Test 6 — Validation errors

```json
{ "sourceCode": null, "reraNumber": "" }
```

Expected: HTTP 400 `DASHBOARD_VALIDATION_FAILED` with `fieldErrors`.

#### Test 7 — After selectors configured (real RERA portal)

```json
{
  "sourceCode": "HARYANA_RERA",
  "reraNumber": "GGM/872/604/2024/99",
  "saveEvidence": true,
  "includeRaw": true
}
```

**Check in response:**
- `found` is `true`
- `summary.status` is `PARTIAL` (55–72% expected for RERA portals)
- `projectCandidate.name` is populated
- `builderCandidate.name` is populated
- `complianceCandidates[0].itemKey` is `"RERA_REGISTRATION"`
- `fieldResults` has entries for all 18 expected fields
- `rawHtmlPath` and `screenshotPath` exist on disk

#### Test 8 — Inspect evidence files

```bash
ls -lh /tmp/sfs-scrapes/rera-haryana_rera-*/
open /tmp/sfs-scrapes/rera-haryana_rera-<timestamp>-<id>.png
```

---

## 30. Error Handling

All exceptions are handled by `DashboardExceptionHandler` (`@RestControllerAdvice`). No stack traces in responses.

| Exception | HTTP | Code | When thrown |
|---|---|---|---|
| `MethodArgumentNotValidException` | 400 | `DASHBOARD_VALIDATION_FAILED` | Bean validation failure (`@NotBlank`, `@NotNull`, `@Pattern`) |
| `IllegalArgumentException` | 400 | `DASHBOARD_INVALID_WORKFLOW` | SSRF guard; malformed URL; source not implemented |
| `IllegalStateException` | 400 | `DASHBOARD_INVALID_WORKFLOW` | Selectors not configured; navigation timeout; Chromium not installed; Playwright error |
| `AccessDeniedException` | 403 | `DASHBOARD_ACCESS_DENIED` | Non-ADMIN role |
| `Exception` (catch-all) | 500 | `DASHBOARD_INTERNAL_ERROR` | Unexpected runtime error |

**Non-fatal events** — reported in `warnings[]` within the HTTP 200 response:
- NETWORKIDLE wait timeout (S1)
- File save failure (S1 + S1B)
- Screenshot capture failure
- Detail page navigation failure (S1B)
- Parse failure

---

## 31. Limitations

### Phase S1 Limitations

| Limitation | Detail |
|---|---|
| No DB persistence | Scrape results ephemeral — not stored |
| No job tracking | No scrape job ID, status history, or audit log |
| Single-use browser | New Chromium process per request — no pool |
| No proxy rotation | All requests from server's IP |
| No CAPTCHA bypass | Detected but not solved |
| Generic parser only | No portal-specific column mapping |
| Local file storage only | `/tmp/sfs-scrapes` — files lost on restart |
| No file cleanup | Directory grows indefinitely |
| ADMIN-only manual trigger | No scheduling |

### Phase S1B Limitations

| Limitation | Detail |
|---|---|
| Only Haryana RERA + UP RERA implemented | `MAHA_RERA`, `KARNATAKA_RERA` not built yet |
| UP RERA Angular filter is best-effort | 3-strategy filter covers DataTables and Angular inputs; if portal markup changes, filter may fall back to full table scan (slower) |
| No pagination of district results | Only the first DataTable-filtered match is scraped |
| Confidence score is field-count based | Does not account for value quality or cross-field consistency |
| Date parsing may fail for unusual formats | Returns raw string in `value` with no date normalization |
| Price fields rarely on RERA portals | `priceMin`/`priceMax` almost always missing |

### Phase S2 Limitations

| Limitation | Detail |
|---|---|
| No city ID auto-resolution | `cityName` (string) must be supplied as `cityId` (Long) in the apply request |
| RERA number truncation at 50 chars | `ProjectUpsertRequest.reraNumber` has `@Size(max=50)` — longer values are truncated with a warning |
| No duplicate detection at save | Does not check if a candidate with the same RERA number already exists |
| No duplicate detection at apply | Does not prevent applying the same candidate twice to different projects |
| Apply writes DRAFT only | Published project state requires a separate reviewer approval step |
| Cost/land overwrite is all-or-nothing | `overwrite=true` replaces all cost/land fields; no per-field merge |
| Local file evidence | HTML + screenshot evidence stays in `/tmp/sfs-scrapes` — not stored in S3 |

---

## 32. Roadmap

### Phase S2 — Candidate Persistence ✅ Complete

- ~~Create staging tables~~ — V73 migration: 9 tables (`dashboard_scrape_candidate` + 8 child tables)
- ~~Candidate save/list/detail/status/link-builder APIs~~ — `ScrapeCandidateController`
- ~~Apply to project pipeline~~ — `ScrapeCandidateApplyServiceImpl` (CREATE_NEW_PROJECT + UPDATE_EXISTING_PROJECT)
- ~~Cost breakdown derivation from raw HRERA Form A-H keys~~ — `ScrapeCandidateEntityMapper`
- ~~Land utilization with unit conversion~~ — `ScrapeCandidateEntityMapper.deriveAreaSqm()`
- Remaining: Dashboard UI for candidate review (React frontend not yet built)
- Remaining: City ID resolution helper (auto-match `cityName` → `CityEntity`)
- Remaining: Duplicate detection (check by RERA number before saving candidate)

### Phase S3 — Additional RERA Sources

- ~~Build `UpReraSourceScraper` for `up-rera.in`~~ ✅ **Complete** — direct registration-number search, 3-strategy Angular-aware filter, row-click fallback
- Build `MahaReraSourceScraper` for `maharera.mahaonline.gov.in`
- Build `KarnatakaReraSourceScraper` for `rera.karnataka.gov.in`
- Build `GujaratReraSourceScraper` for `gujrera.gujarat.gov.in`
- Add portal-specific synonym overrides on top of the generic mapper
- Handle portal-specific pagination (HRERA approach: DataTable filter — UP RERA: Angular SPA — others vary)
- Handle portal-specific date format and area unit quirks

### Phase S4 — Production-Grade Infrastructure

- Replace local Playwright with managed scraping service:
  - `ApifyScrapingProvider` or `ZyteScrapingProvider` for proxy rotation and CAPTCHA solving
- Upload evidence (HTML + screenshot) to S3 via `ScrapeFileStorageService` implementation swap
- Scheduled scraping: Spring `@Scheduled` or Quartz for nightly RERA portal sync
- Alerting: notify when `captchaDetected` rate exceeds threshold across runs
- Source config table: manage RERA portal URLs and selectors in DB instead of code constants
- Confidence threshold alerting: flag if average confidence score drops significantly

---

*This documentation covers Phase S1, S1B, and S2. Update this file as each new phase is implemented.*
