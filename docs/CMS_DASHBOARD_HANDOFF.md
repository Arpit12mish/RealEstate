# CMS Dashboard Handoff

This is the recommended client integration for the certified CMS API (certified against migration V155; migration head is now V156, additive-only — see `CMS_API_CONTRACT.md`). See `CMS_API_CONTRACT.md` for every field and error. The dashboard's D5 workflow/revision/publishing UI and the D6 admin management pages (`/dashboard/cms/{authors,categories,tags,media,content-team}`) are now implemented against this contract.

## Authentication

1. `POST /api/dashboard/auth/login` with email/password.
2. Keep one access token, one refresh token, and the returned user permission set in the auth store.
3. Send `Authorization: Bearer <accessToken>` on every `/api/dashboard/**` request except login/refresh/logout.
4. Before access expiry (`expiresInMs`), or after one 401 caused by expiry, call refresh once. Refresh rotates both tokens; atomically replace the old pair. Serialize refresh attempts so parallel requests do not reuse an already-rotated token.
5. `GET /api/dashboard/auth/me` restores identity and current permissions after app reload. Treat `active:false` or a 401 as signed out.
6. Logout posts the refresh token, clears local credentials, and returns an empty 200.

Do not decode JWT claims as the permission source; use `user.permissions`. Account disable, permission updates, and password resets invalidate the server identity cache after commit. Refresh is immediately blocked after disable/reset; an access token can otherwise live until its short expiry, subject to the 15-second identity-cache bound.

## Permissions to UI controls

| Authority | Suggested UI |
|---|---|
| `CMS_CONTENT_CREATE` | New Article/Blog/Interview |
| `CMS_CONTENT_EDIT_OWN` | Edit own eligible draft |
| `CMS_CONTENT_EDIT_ANY` | Edit team drafts |
| `CMS_CONTENT_SUBMIT_REVIEW` | Submit / Resubmit |
| `CMS_CONTENT_REVIEW` | Review, Request changes, Approve |
| `CMS_CONTENT_PREVIEW` | CMS content list/detail/revision |
| `CMS_CONTENT_PUBLISH` | Publish / Republish |
| `CMS_CONTENT_UNPUBLISH` | Unpublish |
| `CMS_CONTENT_ARCHIVE` | Archive |
| `CMS_MEDIA_UPLOAD` | Upload image/video |
| `ROLE_ADMIN` | Account and author/category/tag administration |

Hiding a control is not security. Always handle 403 because backend policy is authoritative.

## Editor bootstrap and traffic

At application bootstrap:

- Login or refresh, then cache `/auth/me` for the session.
- Load static client-side document schema support (schema 2); no backend call is needed.

On content-list screen:

- `GET /api/dashboard/cms/content?page=0&size=20&sortBy=updatedAt&sortDirection=desc`.
- Request subsequent pages on demand. Never request an unbounded list.

On “new content” screen:

- Load active authors/categories/tags with paginated `search` queries.
- Load READY media only when the picker opens.
- `POST /api/dashboard/cms/content` once; server returns the canonical ID, slug, status, and version.

On editor screen load:

- `GET /api/dashboard/cms/content/{id}` for metadata/workflow/version.
- `GET /api/dashboard/cms/content/{id}/document` for body, word count, and one bulk preview media map.
- Do not fetch every media ID separately when the document response already includes it.

Search boxes should debounce 250–400 ms, cancel superseded requests, and use `search`, `page=0`, and a modest `size` (20–50).

## Create and update metadata

Create example:

```json
{
  "contentType": "ARTICLE",
  "title": "Best Places to Buy Property in Gurgaon",
  "excerpt": "A practical market guide.",
  "robotsIndex": true,
  "robotsFollow": true
}
```

The server generates a unique slug when omitted and derives owner/actors/status/version. A draft may be incomplete. A full metadata save uses the current version and IDs, not nested objects:

