-- Ensure existing rows are safe
UPDATE business
SET avg_rating = 0
WHERE avg_rating IS NULL;

-- Add default
ALTER TABLE business
ALTER COLUMN avg_rating SET DEFAULT 0;

-- Enforce not-null safely
ALTER TABLE business
ALTER COLUMN avg_rating SET NOT NULL;
