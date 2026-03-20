CREATE TABLE project_meter_snapshot (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL UNIQUE,
    construction_progress_percent INTEGER NOT NULL DEFAULT 0,
    delay_days INTEGER NOT NULL DEFAULT 0,
    construction_start_date DATE,
    expected_completion_date DATE,
    revised_completion_date DATE,
    compliance_score INTEGER,
    amenity_score INTEGER,
    location_score DOUBLE PRECISION,
    location_appreciation_percent_3y DOUBLE PRECISION,
    launch_price BIGINT,
    current_price BIGINT,
    average_area_price BIGINT,
    price_appreciation_percent DOUBLE PRECISION,
    estimated_cost_total BIGINT,
    computed_at TIMESTAMP WITH TIME ZONE,
    last_verified_at TIMESTAMP WITH TIME ZONE,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_meter_snapshot_project
        FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE TABLE project_construction_stage (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    stage_code VARCHAR(50) NOT NULL,
    stage_label VARCHAR(120) NOT NULL,
    display_order INTEGER NOT NULL,
    weight_percent INTEGER NOT NULL,
    progress_percent INTEGER NOT NULL DEFAULT 0,
    planned_start_date DATE,
    planned_end_date DATE,
    actual_start_date DATE,
    actual_end_date DATE,
    status VARCHAR(30) NOT NULL,
    remarks TEXT,
    evidence_count INTEGER NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_construction_stage_project
        FOREIGN KEY (project_id) REFERENCES project(id),
    CONSTRAINT uk_project_stage_code
        UNIQUE (project_id, stage_code)
);

CREATE INDEX idx_project_construction_stage_project_id
    ON project_construction_stage(project_id);

CREATE INDEX idx_project_meter_snapshot_project_id
    ON project_meter_snapshot(project_id);