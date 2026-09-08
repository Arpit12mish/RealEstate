# CMS LAYOUT block — rendering contract

Frontend-independent contract for the `LAYOUT` ContentDocument block (schema v4). The dashboard
preview (`SfsContentPreviewRenderer`) already implements this; the public website has no
ContentDocument renderer yet (confirmed absent as of this doc) — implement against this contract
when one is built, so dashboard preview and the public site render the same JSON identically.

## Shape

```json
{
  "type": "LAYOUT",
  "columns": 3,
  "children": [
    { "type": "TABLE", "title": "Interior Packages", "caption": null, "columns": [...], "rows": [...] },
    { "type": "TABLE", "title": "Paint Budget", "caption": null, "columns": [...], "rows": [...] },
    { "type": "TABLE", "title": "Furniture Budget", "caption": null, "columns": [...], "rows": [...] }
  ]
}
```

```json
{
  "type": "LAYOUT",
  "columns": 2,
  "children": [
    { "type": "IMAGE", "mediaAssetId": 481, "decorative": false, "altText": "Express Zenith – Alpha Tower", "caption": [], "layout": "STANDARD", "link": null },
    { "type": "IMAGE", "mediaAssetId": 482, "decorative": false, "altText": "Skyline Innovation", "caption": [], "layout": "STANDARD", "link": null }
  ]
}
```

- `columns`: integer, 1–3. The desktop column count — never a raw CSS/pixel value.
- `children`: 1–12 items, each a normal `IMAGE` or `TABLE` block exactly as it appears
  top-level elsewhere in the document (same fields, same validation, same media resolution —
  see `CmsMediaReferenceService`). No `LAYOUT_IMAGE`/`LAYOUT_TABLE` wrapper types exist.
- No `gap`/spacing field. The renderer owns one consistent spacing value.
- `children.length` has no required relationship to `columns` — more children than columns
  wraps into additional rows automatically (CSS Grid `auto-flow: row` semantics). There is no
  separate "row" concept in the JSON.

## Responsive columns (required, not optional polish)

Never render `columns` fixed at all widths — a 3-table section must not stay 3 squeezed columns
on a 390px phone. Map `columns` to breakpoints, mobile-first, capping at 2 before the desktop
tier:

| `columns` | < 640px (mobile) | ≥ 640px (tablet) | ≥ 1024px (desktop) |
|---|---|---|---|
| 1 | 1 | 1 | 1 |
| 2 | 1 | 2 | 2 |
| 3 | 1 | 2 | 3 |

(Dashboard implementation: `LAYOUT_GRID_COLUMN_CLASSES` in `SfsContentPreviewRenderer.tsx` —
Tailwind's default `sm`/`lg` breakpoints, already used elsewhere in this codebase. A future
website implementation should reuse its own equivalent breakpoint tokens, not necessarily these
exact pixel values, but must preserve the mobile-always-1/desktop-full-columns behavior.)

Grid item wrapper needs `min-width: 0` (a CSS Grid default-`auto` min-width otherwise prevents a
wide TABLE child from shrinking below its natural width, breaking overflow handling below).

## TABLE children

- `title` (optional): short heading rendered **above** the table, bold/semibold. Distinct from
  the pre-existing `caption`, which stays a small annotation rendered **below** the table —
  never conflate the two or repurpose one for the other.
- The table itself stays a real semantic `<table>` with `<thead>`/`<tbody>`/`<th>`/`<td>` — never
  a div/card grid pretending to be a table. The surrounding grid arranges table *cards*; each
  card's content remains an accessible table.
- Overflow: wrap the `<table>` in its own `overflow-x-auto` container (already true of the
  existing top-level TABLE renderer — a LAYOUT child reuses it unmodified). Combined with the
  mobile 1-column collapse above, a table only needs its own horizontal scroll in the rare case
  it has many columns even at full single-column width — it must never be the *primary* strategy
  for fitting 3 tables into a narrow viewport.

## IMAGE children

Unchanged IMAGE semantics — `mediaAssetId`, `altText`, `caption`, `layout` (`STANDARD`/`WIDE`)
all mean exactly what they mean at the top level. `object-fit: cover` inside a fixed-aspect-ratio
box (the existing top-level IMAGE renderer's `aspect-video` treatment) is what a LAYOUT child
reuses unmodified — the layout controls placement/column width only, never the image's own
presentation attributes.

## Accessibility

- Every `alt` from `IMAGE.altText` (or empty string for `decorative: true`) is preserved exactly
  as the top-level renderer already does.
- The grid container has no interactive semantics of its own (no `role="grid"` — that role
  implies a 2D interactive widget model, e.g. a spreadsheet, which this is not). Plain `<div>`s
  with `display: grid` are correct; the *tables inside* keep their own real table semantics.

## What is explicitly NOT part of this contract

- No arbitrary CSS, `className`, `style`, or pixel values in the document JSON — only the
  semantic fields above.
- No column-span (`1/3 + 2/3` asymmetric widths) in v1 — every child in a row is an equal
  fraction of the row. Revisit only if a real design need appears; it is a genuinely additive
  schema change (`span` on each child) if/when added, not a breaking one.
- No nested LAYOUT — a LAYOUT's children can only be IMAGE/TABLE (enforced in the type system on
  both backend and dashboard, not just by convention).
