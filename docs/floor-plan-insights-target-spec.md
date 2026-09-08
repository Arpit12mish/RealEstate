# Floor Plan Insights — Target UX Specification

This documents the desired end state for the "Floor Plans Insights" bottom sheet, opened by the **View Unit Insights** CTA on a floor-plan card. It is a redesign of the existing `FloorPlanInsightsBottomSheet.tsx` in `Mobile_SFS/SFS_App` (see [`floor-plan-insights-mobile-audit.md`](./floor-plan-insights-mobile-audit.md) §5) — every "current" reference below points at that component.

## 0. Entry point (unchanged)

- Rendered from `components/projects/FloorPlanCard.tsx`, the existing "View Unit Insights" black-pill CTA on each floor-plan card.
- Opens over the Project Details screen, background dimmed, content scrollable.
- No change needed to the entry point or its props (`visible`, `projectId`, `floorPlan`, `onClose`).

## 1. Header

- Title: **"Floor Plans Insights"** (already correct in the current component's `SHEET_TITLE`).
- Close button, top-right, circular outline with an "X" icon (current implementation already matches this).
- Dimmed backdrop, tap-to-dismiss (current implementation already matches this).
- Scrollable content body below a fixed header.

**Recommendation:** migrate the sheet container from a plain RN `Modal` to `@gorhom/bottom-sheet` (pattern: `ScoreExplanationBottomSheet.tsx`), for native snap-point/gesture behavior matching the reference screenshots' rounded-corner, draggable-handle polish. Not strictly required — a plain `Modal` can be styled to look identical — but it's the app's established idiom for this exact sheet shape and gets backdrop/gesture handling for free.

## 2. Section 1 — Visual Analysis

| Element | Spec |
|---|---|
| Title | "Visual analysis of every important factor" |
| Description | "Visualize how natural light, ventilation, and Vastu influence your space before making a decision." |
| Media | One large media area. Must support `IMAGE`, `VIDEO`, or `LOTTIE_JSON`, driven by a backend `mediaType`/`mediaUrl` pair — replaces the current hardcoded local video asset. |
| Tags | Row of chips below the media: "Sun Lighting", "Air ventilation", "Vastu analysis" (extensible list, not a fixed 3). Each tag has a small colored dot + label. |

Implementation notes:
- Media: extend `components/media/SmartMedia.tsx` (already handles image/video by URL) with a Lottie branch, or branch explicitly on `mediaType` in the sheet component before delegating.
- Tags: replace the current plain-text `analysisFactors` bullet list with chip components carrying `label`, `color` (hex, dashboard-authored per tag), and `sortOrder`. This directly replaces the "Ligthing & Ventilation / Layout & Movement / Vastu analysis / Tower analysis / Privacy, Amenities & More" bullet list currently rendered under `analysisHeading`.
- The three named tags in the mockup map loosely onto existing `FloorPlanInsightType` values (`NATURAL_LIGHT`, `NATURAL_VENTILATION`/`CROSS_VENTILATION`, `VASTU_COMPLIANCE`) but should **not** be modeled by reusing that enum — see the API contract plan for why a dedicated tag model is cleaner.

## 3. Section 2 — Space Comparison

| Element | Spec |
|---|---|
| Title | "Space Comparison" |
| Room chips | Horizontal scroll: "Master Bedroom", "Dining Room", "Kitchen", "Child Bedroom", + any other rooms present on this floor plan. Selected = black background/white text. Unselected = white background, outlined, black text. Exactly the same interaction pattern as the existing config-pill selector in `FloorPlanCard.tsx`. |
| Dimension label | `DIMENSIONS · 12ft * 14ft` — small caps/muted label + the room's `dimensionText`. |
| Size | Large number + unit, e.g. `168 sq ft`. |
| Comparison badge | Pill badge, e.g. `+13% from avg`, colored (green for above-average size in the reference; sign/color should be driven by whether the difference is positive or negative, not hardcoded green). |
| Summary | One sentence, e.g. "An average master bedroom in Varthur measures 149 sq ft. This unit's master bedroom is 13% bigger." |

Implementation notes:
- **This requires room selection state that does not exist today.** The current `RoomCard` components are static (no `onPress`). Add `selectedRoomKey` state to the sheet; tapping a chip updates it; the detail block below re-renders from the room matching that key.
- Each room chip needs its *own* dimension/area/average/%diff/summary bundle, not a single shared `insights[0]` as today — this is the room↔comparison linkage gap identified in the backend audit (§5) and addressed in the API contract plan.
- Rooms without authored comparison data yet (e.g. Dining Room, Kitchen in the reference screenshots — dimension shown, but no average/badge/summary) must render gracefully: dimension + size always shown if present, badge/summary block omitted (not "N/A" placeholders) when the backend hasn't supplied a comparison for that room. This lets dashboard users roll out comparisons room-by-room without blocking the whole feature.
- Replace the current bar-chart comparison (`comparisonBlock`/"This Bedroom" vs "Average" bars) with the badge+summary treatment — visually different, and per-room rather than per-`insights[0]`.

## 4. Future sections (not in this release — backend must not block on these)

Listed for context only, so the data model doesn't paint itself into a corner:

- Lighting & ventilation (detail view, beyond the Visual Analysis tag)
- Layout & movement
- Vastu analysis (detail view)
- Tower analysis
- Privacy / amenities
- Sunlight/windflow overlay visual (a placeholder for this already exists, commented out, in the current component — `LightingWindflowPlaceholder`, unused)

These map onto `FloorPlanInsightType` values that already exist (`FLOW_EFFICIENCY`, `VASTU_COMPLIANCE`, `PRIVACY_SCORE`) or would need new ones (`TOWER_ANALYSIS`). The current generic insight-benchmark model (`insights[]`, unchanged) remains the right home for these when built — they are not room-keyed comparisons, they're floor-plan-level factor scores, which is exactly what `ProjectFloorPlanInsightEntity` already models well.

## 5. Demo / real-data handling

- Keep the existing client-side fallback mechanism (`ENABLE_STATIC_UNIT_INSIGHTS_DEMO`, per [`floor-plan-unit-insights-demo-plan.md`](./floor-plan-unit-insights-demo-plan.md)) as the safety net for floor plans with no authored data yet — but source the "is this real or demo" signal from the backend (`demo: boolean` field on the response, see API contract plan) rather than only client-side heuristics, so the "sample content" disclaimer is accurate per-floor-plan instead of an unconditional client-side string.
- Do not hardcode the specific numbers from the screenshots (168 sqft / 149 sqft / 13%) as permanent production copy — those are today's demo dataset values, coincidentally identical to the mockup because the mockup was generated from this same demo data. Production must be able to show different, dashboard-authored numbers per project without a code change.

## 6. Non-goals for this pass

- No change to the entry-point CTA, config-pill grouping, or floor-plan card layout — all already correct.
- No removal of the existing `rooms[]`/`insights[]` flat lists from the API response — additive extension only (see API contract plan).
- No dashboard UI work is scoped here (no dashboard frontend package exists in this workspace per `floor-plan-current-state.md`); this spec covers the mobile-facing contract and UI only. Dashboard authoring UI for the new fields is a separate, follow-on scope.
