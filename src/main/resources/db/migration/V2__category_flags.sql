-- Add priority & active flags to category
ALTER TABLE category
    ADD COLUMN priority INT NOT NULL DEFAULT 100,
    ADD COLUMN active   BOOLEAN NOT NULL DEFAULT TRUE;

-- Helpful index to order children fast
CREATE INDEX idx_category_parent_priority
    ON category (parent_id, priority);
