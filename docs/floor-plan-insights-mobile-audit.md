# Floor Plan Insights — Mobile Audit

Scope: which mobile app is active, where the "View Unit Insights" flow lives today, and how it compares to the target bottom sheet in the screenshots. Read-only audit — no mobile files were modified.

**This supersedes the mobile section of [`floor-plan-current-state.md`](./floor-plan-current-state.md) §7**, which was written before the "View Unit Insights" CTA and `FloorPlanInsightsBottomSheet` existed in `Mobile_SFS/SFS_App`. That doc's claim of "No 'View Unit Insights' CTA" for that app is now out of date.

## 1. Which mobile app is active

Two repos exist, both named `sfs-rn` / "Square Foot Story" (`com.squarefootstory.app`) — same product, different checkouts:

| | `/Users/mac/Desktop/SFS_App` | `/Users/mac/Desktop/Mobile_SFS/SFS_App` |
|---|---|---|
| Commit count | 105 | 181 |
| Last commit | 2026-05-17 (PR #17) | 2026-07-15 (PR #38) |
| Floor-plan-insights code | **None found** | Full implementation exists |
| Verdict | Stale, ~2 months behind | **Active — target this repo** |

Exhaustive case-insensitive grep for `"view unit insights"`, `"unit insights"`, `"FloorPlanInsight"`, `"space comparison"`, `"visual analysis"` across `/Users/mac/Desktop/SFS_App` returned **zero matches**. Its `FloorPlansSection` (`app/projects/[projectId].tsx:427-564`) renders config pills + cards but the card ends at the price footer — no CTA button of any kind exists. This app also both has significant uncommitted local changes and both repos have unrelated uncommitted work in progress — none of it touched by this audit.

**Recommendation:** all future implementation work targets `Mobile_SFS/SFS_App`. If `SFS_App` (Desktop) is still meant to ship separately, it would need the entire feature backported — flag this to the user before any implementation phase begins, since it isn't a small parity gap.

## 2–4. Floor Plans section, CTA, and current behavior (in `Mobile_SFS/SFS_App`)

- Screen: `app/projects/[projectId].tsx`, project detail.
- Section component: `components/projects/FloorPlanCard.tsx` — config pills (`floorPlanGroups[].groupLabel`, black-when-active/white-when-inactive exactly like the target's room-chip styling), horizontal scroll of floor-plan cards, each with image, area stats, and a **"View Unit Insights" button that already exists** (`FloorPlanCard.tsx:131-136`), styled as a full-width black pill CTA.
- Tapping it sets local state `insightFloorPlan` (the tapped `ProjectFloorPlanDto`) and renders `<FloorPlanInsightsBottomSheet visible floorPlan projectId onClose />` (`FloorPlanCard.tsx:181-186`).
- The sheet is **already wired to the real endpoint**: `fetchProjectFloorPlanInsights(projectId, floorPlanId)` → `GET /api/projects/{projectId}/floor-plans/{floorPlanId}/insights` (`services/projectFloorPlanService.ts:58-69`).

## 5. Existing bottom sheet implementation vs. the target design

`components/projects/FloorPlanInsightsBottomSheet.tsx` (666 lines) is a **working prior iteration of the same feature**, not a stub. It is built with a plain React Native `Modal` (not `@gorhom/bottom-sheet`, even though that library is available and used elsewhere in the app — e.g. `ScoreExplanationBottomSheet.tsx`). Current structure, top to bottom:

1. Header: title "Floor Plans Insights", close button. Matches the target.
2. A horizontal row of **room cards** (`RoomCard`) — icon on top, label + `dimensionText` below, rendered in small white cards. **These are static display only** — no `onPress`, no selected/unselected state, no black/white toggle. Tapping a room card does nothing today.
3. A single `primaryInsight` (`content.insights[0]` — always the *first* insight in the array, not the one matching the selected room) rendered as a title + summary paragraph.
4. A horizontal **bar-chart comparison** ("This Bedroom" vs. "Average", colored bars sized proportionally) — this is a different visual treatment than the target's "+13% from avg" pill badge + sentence.
5. An `analysisHeading` ("Analyse every floor plan 30+ factors like") followed by a **bullet list of factor strings** (`analysisFactors`) — plain text rows, not the colored-dot tag chips in the target screenshots.
6. A single **hardcoded local video asset** (`require("@/assests_new/floor insights/insights-video-2.mp4")`) — always the same bundled video regardless of which floor plan or project is open; not driven by any backend media URL.
7. A disclaimer: *"Note: This section displays sample content for demonstration purposes. Real data will be available soon."*

**Net assessment:** this is functionally a v0/prototype of the same idea the new screenshots redesign — same entry point, same title, same underlying data source, but a different visual language (bar chart vs. badge, bullet list vs. colored chips, no room-selection interactivity, one fixed video vs. per-tag media). This should be treated as an **in-place redesign of an existing component**, not a greenfield build.

## 6. Demo/fallback data mechanism (already implemented per prior plan)

Matches the strategy already documented in [`floor-plan-unit-insights-demo-plan.md`](./floor-plan-unit-insights-demo-plan.md), executed as:

- `features/floorPlanInsights/demoFloorPlanInsights.ts` exports `ENABLE_STATIC_UNIT_INSIGHTS_DEMO = true` (hardcoded on, no env/remote-config gating) and a static `DEMO_FLOOR_PLAN_INSIGHTS` object whose room list (`Master Bedroom` 12ft×14ft/168sqft, `Dinning Room`, `Kitchen`, `Child Bedroom`) and comparison bar (168 vs. 149, "13% bigger") are **the exact numbers shown in the product's target screenshots** — i.e., the screenshots the user is designing against are literally this demo dataset rendered through the current (pre-redesign) sheet UI.
- Load priority in `FloorPlanInsightsBottomSheet.tsx:64-120`: if `floorPlan.insightsAvailable` is true and both IDs resolve, attempt the real fetch; on success with non-empty `rooms`/`insights`, render real data; on any failure, empty result, or `insightsAvailable=false`, fall back to the demo object (when the flag is on).
- Given the backend audit found no seeded production insight data path and the flag defaults on, this sheet is almost certainly rendering demo data for effectively all real floor plans today, with the visible "sample content" disclaimer as the honesty mechanism — consistent with the "safer options" the prior plan doc recommended.

## 7. Static vs. real data — verdict

Both. The Floor Plans **section** (cards, pills, areas, price) is fully live-API-driven with no hardcoding. The Floor Plans **Insights bottom sheet** attempts live data first but is effectively demo-driven in practice today, by design, with disclosure — not a hidden hack.

## 8. Video / Lottie / image support

All three primitives are already dependencies and in active use elsewhere in the app:

- `expo-image` — used pervasively.
- `expo-video` (`VideoView`/`useVideoPlayer`) — used by the current insights sheet itself (hardcoded asset) and elsewhere (e.g. `app/(home)/builder.tsx`).
- `lottie-react-native` — used elsewhere (e.g. the "meter band" Lottie in `[projectId].tsx:1284-1291`) but **not currently wired into the insights sheet**.
- `components/media/SmartMedia.tsx` — a reusable component that auto-detects image vs. video by URL and renders the right primitive behind one prop surface. This is a strong existing fit for a backend-driven `mediaType`/`mediaUrl` field; it would need a Lottie branch added (or a sibling `LottieView` render when `mediaType === "LOTTIE_JSON"`) to fully cover the three media types the target spec requires.

## 9. Responsive/layout conventions

House style is `react-native-size-matters` (`scale`, `verticalScale`, `moderateScale`) + `react-native-responsive-fontsize` (`RFValue`), used throughout `FloorPlanCard.tsx` and `FloorPlanInsightsBottomSheet.tsx` already — continue this convention rather than introducing `useWindowDimensions`-based sizing (used only sparingly elsewhere in the app).

## 10. Reusable components for the redesign

- `components/media/SmartMedia.tsx` — Visual Analysis media slot (image/video), extend for Lottie.
- `components/GestureFriendlyCarousel.tsx` — reanimated-carousel wrapper tuned to behave correctly inside a scrollable container (recently touched, per project memory); better fit than the current sheet's plain horizontal `ScrollView` for the room-chip strip.
- `components/projects/ScoreExplanationBottomSheet.tsx` — if migrating off plain `Modal` onto `@gorhom/bottom-sheet` (recommended for a smoother sheet gesture/snap experience matching the reference screenshots' visual polish), this is the existing pattern to follow: `visible`/`onClose` controlled props, `BottomSheetBackdrop`, `snapPoints`, safe-area bottom inset.
- No existing chip/pill or comparison-badge component is factored out anywhere in the app; the config pills in `FloorPlanCard.tsx` (`styles.pill`/`pillActive`) are the closest visual reference and should be extracted into a shared chip component the new room-selector can also use, since the redesign needs the identical black-selected/white-unselected interaction pattern twice (config pills, then room chips).

## 11. Files that will need changes (for a future implementation phase — not touched now)

Modify in place:
- `components/projects/FloorPlanInsightsBottomSheet.tsx` — redesign into the two-section (Visual Analysis / Space Comparison) layout; add room-selection state; replace the bar chart with the badge+summary pattern; replace the hardcoded video with backend-driven media; replace the factor bullet list with colored tag chips.
- `features/floorPlanInsights/demoFloorPlanInsights.ts` — extend the demo shape to match whatever new response contract is adopted (see [`floor-plan-insights-api-contract-plan.md`](./floor-plan-insights-api-contract-plan.md)) so demo and real data keep rendering through the same component.
- `types/project.ts` — extend `FloorPlanInsightRoomDto` / `ProjectFloorPlanInsightDetailDto` (or add new types) for the new `visualAnalysis`/`spaceComparison` fields; remove or backend-implement the dead `insightsUrl` field.
- `services/projectFloorPlanService.ts` — no signature change needed if the existing endpoint is extended additively.

New, if the redesign needs it:
- A small shared `Chip`/`Pill` component under `components/` if the team wants to de-duplicate the config-pill and room-chip styling.

No changes needed in `app/projects/[projectId].tsx` or `FloorPlanCard.tsx` beyond what's already wired — the CTA and invocation are already correct.
