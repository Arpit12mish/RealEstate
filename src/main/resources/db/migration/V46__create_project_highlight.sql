CREATE TABLE project_highlight (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    subtitle VARCHAR(260),
    icon_key VARCHAR(80),
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_project_highlight_project
        FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE INDEX idx_project_highlight_project
    ON project_highlight(project_id);

CREATE INDEX idx_project_highlight_project_active
    ON project_highlight(project_id, active, deleted, sort_order, id);