-- Index 1: by business + created_at (most common analytics pattern)
CREATE INDEX IF NOT EXISTS idx_business_event_business_created_at
    ON business_event (business_id, created_at DESC);

-- Index 2: by event_type + created_at (for global stats by type)
CREATE INDEX IF NOT EXISTS idx_business_event_type_created_at
    ON business_event (event_type, created_at DESC);

-- Index 3: by city + category + created_at (for city/category analytics)
CREATE INDEX IF NOT EXISTS idx_business_event_city_cat_created_at
    ON business_event (city_id, category_id, created_at DESC);
