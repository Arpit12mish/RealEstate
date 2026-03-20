CREATE TABLE project_connectivity (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL UNIQUE,
    title VARCHAR(160),
    subtitle VARCHAR(260),
    map_image_url TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_project_connectivity_project
        FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE INDEX idx_project_connectivity_project_active
    ON project_connectivity(project_id, active, deleted);