-- Room "Space Comparison" (this room vs. an authored average) — extracted
-- from the dirty worktree's own V138__floor_plan_room_comparison_and_
-- visual_analysis.sql, which bundled this room-dimension ALTER TABLE
-- together with the unrelated visual-analysis tables (stabilized
-- separately, see /Users/mac/Desktop/RealEstate-visual-analysis-contract).
-- Purely additive - no existing column is touched, no existing row's
-- meaning changes; a room with these columns all NULL is a legitimate
-- "no comparison authored yet" state, not a migration artifact to clean up.
--
-- comparisonLabel (backend-formatted, e.g. "+13% from avg") and
-- hasComparisonData (= average_area_sqft IS NOT NULL) are deliberately NOT
-- persisted columns - both are computed at read time in
-- ProjectFloorPlanRoomDimensionMapper, never stored, so there is nothing
-- to keep in sync if the formatting rule ever changes.

ALTER TABLE project_floor_plan_room_dimension
    ADD COLUMN IF NOT EXISTS average_area_sqft        DECIMAL(10, 2),
    ADD COLUMN IF NOT EXISTS comparison_context_label VARCHAR(120),
    ADD COLUMN IF NOT EXISTS difference_percent       DECIMAL(6, 2),
    ADD COLUMN IF NOT EXISTS comparison_summary       TEXT,
    ADD COLUMN IF NOT EXISTS comparison_verified      BOOLEAN NOT NULL DEFAULT FALSE;
