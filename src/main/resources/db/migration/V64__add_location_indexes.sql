CREATE INDEX IF NOT EXISTS idx_city_coordinates
ON city (latitude, longitude);

CREATE INDEX IF NOT EXISTS idx_project_city_public
ON project (city_id, published, active, deleted, priority, id);

CREATE INDEX IF NOT EXISTS idx_project_coordinates
ON project (latitude, longitude);