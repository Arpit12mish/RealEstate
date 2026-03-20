CREATE TABLE project_floor_plan (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    floor_code VARCHAR(80),
    image_url TEXT NOT NULL,
    carpet_area VARCHAR(100),
    exclusive_area VARCHAR(100),
    super_area VARCHAR(100),
    unit_label VARCHAR(120),
    description TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_floor_plan_project
        FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE INDEX idx_project_floor_plan_project_id
    ON project_floor_plan(project_id);