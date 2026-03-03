ALTER TABLE home_project_analytics
ADD COLUMN IF NOT EXISTS builder_id BIGINT NULL;

CREATE INDEX IF NOT EXISTS idx_home_project_analytics_cat_builder
ON home_project_analytics (category_id, builder_id);

-- Optional but useful:
-- If you want to support "soft delete" like your loader:
ALTER TABLE home_project_analytics
ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- If you want "priority" ordering (your loader uses priority):
ALTER TABLE home_project_analytics
ADD COLUMN IF NOT EXISTS priority INTEGER NOT NULL DEFAULT 1;

-- If you want caption like your DTO:
ALTER TABLE home_project_analytics
ADD COLUMN IF NOT EXISTS caption VARCHAR(255) NULL;
