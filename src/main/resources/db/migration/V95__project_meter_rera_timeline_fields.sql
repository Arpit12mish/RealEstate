ALTER TABLE project_meter_snapshot
    ADD COLUMN IF NOT EXISTS original_completion_date DATE,
    ADD COLUMN IF NOT EXISTS latest_rera_completion_date DATE,
    ADD COLUMN IF NOT EXISTS actual_completion_date DATE,
    ADD COLUMN IF NOT EXISTS rera_extension_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS delay_vs_original_days INTEGER,
    ADD COLUMN IF NOT EXISTS delay_vs_latest_rera_days INTEGER,
    ADD COLUMN IF NOT EXISTS timeline_status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS timeline_label TEXT,
    ADD COLUMN IF NOT EXISTS timeline_hint TEXT;

UPDATE project_meter_snapshot
SET original_completion_date = COALESCE(original_completion_date, expected_completion_date),
    latest_rera_completion_date = COALESCE(latest_rera_completion_date, revised_completion_date, expected_completion_date),
    rera_extension_count = COALESCE(rera_extension_count, 0);
