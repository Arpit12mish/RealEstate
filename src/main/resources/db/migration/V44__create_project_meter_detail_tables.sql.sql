CREATE TABLE project_compliance_item (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    item_group VARCHAR(40) NOT NULL,
    item_key VARCHAR(80) NOT NULL,
    item_label VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    value_text VARCHAR(255),
    document_url TEXT,
    remarks TEXT,
    display_order INTEGER NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_compliance_item_project
        FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE INDEX idx_project_compliance_project_id
    ON project_compliance_item(project_id);

CREATE TABLE project_price_history (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    year_label VARCHAR(20) NOT NULL,
    project_price BIGINT NOT NULL,
    average_area_price BIGINT,
    display_order INTEGER NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_price_history_project
        FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE INDEX idx_project_price_history_project_id
    ON project_price_history(project_id);

CREATE TABLE project_payment_milestone (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    milestone_code VARCHAR(80) NOT NULL,
    milestone_label VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    percentage_value INTEGER NOT NULL,
    display_order INTEGER NOT NULL,
    linked_stage_code VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_payment_milestone_project
        FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE INDEX idx_project_payment_milestone_project_id
    ON project_payment_milestone(project_id);

CREATE TABLE project_cost_breakdown (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL UNIQUE,
    land_cost BIGINT,
    construction_cost BIGINT,
    infrastructure_cost BIGINT,
    other_cost BIGINT,
    total_cost BIGINT,
    source_label VARCHAR(80),
    remarks TEXT,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_cost_breakdown_project
        FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE TABLE project_land_utilization (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL UNIQUE,
    total_land_area_sqm DOUBLE PRECISION,
    commercial_area_sqm DOUBLE PRECISION,
    parks_area_sqm DOUBLE PRECISION,
    open_area_sqm DOUBLE PRECISION,
    residential_area_sqm DOUBLE PRECISION,
    parking_area_sqm DOUBLE PRECISION,
    utility_area_sqm DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_land_utilization_project
        FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE TABLE project_location_score (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL UNIQUE,
    metro_score DOUBLE PRECISION,
    education_score DOUBLE PRECISION,
    healthcare_score DOUBLE PRECISION,
    retail_score DOUBLE PRECISION,
    job_score DOUBLE PRECISION,
    leisure_score DOUBLE PRECISION,
    current_strength_score DOUBLE PRECISION,
    future_growth_score DOUBLE PRECISION,
    final_score DOUBLE PRECISION,
    appreciation_percent_3y DOUBLE PRECISION,
    score_summary TEXT,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_location_score_project
        FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE TABLE project_amenity_progress (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    amenity_code VARCHAR(80) NOT NULL,
    amenity_label VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    progress_percent INTEGER NOT NULL DEFAULT 0,
    weight_percent INTEGER NOT NULL DEFAULT 0,
    display_order INTEGER NOT NULL,
    remarks TEXT,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_amenity_progress_project
        FOREIGN KEY (project_id) REFERENCES project(id)
);

CREATE INDEX idx_project_amenity_progress_project_id
    ON project_amenity_progress(project_id);