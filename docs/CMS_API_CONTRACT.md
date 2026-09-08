# SFS CMS API Contract

Certified against migration `V155` on 2026-08-19; migration head is now `V156` (additive schema-version-3 CHECK widening only — no contract change, see `CMS_PHASE8_CERTIFICATION.md`). This is the dashboard team's backend source of truth. JSON field names are case-sensitive. Dashboard endpoints use the dashboard JWT system and `Authorization: Bearer <accessToken>`; public endpoints are anonymous.

## Common conventions

- JSON content type: `application/json`.
- Dashboard paging is Spring `Page`: `content`, `number`, `size`, `totalElements`, `totalPages`, `first`, `last`. Page sizes are clamped to 1–50, except author/category/tag lookups which allow 1–100.
- Public paging is `content`, `page`, `size`, `totalElements`, `totalPages`, `last`; size must be 1–50.
- Content metadata, document, and workflow commands share one `ContentPost.version`. A stale write returns `409 CONTENT_VERSION_CONFLICT`.
- Metadata and document writes are allowed only in `DRAFT` or `CHANGES_REQUESTED`. Workflow state is never accepted by those DTOs.
- Unknown JSON fields are rejected on security-sensitive and rich-document DTOs.
- Dashboard error envelope:

```json
{
  "success": false,
  "status": 409,
  "error": "CONFLICT",
  "code": "CONTENT_VERSION_CONFLICT",
  "message": "Content was modified by another user. Reload it before saving again.",
  "path": "/api/dashboard/cms/content/42",
  "method": "PUT",
  "requestId": "...",
  "timestamp": "2026-08-19T16:00:00+05:30"
}
```

- Public error envelope: `timestamp`, `status`, `code`, `message`, `path`. Public 400/404/503 responses use `Cache-Control: no-store`.

## Endpoint inventory

### Authentication

| Method and path | Authentication | Request / query | Success |
|---|---|---|---|
| `POST /api/dashboard/auth/login` | Public | `DashboardLoginRequest {email,password}` | `200 DashboardAuthResponse` |
| `POST /api/dashboard/auth/refresh` | Public; valid refresh token required | `{refreshToken}` | `200 DashboardAuthResponse`; refresh token rotates |
| `POST /api/dashboard/auth/logout` | Public; token is idempotently revoked | `{refreshToken}` | `200` empty body |
| `GET /api/dashboard/auth/me` | Dashboard bearer | none | `200 DashboardUserResponse` |

