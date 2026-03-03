-- Vxx__home_section_item_add_config_id.sql

ALTER TABLE home_section_item
ADD COLUMN IF NOT EXISTS config_id bigint;

-- FK
ALTER TABLE home_section_item
ADD CONSTRAINT fk_home_section_item_config
FOREIGN KEY (config_id) REFERENCES home_section_config(id);

-- Optional but recommended index
CREATE INDEX IF NOT EXISTS idx_home_section_item_config_sort
ON home_section_item (config_id, sort_order, id);

-- Backfill for existing data (important)
-- If your existing items are unique per (home_category_id, section_type),
-- this will attach all those items to the *first* matching config row.
UPDATE home_section_item i
SET config_id = c.id
FROM home_section_config c
WHERE i.config_id IS NULL
  AND i.home_category_id = c.home_category_id
  AND i.section_type = c.section_type;

-- After you verify everything, you can enforce NOT NULL:
-- ALTER TABLE home_section_item ALTER COLUMN config_id SET NOT NULL;