```json
{
  "version": 7,
  "contentType": "ARTICLE",
  "title": "Best Places to Buy Property in Gurgaon",
  "slug": "best-places-to-buy-property-in-gurgaon",
  "excerpt": "A practical market guide.",
  "seoTitle": null,
  "seoDescription": null,
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

After first publication, treat slug as read-only until redirect history exists.

## Rich document

GET response:

```json
{
  "contentId": 42,
  "version": 8,
  "updatedAt": "2026-08-19T16:00:00+05:30",
  "document": {
    "schemaVersion": 5,
    "blocks": [
      {"type":"PARAGRAPH","content":[{"type":"TEXT","text":"Hello world","marks":[{"type":"BOLD"}]}]}
    ]
  },
  "wordCount": 2,
  "media": {}
}
```

PUT request:

```json
{
  "version": 8,
  "document": {
    "schemaVersion": 5,
    "blocks": [
      {"type":"HEADING","level":"H2","content":[{"type":"TEXT","text":"Overview","marks":[]}]},
      {"type":"IMAGE","mediaAssetId":481,"decorative":false,"altText":"Apartment clubhouse","caption":[],"layout":"WIDE"},
      {"type":"VIDEO","mediaAssetId":512,"posterMediaAssetId":481,"caption":[]},
      {"type":"EMBED","provider":"YOUTUBE","externalId":"dQw4w9WgXcQ","caption":[]}
    ]
  }
}
```

The editor adapter must translate frontend-editor state into these semantic nodes and back. Never send HTML or editor-library internals. Render CMS previews with an explicit block switch, escaped text, derived link attributes, canonical YouTube IDs, and trusted media preview data. Never pass content text to `dangerouslySetInnerHTML`.

### Schema version 5 (current write version)

Documents stored with `schemaVersion` 1, 2, 3, or 4 remain fully readable — GET returns the raw stored document as-is. Any PUT (whatever `schemaVersion` the request body declares) is re-validated, canonicalized, and re-saved as `schemaVersion` 5. There is no bulk/eager rewrite of old rows; upcast only happens the next time a document is saved.

**This is not purely informational** — the validator gates each schema generation's new block types behind the *declared* `schemaVersion` of the PUT body, not the server's own current version. Concretely: a `LAYOUT` block or a `TABLE.title` in the request needs `schemaVersion: 4`, and an `IMAGE_GALLERY` needs `schemaVersion: 5` — each is rejected with `400 CONTENT_DOCUMENT_SCHEMA_UNSUPPORTED` if the request declares an older version, even though 5 is already the server's current version. So the dashboard's own "current schema version" constant must track the block vocabulary its editor actually authors, not just mirror whatever this doc last said — bumping this doc's number without also bumping that constant (and vice versa) reproduces exactly this failure the moment an editor uses the new block. Short of that pitfall, the dashboard editor doesn't need to think further about versioning: it always reads whatever `schemaVersion` GET returns and always sends new writes with the current block vocabulary below.

v3 adds three blocks on top of the v2 set (`PARAGRAPH`, `HEADING`, `BULLET_LIST`, `ORDERED_LIST`, `BLOCKQUOTE`, `DIVIDER`, `IMAGE`, `VIDEO`, `EMBED`): `CHECK_LIST`, `CALLOUT`, `TABLE`.

**`CHECK_LIST`** — an editorial "verified/features" list (construction specs, nearby landmarks, etc.), *not* an interactive to-do list — items carry no checked/unchecked state. Shape is identical to `BULLET_LIST`/`ORDERED_LIST`:

```json
{"type":"CHECK_LIST","items":[
  {"content":[{"type":"TEXT","text":"RERA registered","marks":[]}]},
  {"content":[{"type":"TEXT","text":"Vastu compliant","marks":[]}]}
]}
```

**`CALLOUT`** — a controlled editorial box (SFS Verdict, Editor's Note, etc.). `variant` is one of `INFO`, `NOTE`, `VERDICT`, `WARNING` and maps to a renderer-owned appearance — no color/CSS is ever persisted. `title` is optional and never defaulted server-side (e.g. the "SFS Verdict" label for a `VERDICT` callout is a dashboard/renderer presentation concern, max 120 characters if provided).

```json
{"type":"CALLOUT","variant":"VERDICT","title":"SFS Verdict","content":[
  {"type":"TEXT","text":"Strong pick for end-users, priced fairly for the micro-market.","marks":[]}
]}
```

**`TABLE`** — a structured semantic table, not an arbitrary HTML table: no colspan/rowspan numbers, no per-cell styling. `caption` is optional (max 200 chars). `columns` is 1–8 entries, each `{"label": "..."}` (max 60 chars) — the header row **is** the column labels, there is no separate header-row concept. `rows` is 1–100 entries; each row has a `rowType` of `NORMAL`, `SECTION`, or `TOTAL` and a `cells` array of inline-node arrays (max 20 inline nodes/cell). `NORMAL`/`TOTAL` rows must have exactly as many cells as there are columns; a `SECTION` row is a single full-width label cell (semantically "spans the table") and must have exactly one cell. At least one cell in the whole table must contain visible (non-blank) text.

```json
{"type":"TABLE","caption":"Price comparison","columns":[
  {"label":"Project"},{"label":"Price/sqft"},{"label":"Possession"}
],"rows":[
  {"rowType":"SECTION","cells":[[{"type":"TEXT","text":"Sector 62","marks":[]}]]},
  {"rowType":"NORMAL","cells":[
    [{"type":"TEXT","text":"Skyline Heights","marks":[]}],
    [{"type":"TEXT","text":"₹8,200","marks":[]}],
    [{"type":"TEXT","text":"Ready","marks":[]}]
  ]},
  {"rowType":"TOTAL","cells":[
    [{"type":"TEXT","text":"Average","marks":[]}],
    [{"type":"TEXT","text":"₹8,200","marks":[]}],
    [{"type":"TEXT","text":"—","marks":[]}]
  ]}
]}
```

Cells reuse `InlineNode` directly — the same `TEXT`/`HARD_BREAK` nodes and `BOLD`/`ITALIC`/`UNDERLINE`/`LINK` marks used everywhere else. No block content (images, nested tables, embeds) is representable inside a cell.

None of these three blocks introduce media references, so they never appear in the `media` map.

### Schema version 4 additions: `TABLE.title` and `LAYOUT`

v4 adds one field on top of v3's `TABLE` and one new block, `LAYOUT`. Both require the request to declare `schemaVersion: 4` (see the callout above) — sending either one with `schemaVersion: 3` fails with `400 CONTENT_DOCUMENT_SCHEMA_UNSUPPORTED`.

**`TABLE.title`** — an optional short heading (max 120 chars) rendered *above* the table, distinct from the pre-existing `caption` which stays a small annotation rendered *below*. `null`/absent on any table authored before this field existed; never defaulted server-side.

```json
{"type":"TABLE","title":"Interior Packages","caption":null,"columns":[
  {"label":"Package"},{"label":"Price/sqft"}
],"rows":[
  {"rowType":"NORMAL","cells":[[{"type":"TEXT","text":"Essential","marks":[]}],[{"type":"TEXT","text":"₹1,200","marks":[]}]]}
]}
```

**`LAYOUT`** — a responsive multi-column grid grouping existing `IMAGE`/`TABLE` blocks (no wrapper types; each child is validated exactly as strictly as a standalone top-level block of the same type, and each `IMAGE` child's `mediaAssetId` appears in the `media` map same as any top-level `IMAGE`). `columns` is the desktop column count, 1–3; `children` is 1–12 items and auto-wraps into additional rows once it exceeds `columns` (CSS Grid semantics — there is no separate "row" concept). No nested `LAYOUT`, and no `gap`/spacing field (the renderer owns one consistent spacing value).

```json
{"type":"LAYOUT","columns":2,"children":[
  {"type":"IMAGE","mediaAssetId":481,"decorative":false,"altText":"Express Zenith – Alpha Tower","caption":[],"layout":"STANDARD","link":null},
  {"type":"IMAGE","mediaAssetId":482,"decorative":false,"altText":"Skyline Innovation","caption":[],"layout":"STANDARD","link":null}
]}
```

Full rendering contract (responsive column→breakpoint mapping, table-vs-card semantics, accessibility) is in `docs/CMS_LAYOUT_RENDERING_CONTRACT.md` — read it before building the editor UI or preview renderer for this block, not just this summary. As of that doc, the **public website has no `LAYOUT` renderer yet**: a published post containing one will not display correctly there until one is built.

### Schema version 5 addition: `IMAGE_GALLERY`

v5 adds one new block, `IMAGE_GALLERY`. Requires the request to declare `schemaVersion: 5` (see the callout above) — sending it with `schemaVersion: 4` fails with `400 CONTENT_DOCUMENT_SCHEMA_UNSUPPORTED`.

**`IMAGE_GALLERY`** — an editorial photo gallery, deliberately its own block rather than a `LAYOUT` of `IMAGE` children: a gallery is one cohesive tile set with lightbox/carousel semantics that `LAYOUT`'s generic grid doesn't provide. `columns` is an optional 1–4 desktop column hint — omit it to let the renderer choose a sensible default (e.g. 3). `images` is 1–40 entries, each `{mediaAssetId,decorative,altText,caption}` — the exact same accessibility shape as a top-level `IMAGE` (decorative/alt-text mutual exclusivity, same caption limits), validated exactly as strictly as a standalone `IMAGE` would be. There is no `layout`/`link` per tile — a gallery tile isn't independently placed or linked. Every `images[].mediaAssetId` appears in the `media` map the same way any top-level `IMAGE`'s does.

```json
{"type":"IMAGE_GALLERY","columns":3,"images":[
  {"mediaAssetId":601,"decorative":false,"altText":"Clubhouse exterior","caption":[]},
  {"mediaAssetId":602,"decorative":false,"altText":"Rooftop pool","caption":[{"type":"TEXT","text":"Infinity pool, level 40","marks":[]}]}
]}
```

As with `LAYOUT`, the **public website has no `IMAGE_GALLERY` renderer yet** (confirmed absent as of this doc): a published post containing one will not display correctly there until one is built.

> **PRODUCTION-READINESS BLOCKER**: Do not publish migrated gallery posts in production until the website renderer safely renders `IMAGE_GALLERY` and the dashboard can at minimum read/display existing gallery blocks without corrupting or deleting them. This is a hard dependency for the WordPress migration's production import checklist, not just a cosmetic gap.

## Autosave and concurrency

Metadata and body share one version. Keep a single authoritative `latestVersion` per open post.

Recommended queue:

1. Debounce local changes.
2. Serialize metadata/document writes through one per-post promise queue.
3. Send the queue's `latestVersion`.
4. On success, replace it with response `version` and acknowledge only the saved slice.
5. If another slice became dirty while saving, enqueue it with the new version.
6. On `409 CONTENT_VERSION_CONFLICT`, stop automatic writes, fetch metadata + document, and ask the user to reload/reconcile. Do not blindly retry stale data.

Example: metadata 10→11 means a queued body request carrying 10 must be rebuilt with 11. Parallel PUTs with version 10 intentionally conflict. This protects metadata and body as one aggregate.

Do not autosave while status is `IN_REVIEW`, `APPROVED`, `PUBLISHED`, `UNPUBLISHED`, or `ARCHIVED`. `CHANGES_REQUESTED` is editable.

## Media upload

Initiate:

```json
{"mediaType":"IMAGE","filename":"clubhouse.jpg","contentType":"image/jpeg","sizeBytes":2481920}
```

Response:

```json
{
  "mediaAssetId": 481,
  "status": "PENDING_UPLOAD",
  "uploadUrl": "https://private-bucket-presigned-put.example/...",
  "expiresInSeconds": 300,
  "assetExpiresAt": "2026-08-19T16:05:00+05:30",
  "requiredHeaders": {
    "Content-Type": "image/jpeg",
    "Cache-Control": "public, max-age=31536000, immutable",
    "If-None-Match": "*"
  }
}
```

Sequence:

1. Validate file size/type client-side for fast feedback, but expect server enforcement.
2. Initiate upload.
3. PUT the raw file directly to `uploadUrl`, exactly preserving all required headers.
4. `POST /api/dashboard/cms/media/{mediaAssetId}/complete`.
5. Use the asset only after response status is `READY`.

Completion may return incomplete/validation/storage errors. It is safe to retry READY completion. Never log upload URLs, persist them in documents, or convert a user URL into a media block. Documents reference `mediaAssetId` only.

## Workflow

Submit:

```json
{"version":12}
```

`POST /content/{id}/workflow/submit` creates an immutable revision and returns `IN_REVIEW`. Missing author/category/cover/alt/meaningful document, inactive taxonomy, or non-READY media returns `CONTENT_NOT_REVIEW_READY` or a media dependency code.

Editor sequence:

1. GET revisions list.
2. GET the `currentReviewRevisionId` detail and review that exact snapshot.
3. Request changes with `{"version":13,"comment":"Please add source attribution."}` or approve with `{"version":13}`.

Writer resubmission creates a new revision; the previous one never changes. Publisher sends `{"version":15}` to publish. The backend publishes `approvedRevisionId`, never current mutable fields. Unpublish removes public visibility but preserves history; publish from `UNPUBLISHED` restores the same approved revision with a new publication timestamp/ETag. Published content must be unpublished before archive.

## Public response and media

Public detail is anonymous and revision-backed:

```json
{
  "id": 42,
  "slug": "best-places-to-buy-property-in-gurgaon",
  "contentType": "ARTICLE",
  "title": "Best Places to Buy Property in Gurgaon",
  "excerpt": "A practical market guide.",
  "author": {"id":3,"displayName":"Arpit Mishra","slug":"arpit-mishra","designation":"Senior Analyst","profileMediaAssetId":null},
  "category": {"id":5,"name":"Market Insights","slug":"market-insights"},
  "tags": [{"id":2,"name":"Gurgaon","slug":"gurgaon"}],
  "cover": {"mediaAssetId":481,"altText":"Gurgaon residential skyline","deliveryUrl":"https://media.example.com/cms/images/...jpg","contentType":"image/jpeg","width":1920,"height":1080},
  "seo": {"title":null,"description":null,"canonicalUrl":null,"robotsIndex":true,"robotsFollow":true},
  "document": {"schemaVersion":2,"blocks":[]},
  "publishedAt": "2026-08-19T16:00:00+05:30",
  "revisionCreatedAt": "2026-08-19T15:30:00+05:30",
  "media": {}
}
```

The website applies SEO fallback `seo.title ?? title` and `seo.description ?? excerpt`; it derives a host-specific canonical URL when canonical is null. Stable public media URLs require the manually provisioned CloudFront/OAC infrastructure described in `CMS_MEDIA_CLOUDFRONT_DELIVERY.md`.

## Error handling

- 400: show field errors or structured-document diagnostics.
- 401: attempt one serialized refresh; otherwise sign out.
- 403: refresh `/me`, hide/disable the action, retain unsaved local content.
- 404: content/resource disappeared or is not publicly visible.
- 409 version: stop autosave and reconcile.
- Other 409 workflow/dependency: keep editor state and display the server message.
- 413: block save/upload and show size guidance.
- 422 media: mark upload failed and require a new asset.
- 429: honor retry timing/back off; do not retry in a loop.
- 503: transient media/public-integrity problem; retry with bounded backoff.

Persist no plaintext password, refresh token in logs, presigned URL, dashboard preview URL, bucket, or storage key. Include server `requestId` in support reports.

## API freeze

The editor-facing MVP contract is complete. Add scheduled publishing, redirects, RSS/sitemap, author pages, analytics, transcoding, and similar post-MVP features through additive endpoints/fields; do not overload these command DTOs or expose status mutation.