Login/refresh response:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "opaque-token",
  "tokenType": "Bearer",
  "expiresInMs": 900000,
  "user": {
    "id": 12,
    "name": "Content Writer",
    "email": "writer@squarefootstory.com",
    "role": "CONTENT_STAFF",
    "active": true,
    "permissions": ["CMS_CONTENT_CREATE", "CMS_CONTENT_EDIT_OWN", "CMS_CONTENT_SUBMIT_REVIEW", "CMS_CONTENT_PREVIEW", "CMS_MEDIA_UPLOAD"],
    "permissionProfiles": ["WRITER"]
  }
}
```

The access-token lifetime is returned by the server (15 minutes by the normal/prod contract unless overridden). Refresh tokens default to 15 days and rotate on every refresh. Store the refresh token in the dashboard's secure credential mechanism; never place it in a URL. Disabled users cannot log in, refresh, or use an access token after the identity cache reload (maximum configured dashboard cache staleness is 15 seconds; management mutations invalidate it after commit). Password reset/deactivation revokes all refresh tokens; already-issued access JWTs have no token-version revocation and may remain usable until expiry/cache reload as documented above.

Errors: `400 DASHBOARD_VALIDATION_FAILED/DASHBOARD_INVALID_JSON`, `401 DASHBOARD_AUTHENTICATION_FAILED`, and rate-limit `429` where configured.

### Dashboard users (ADMIN only)

| Method and path | Request / query | Success |
|---|---|---|
| `POST /api/dashboard/users` | `DashboardUserCreateRequest` | `201 DashboardUserResponse` |
| `GET /api/dashboard/users` | `role`, `active`, `search`, `page`, `size`, `sortBy`, `sortDirection` | `200 Page<DashboardUserResponse>` |
| `GET /api/dashboard/users/{userId}` | none | `200 DashboardUserResponse` |
| `PUT /api/dashboard/users/{userId}/cms-permissions` | `{permissionProfiles:[WRITER|EDITOR|PUBLISHER]}` | `200 DashboardUserResponse` |
| `PATCH /api/dashboard/users/{userId}/status` | `{active:boolean}` | `200 DashboardUserResponse` |
| `PUT /api/dashboard/users/{userId}/password` | `{newPassword}` | `204` |

Create example:

```json
{
  "email": "writer@squarefootstory.com",
  "displayName": "Content Writer",
  "role": "CONTENT_STAFF",
  "permissionProfiles": ["WRITER"],
  "initialPassword": "Initial!Pass123"
}
```

Only `CONTENT_STAFF` can be created. Passwords are 12–128 characters and require uppercase, lowercase, digit, and special characters. Responses include `id,email,displayName,role,active,permissions,permissionProfiles,createdAt,updatedAt` and never a password/token. Sort allowlist: `id,email,displayName,role,active,createdAt,updatedAt`; maximum size 50. Self-deactivation and deactivation of the final active ADMIN are rejected. CMS profiles cannot be assigned to non-content staff.

### Authors, categories, and tags (create/update ADMIN only; read requires `CMS_CONTENT_PREVIEW`)

| Method and path | Request | Success |
|---|---|---|
| `POST /api/dashboard/cms/authors` | `CmsAuthorRequest` | `201 CmsAuthorResponse` |
| `GET /api/dashboard/cms/authors` | `active`, `search`, `page`, `size` | `200 Page<CmsAuthorResponse>` |
| `GET /api/dashboard/cms/authors/{id}` | none | `200 CmsAuthorResponse` |
| `PUT /api/dashboard/cms/authors/{id}` | `CmsAuthorRequest` with expected `version` | `200 CmsAuthorResponse` |
| `POST /api/dashboard/cms/categories` | `CmsCategoryRequest` | `201 CmsCategoryResponse` |
| `GET /api/dashboard/cms/categories` | `active`, `search`, `page`, `size` | `200 Page<CmsCategoryResponse>` |
| `GET /api/dashboard/cms/categories/{id}` | none | `200 CmsCategoryResponse` |
| `PUT /api/dashboard/cms/categories/{id}` | `CmsCategoryRequest` with expected `version` | `200 CmsCategoryResponse` |
| `POST /api/dashboard/cms/tags` | `CmsTagRequest` | `201 CmsTagResponse` |
| `GET /api/dashboard/cms/tags` | `active`, `search`, `page`, `size` | `200 Page<CmsTagResponse>` |
| `GET /api/dashboard/cms/tags/{id}` | none | `200 CmsTagResponse` |
| `PUT /api/dashboard/cms/tags/{id}` | `CmsTagRequest` with expected `version` | `200 CmsTagResponse` |

```json
{"displayName":"Arpit Mishra","slug":"arpit-mishra","bio":"Real-estate analyst.","designation":"Senior Analyst","profileMediaAssetId":481,"active":true,"version":0}
```

```json
{"name":"Market Insights","slug":"market-insights","description":"Market reporting.","active":true,"version":0}
```

```json
{"name":"Gurgaon","slug":"gurgaon","active":true,"version":0}
```

`POST`/`PUT` require `hasRole('ADMIN')`. `GET` (list and by-id) requires only `CMS_CONTENT_PREVIEW` — any WRITER/EDITOR/PUBLISHER-profile account can read taxonomy for its own pickers, per `@cmsContentAccessPolicy.canReadTaxonomy(authentication)`; it is not ADMIN-gated. On create, omit `version`; on update it is required by service policy. Slugs may be omitted for generation. Author profile media must be a READY IMAGE. No delete routes exist; set `active:false`. Search is case-insensitive and page size is clamped to 100. Responses mirror request fields and add `id,createdAt,updatedAt,version`. Errors include `404 CMS_METADATA_NOT_FOUND`, `409 CMS_METADATA_CONFLICT`, `409 CONTENT_VERSION_CONFLICT`, and dashboard validation errors.

### Content draft shell

| Method and path | Permission | Request / query | Success |
|---|---|---|---|
| `POST /api/dashboard/cms/content` | `CMS_CONTENT_CREATE` | `ContentPostCreateRequest` | `201 ContentPostDetailResponse` |
| `GET /api/dashboard/cms/content/{contentId}` | owner preview or view-all policy | none | `200 ContentPostDetailResponse` |
| `GET /api/dashboard/cms/content` | `CMS_CONTENT_PREVIEW`; owner-scoped unless view-all | filters below | `200 Page<ContentPostListResponse>` |
| `PUT /api/dashboard/cms/content/{contentId}` | `EDIT_OWN` + owner, or `EDIT_ANY` | `ContentPostUpdateRequest` | `200 ContentPostDetailResponse` |

Create:

```json
{
  "contentType": "ARTICLE",
  "title": "Best Places to Buy Property in Gurgaon",
  "slug": "best-places-to-buy-property-in-gurgaon",
  "excerpt": "A practical market guide.",
  "seoTitle": "Best Places to Buy Property in Gurgaon",
  "seoDescription": "Compare established and emerging Gurgaon markets.",
  "canonicalUrl": null,
  "robotsIndex": true,
  "robotsFollow": true,
  "publicAuthorId": 3,
  "categoryId": 5,
  "tagIds": [2, 9],
  "coverMediaAssetId": 481,
  "coverAltText": "Gurgaon residential skyline"
}
```

Update uses the same editable fields and additionally requires `version`; `slug`, `robotsIndex`, and `robotsFollow` are required. `contentType` is `ARTICLE|BLOG|INTERVIEW`. The server owns status, owner/actors, timestamps, workflow pointers, and version. Drafts may omit author/category/cover; review submission may not. A post has at most 15 tags. Canonical URL is null or HTTPS. Public-facing strings and references are validated server-side.

Detail contains `id,contentType,status,title,slug,excerpt,publicAuthorId,categoryId,tagIds,coverMediaAssetId,coverAltText`, owner/creator/updater IDs and names, SEO fields, robots flags, workflow revision pointers, publication actor/time, timestamps, and `version`. List is metadata-only and excludes document/media. List filters: `contentType,status,ownerId,search,page,size`; sort allowlist `id,createdAt,updatedAt,title`, direction `asc|desc`, max 50.

### Rich document

| Method and path | Permission | Request | Success |
|---|---|---|---|
| `GET /api/dashboard/cms/content/{contentId}/document` | content view policy | none | `200 ContentDocumentResponse` |
| `PUT /api/dashboard/cms/content/{contentId}/document` | content edit policy | `{version,document}` | `200 ContentDocumentResponse` |

Response: `contentId,version,updatedAt,document,wordCount,media`. `media` is a map keyed by media ID with short-lived dashboard `previewUrl` and expiry; never persist it in the document.

Current write schema is version 3; stored versions 1, 2, and 3 are readable — GET never re-validates, it returns the row as stored. Any PUT is re-validated, canonicalized, and re-saved as version 3 regardless of the `schemaVersion` sent (lazy upcast-on-write; old rows are never rewritten in bulk). Supported blocks are `PARAGRAPH`, `HEADING` (`H2|H3|H4`), `BULLET_LIST`, `ORDERED_LIST`, `BLOCKQUOTE`, `DIVIDER`, `IMAGE`, `VIDEO`, `EMBED`, `CHECK_LIST`, `CALLOUT`, and `TABLE`. Inline nodes are `TEXT` and `HARD_BREAK`; marks are `BOLD`, `ITALIC`, `UNDERLINE`, and `LINK`. Links accept HTTPS or safe relative paths only. `IMAGE` uses `mediaAssetId,decorative,altText,caption,layout,link`; layout is `STANDARD|WIDE`. `VIDEO` uses `mediaAssetId,posterMediaAssetId,caption`. `EMBED` uses `provider:YOUTUBE`, canonical `externalId`, and optional caption. `CHECK_LIST` is `items:[{content}]`, same shape as `BULLET_LIST`/`ORDERED_LIST` items — an editorial "verified/features" list, not an interactive to-do list (items carry no checked state). `CALLOUT` is `variant,title,content`; `variant` is `INFO|NOTE|VERDICT|WARNING`, `title` is optional (max 120 chars, never defaulted server-side). `TABLE` is `caption,columns,rows`; `columns` is 1–8 `{label}` entries (max 60 chars each) that double as the header row; `rows` is 1–100 entries of `{rowType,cells}` where `rowType` is `NORMAL|SECTION|TOTAL`, `cells` is inline-node arrays (max 20 nodes/cell), `NORMAL`/`TOTAL` rows need exactly one cell per column, and a `SECTION` row is a single full-width cell; at least one cell in the table must have visible text. No HTML, iframe, `src`, CSS, event handlers, colspan/rowspan, or arbitrary URLs are accepted anywhere.

```json
{
  "version": 10,
  "document": {
    "schemaVersion": 3,
    "blocks": [
      {"type":"HEADING","level":"H2","content":[{"type":"TEXT","text":"Market overview","marks":[]}]},
      {"type":"PARAGRAPH","content":[{"type":"TEXT","text":"Read our guide","marks":[{"type":"LINK","href":"/projects/27","openInNewTab":false,"nofollow":false,"sponsored":false}]}]},
      {"type":"IMAGE","mediaAssetId":481,"decorative":false,"altText":"Apartment clubhouse","caption":[],"layout":"WIDE"},
      {"type":"VIDEO","mediaAssetId":512,"posterMediaAssetId":481,"caption":[]},
      {"type":"EMBED","provider":"YOUTUBE","externalId":"dQw4w9WgXcQ","caption":[]},
      {"type":"CHECK_LIST","items":[{"content":[{"type":"TEXT","text":"RERA registered","marks":[]}]}]},
      {"type":"CALLOUT","variant":"VERDICT","title":"SFS Verdict","content":[{"type":"TEXT","text":"Strong pick.","marks":[]}]},
      {"type":"TABLE","caption":"Price comparison","columns":[{"label":"Project"},{"label":"Price/sqft"}],"rows":[
        {"rowType":"NORMAL","cells":[[{"type":"TEXT","text":"Skyline Heights","marks":[]}],[{"type":"TEXT","text":"₹8,200","marks":[]}]]}
      ]}
    ]
  }
}
```

Limits: request 1 MiB; canonical serialized document 768 KiB; 750 blocks; 500 inline nodes/block; 200 list items (also applies to `CHECK_LIST`); 100 inline nodes/list item; 20,000 chars/text node; 500,000 total text chars; 4 marks/text node; 300-char alt; 50 caption nodes/1,000 caption chars; 120-char callout title; 8 table columns (60-char label); 100 table rows; 20 inline nodes/table cell; 200-char table caption. Errors: `400 CONTENT_DOCUMENT_INVALID`, `400 CONTENT_DOCUMENT_SCHEMA_UNSUPPORTED`, `413 CONTENT_DOCUMENT_TOO_LARGE`, media-reference errors, `409 CONTENT_NOT_EDITABLE`, and `409 CONTENT_VERSION_CONFLICT`.

### CMS media

| Method and path | Permission | Request / query | Success |
|---|---|---|---|
| `POST /api/dashboard/cms/media/uploads` | `CMS_MEDIA_UPLOAD` | `CmsMediaUploadRequest` | `201 CmsMediaUploadResponse` |
| `POST /api/dashboard/cms/media/{mediaId}/complete` | `CMS_MEDIA_UPLOAD`; creator/admin finalize policy | none | `200 CmsMediaAssetResponse` |
| `GET /api/dashboard/cms/media` | CMS read policy | filters below | `200 Page<CmsMediaAssetResponse>` |
| `GET /api/dashboard/cms/media/{mediaId}` | CMS read policy | none | `200 CmsMediaAssetResponse` |

```json
{"mediaType":"IMAGE","filename":"cover.jpg","contentType":"image/jpeg","sizeBytes":2481920}
```

Upload response:

```json
{"mediaAssetId":481,"status":"PENDING_UPLOAD","uploadUrl":"https://...","expiresInSeconds":300,"assetExpiresAt":"...","requiredHeaders":{"Content-Type":"image/jpeg","Cache-Control":"public, max-age=31536000, immutable","If-None-Match":"*"}}
```

The client must PUT the bytes to `uploadUrl` with every `requiredHeaders` entry, then call complete. Presigned URLs are secrets and must not be logged or persisted. Supported declarations/signatures: JPEG, PNG, WebP up to 15 MiB and MP4 up to 250 MiB. SVG and mismatched/fake content are rejected. Complete verifies object existence, size, metadata, signature, dimensions for images, and changes `PENDING_UPLOAD` to `READY` or `FAILED`; READY completion is idempotent. List filters: `mediaType,status,createdBy,search,page,size`; sort allowlist `id,createdAt,updatedAt,filename`; maximum 50.

`CmsMediaAssetResponse` includes `id,mediaType,status,filename,contentType,sizeBytes,width,height,durationMillis,createdBy,createdByName,createdAt,updatedAt,readyAt,failureCode,previewUrl,previewExpiresInSeconds`. Errors: `CMS_MEDIA_NOT_FOUND` 404, `CMS_MEDIA_INVALID_TYPE` 400, `CMS_MEDIA_TOO_LARGE` 413, `CMS_MEDIA_VALIDATION_FAILED` 422, `CMS_MEDIA_UPLOAD_INCOMPLETE` 409, `CMS_MEDIA_NOT_READY` 409, `CMS_MEDIA_STORAGE_UNAVAILABLE` 503.

### Workflow, revisions, and history

| Method and path | Permission / state | Request | Success |
|---|---|---|---|
| `POST .../{contentId}/workflow/submit` | submit permission + ownership/edit; `DRAFT|CHANGES_REQUESTED` | `{version}` | `200 ContentWorkflowResponse` |
| `POST .../{contentId}/workflow/request-changes` | `CMS_CONTENT_REVIEW`; `IN_REVIEW` | `{version,comment}` | `200 ContentWorkflowResponse` |
| `POST .../{contentId}/workflow/approve` | `CMS_CONTENT_REVIEW`; `IN_REVIEW` | `{version}` | `200 ContentWorkflowResponse` |
| `POST .../{contentId}/workflow/publish` | `CMS_CONTENT_PUBLISH`; `APPROVED|UNPUBLISHED` | `{version}` | `200 ContentWorkflowResponse` |
| `POST .../{contentId}/workflow/unpublish` | `CMS_CONTENT_UNPUBLISH`; `PUBLISHED` | `{version}` | `200 ContentWorkflowResponse` |
| `POST .../{contentId}/workflow/archive` | `CMS_CONTENT_ARCHIVE`; eligible non-published state | `{version}` | `200 ContentWorkflowResponse` |
| `GET .../{contentId}/revisions` | content view policy | `page,size` | `200 Page<ContentRevisionListResponse>` |
| `GET .../{contentId}/revisions/{revisionId}` | content view policy | none | `200 ContentRevisionDetailResponse` |
| `GET .../{contentId}/workflow-history` | content view policy | `page,size` | `200 Page<ContentWorkflowActivityResponse>` |

The omitted prefix is `/api/dashboard/cms/content`. Request-changes `comment` is required plain text, max 4,000. `ContentWorkflowResponse` contains `contentId,status,version,currentReviewRevisionId,approvedRevisionId,currentPublishedRevisionId,publishedAt,publishedByDashboardUserId`.

Submit atomically snapshots all public metadata and document. Approval and publication use that exact immutable revision. Revisions list returns metadata only; detail includes the snapshot document and one bulk-resolved media map. Workflow history contains `id,revisionId,revisionNumber,action,comment,actorDashboardUserId,actorDisplayName,createdAt`.

Errors include `CONTENT_WORKFLOW_INVALID_TRANSITION`, `CONTENT_NOT_REVIEW_READY`, missing-revision codes, `CONTENT_REVIEW_COMMENT_REQUIRED/INVALID`, `CONTENT_PUBLISH_DEPENDENCY_INVALID`, `CONTENT_SLUG_LOCKED`, `CONTENT_WORKFLOW_CONFLICT`, and version/access errors. Published content must be unpublished before archive. Republish restores the approved revision and assigns a fresh server publication timestamp.

### Public content

| Method and path | Authentication | Query / headers | Success |
|---|---|---|---|
| `GET /api/public/content/{slug}` | Public | optional `If-None-Match` | `200 PublicContentDetailResponse` or `304` |
| `GET /api/public/content` | Public | `contentType,category,author,tag,page,size` | `200 PublicContentPageResponse` |

Only `PUBLISHED` posts with a valid current-published revision are visible; every other state returns 404 without revealing workflow state. Detail is sourced only from the immutable revision and contains `id,slug,contentType,title,excerpt,author,category,tags,cover,seo,document,publishedAt,revisionCreatedAt,media`. Public media exposes safe metadata plus stable HTTPS `deliveryUrl`, never bucket/key/preview URL/AWS fields. YouTube remains `provider + externalId`; the renderer constructs the iframe.

`document.schemaVersion` can be 1, 2, or 3 — the public API never rewrites a stored document, so the renderer must switch on every block `type` it supports rather than assuming the latest schema. For the three v3 additions the renderer owns all presentation; the backend supplies structure only:
- `CHECK_LIST` renders as a plain feature/verified list (e.g. a bulleted or icon-marked list) — there is no checked/unchecked state to render, it is not a to-do widget.
- `CALLOUT` renders as a distinct boxed/highlighted region whose appearance is chosen by the renderer from `variant` (`INFO|NOTE|VERDICT|WARNING`); `title` is shown only if present — the renderer must not invent a default title for a variant.
- `TABLE` renders as a semantic `<table>`: `columns[].label` becomes the header row, `rows[].rowType` distinguishes a `SECTION` row (a single full-width label spanning the table, e.g. `colspan` at the CSS layer) from `NORMAL` rows and an optional `TOTAL` row the renderer may style as a footer/summary. No column width, alignment, or color is supplied by the API.

List items contain `id,slug,contentType,title,excerpt,author,category,cover,publishedAt`; no document or tags. Filter values use normalized slugs. Ordering is `publishedAt DESC, postId DESC`. Success cache header is `public, max-age=0, s-maxage=60, stale-while-revalidate=60, stale-if-error=300`. ETag derives from revision ID plus publication timestamp; matching `If-None-Match` returns 304 before media resolution. Public read rate limit is 120 requests/minute per resolved primary identity.

## Permission-to-action matrix

| Action | Writer | Editor | Publisher | ADMIN |
|---|---:|---:|---:|---:|
| Create | yes | yes | no | yes |
| Edit own | yes | yes | no | yes |
| Edit any | no | yes | no | yes |
| Submit | own | any editable | no | yes |
| Review/request changes/approve | no | yes | no | yes |
| Publish/unpublish/archive | no | no | yes | yes |
| Upload media | yes | yes | no | yes |
| Manage authors/categories/tags/users | no | no | no | yes |

Backend authorization is authoritative; UI visibility is convenience only.

## Error-code reference

| HTTP | Codes |
|---:|---|
| 400 | `DASHBOARD_VALIDATION_FAILED`, `DASHBOARD_INVALID_JSON`, `DASHBOARD_INVALID_PARAMETER`, `DASHBOARD_INVALID_WORKFLOW`, `CONTENT_VALIDATION_ERROR`, `CONTENT_DOCUMENT_INVALID`, `CONTENT_DOCUMENT_SCHEMA_UNSUPPORTED`, `CONTENT_MEDIA_NOT_FOUND`, `CONTENT_MEDIA_TYPE_MISMATCH`, `CONTENT_REVIEW_COMMENT_REQUIRED`, `CONTENT_REVIEW_COMMENT_INVALID`, `CMS_MEDIA_INVALID_TYPE`, public `CONTENT_INVALID_REQUEST` |
| 401 | `DASHBOARD_AUTHENTICATION_REQUIRED`, `DASHBOARD_AUTHENTICATION_FAILED` |
| 403 | `DASHBOARD_ACCESS_DENIED` |
| 404 | `CONTENT_NOT_FOUND`, `CONTENT_REVISION_NOT_FOUND`, `CMS_METADATA_NOT_FOUND`, `CMS_MEDIA_NOT_FOUND`, dashboard resource/endpoint codes |
| 409 | `CONTENT_SLUG_CONFLICT`, `CONTENT_VERSION_CONFLICT`, `CONTENT_MEDIA_NOT_READY`, `CONTENT_NOT_EDITABLE`, `CONTENT_WORKFLOW_INVALID_TRANSITION`, `CONTENT_NOT_REVIEW_READY`, `CONTENT_REVIEW_REVISION_MISSING`, `CONTENT_APPROVED_REVISION_MISSING`, `CONTENT_PUBLISHED_REVISION_MISSING`, `CONTENT_PUBLISH_DEPENDENCY_INVALID`, `CONTENT_SLUG_LOCKED`, `CONTENT_WORKFLOW_CONFLICT`, `CMS_METADATA_CONFLICT`, `CMS_MEDIA_UPLOAD_INCOMPLETE`, `CMS_MEDIA_NOT_READY`, `DASHBOARD_DATA_CONFLICT` |
| 413 | `CONTENT_DOCUMENT_TOO_LARGE`, `CMS_MEDIA_TOO_LARGE` |
| 422 | `CMS_MEDIA_VALIDATION_FAILED` |
| 429 | rate-limit error envelope from the shared limiter |
| 503 | `CMS_MEDIA_STORAGE_UNAVAILABLE`, public `CONTENT_TEMPORARILY_UNAVAILABLE` |

No raw SQL, Hibernate, AWS SDK exception, stack trace, password hash, refresh token record, storage bucket, or storage key is part of an error or content response contract.

## OpenAPI decision

The project contains disabled `springdoc` configuration keys and Swagger security routes, but no Springdoc/OpenAPI dependency or CMS annotations are present in `pom.xml` or the application code. Phase 8 does not add a generator solely for certification: the polymorphic document examples, permission policy, autosave ordering, and workflow invariants need this human-readable contract regardless. OpenAPI can be added later without changing the certified API.
