CREATE TABLE project_connectivity_place (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    connectivity_id BIGINT,
    place_name VARCHAR(180) NOT NULL,
    place_type VARCHAR(60) NOT NULL,
    distance_label VARCHAR(80),
    image_url TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_project_connectivity_place_project
        FOREIGN KEY (project_id) REFERENCES project(id),

    CONSTRAINT fk_project_connectivity_place_connectivity
        FOREIGN KEY (connectivity_id) REFERENCES project_connectivity(id)
);

CREATE INDEX idx_project_connectivity_place_project_active
    ON project_connectivity_place(project_id, active, deleted, sort_order, id);