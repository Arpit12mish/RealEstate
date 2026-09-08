-- Insights tab (dashboard): room-size insight types (MASTER_BEDROOM_SIZE, etc.)
-- need a free-form dimension string alongside the existing numeric benchmark
-- fields, matching project_floor_plan_room_dimension.dimension_text.

ALTER TABLE project_floor_plan_insight
    ADD COLUMN IF NOT EXISTS dimension_text VARCHAR(100);
