CREATE TABLE IF NOT EXISTS project_master_plan (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(id),

    title VARCHAR(150),
    subtitle VARCHAR(300),
    description TEXT,
    master_plan_image_url TEXT,
    image_caption VARCHAR(300),
    image_alt_text VARCHAR(300),

    total_units INTEGER,
    total_towers INTEGER,
    total_floors INTEGER,

    park_area_value DECIMAL(12, 2),
    park_area_unit VARCHAR(20),
    total_land_area_value DECIMAL(12, 2),
    total_land_area_unit VARCHAR(20),
    open_space_area_value DECIMAL(12, 2),
    open_space_area_unit VARCHAR(20),
    green_area_value DECIMAL(12, 2),
    green_area_unit VARCHAR(20),
    clubhouse_area_value DECIMAL(12, 2),
    clubhouse_area_unit VARCHAR(20),
    amenity_area_value DECIMAL(12, 2),
    amenity_area_unit VARCHAR(20),
    road_width_value DECIMAL(8, 2),
    road_width_unit VARCHAR(20),

    water_source VARCHAR(120),
    parking_type VARCHAR(30),
    total_parking_slots INTEGER,
    visitor_parking_slots INTEGER,
    basement_levels INTEGER,
    entry_exit_gates INTEGER,
    lift_count INTEGER,
    phase_count INTEGER,
    current_phase VARCHAR(80),
    open_space_percent DECIMAL(5, 2),
    green_coverage_percent DECIMAL(5, 2),

    vastu_compliant BOOLEAN,
    gated_community BOOLEAN,
    boundary_wall BOOLEAN,
    fire_tender_movement BOOLEAN,
    sewage_treatment_plant BOOLEAN,
    rainwater_harvesting BOOLEAN,
    power_backup BOOLEAN,

    approval_status VARCHAR(30),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    source_label VARCHAR(180),
    source_document_url VARCHAR(500),
    last_verified_at TIMESTAMPTZ,
    remarks TEXT,

    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_project_master_plan_area_unit
        CHECK (
            park_area_unit IS NULL OR park_area_unit IN ('SQ_FT', 'SQ_MT', 'ACRE', 'HECTARE')
        ),
    CONSTRAINT chk_project_master_plan_total_land_area_unit
        CHECK (
            total_land_area_unit IS NULL OR total_land_area_unit IN ('SQ_FT', 'SQ_MT', 'ACRE', 'HECTARE')
        ),
    CONSTRAINT chk_project_master_plan_open_space_area_unit
        CHECK (
            open_space_area_unit IS NULL OR open_space_area_unit IN ('SQ_FT', 'SQ_MT', 'ACRE', 'HECTARE')
        ),
    CONSTRAINT chk_project_master_plan_green_area_unit
        CHECK (
            green_area_unit IS NULL OR green_area_unit IN ('SQ_FT', 'SQ_MT', 'ACRE', 'HECTARE')
        ),
    CONSTRAINT chk_project_master_plan_clubhouse_area_unit
        CHECK (
            clubhouse_area_unit IS NULL OR clubhouse_area_unit IN ('SQ_FT', 'SQ_MT', 'ACRE', 'HECTARE')
        ),
    CONSTRAINT chk_project_master_plan_amenity_area_unit
        CHECK (
            amenity_area_unit IS NULL OR amenity_area_unit IN ('SQ_FT', 'SQ_MT', 'ACRE', 'HECTARE')
        ),
    CONSTRAINT chk_project_master_plan_road_width_unit
        CHECK (
            road_width_unit IS NULL OR road_width_unit IN ('SQ_FT', 'SQ_MT', 'ACRE', 'HECTARE')
        ),
    CONSTRAINT chk_project_master_plan_parking_type
        CHECK (
            parking_type IS NULL OR parking_type IN ('OPEN', 'COVERED', 'BASEMENT', 'STILT', 'MECHANICAL', 'MIXED', 'NOT_DISCLOSED')
        ),
    CONSTRAINT chk_project_master_plan_approval_status
        CHECK (
            approval_status IS NULL OR approval_status IN ('DRAFT', 'SUBMITTED', 'VERIFIED', 'NEEDS_REVIEW', 'NOT_AVAILABLE')
        ),
    CONSTRAINT chk_project_master_plan_non_negative_counts
        CHECK (
            (total_units IS NULL OR total_units >= 0)
            AND (total_towers IS NULL OR total_towers >= 0)
            AND (total_floors IS NULL OR total_floors >= 0)
            AND (total_parking_slots IS NULL OR total_parking_slots >= 0)
            AND (visitor_parking_slots IS NULL OR visitor_parking_slots >= 0)
            AND (basement_levels IS NULL OR basement_levels >= 0)
            AND (entry_exit_gates IS NULL OR entry_exit_gates >= 0)
            AND (lift_count IS NULL OR lift_count >= 0)
            AND (phase_count IS NULL OR phase_count >= 0)
        ),
    CONSTRAINT chk_project_master_plan_non_negative_areas
        CHECK (
            (park_area_value IS NULL OR park_area_value >= 0)
            AND (total_land_area_value IS NULL OR total_land_area_value >= 0)
            AND (open_space_area_value IS NULL OR open_space_area_value >= 0)
            AND (green_area_value IS NULL OR green_area_value >= 0)
            AND (clubhouse_area_value IS NULL OR clubhouse_area_value >= 0)
            AND (amenity_area_value IS NULL OR amenity_area_value >= 0)
            AND (road_width_value IS NULL OR road_width_value >= 0)
        ),
    CONSTRAINT chk_project_master_plan_percentages
        CHECK (
            (open_space_percent IS NULL OR (open_space_percent >= 0 AND open_space_percent <= 100))
            AND (green_coverage_percent IS NULL OR (green_coverage_percent >= 0 AND green_coverage_percent <= 100))
        )
);

CREATE INDEX IF NOT EXISTS idx_project_master_plan_project_id
    ON project_master_plan(project_id);

CREATE INDEX IF NOT EXISTS idx_project_master_plan_public_lookup
    ON project_master_plan(project_id, active, deleted);

CREATE UNIQUE INDEX IF NOT EXISTS uq_project_master_plan_project_not_deleted
    ON project_master_plan(project_id)
    WHERE deleted = FALSE;

INSERT INTO dashboard_field_help
    (module, field_key, field_label, short_help, detailed_help, why_needed, source_hint, example_value, validation_hint, display_order)
VALUES
('PROJECT_MASTER_PLAN', 'masterPlanImageUrl', 'Master Plan Image', 'Site layout or approved master plan image.', 'Upload the project site layout, tower placement, open-space map, or approved master layout image.', 'This is the primary visual for the mobile Master Plan card.', 'Use approved layout drawings, builder disclosure material, or verified brochure assets.', 'https://cdn.example.com/projects/101/master-plan.webp', 'Use MASTER_PLAN_IMAGE upload. Supported: jpg, png, webp.', 1),
('PROJECT_MASTER_PLAN', 'totalUnits', 'Total Units', 'Total number of units in the project.', 'Represents the complete unit count across all towers/phases when available.', 'Helps buyers understand project scale and density.', 'Use RERA registration, approved plan, or builder disclosure.', '1520', 'Enter a non-negative number.', 2),
('PROJECT_MASTER_PLAN', 'totalTowers', 'Total Towers', 'Total number of towers or blocks.', 'Count all residential/commercial towers shown in the master layout.', 'Helps the mobile card summarize project structure.', 'Use approved layout plan or builder disclosure.', '18', 'Enter a non-negative number.', 3),
('PROJECT_MASTER_PLAN', 'totalFloors', 'Total Floors', 'Typical or maximum floor count.', 'Use the floor count represented by the master plan or approved tower layout.', 'Helps users quickly understand vertical scale.', 'Use approval drawings, RERA details, or builder disclosure.', '19', 'Enter a non-negative number.', 4),
('PROJECT_MASTER_PLAN', 'parkAreaValue', 'Park Area', 'Dedicated park or landscaped park area.', 'Area reserved for park/open landscaped use, paired with an area unit.', 'Shown as a key stat below the master plan image.', 'Use approved layout, landscape plan, or brochure.', '2.70', 'Enter a non-negative value and select a unit.', 5),
('PROJECT_MASTER_PLAN', 'totalLandAreaValue', 'Total Land Area', 'Overall project land parcel area.', 'Total land area of the project site, paired with an area unit.', 'Supports buyer understanding of project scale.', 'Use title/RERA records, approved layout, or builder disclosure.', '12.5', 'Enter a non-negative value and select a unit.', 6),
('PROJECT_MASTER_PLAN', 'openSpaceAreaValue', 'Open Space Area', 'Total open-space area.', 'Area allocated to open spaces, circulation, parks, and non-built areas when disclosed.', 'Useful for assessing density and livability.', 'Use approved site plan or builder disclosure.', '4.2', 'Enter a non-negative value and select a unit.', 7),
('PROJECT_MASTER_PLAN', 'greenAreaValue', 'Green Area', 'Green or landscaped area.', 'Area allocated to green cover, landscape, and garden areas.', 'Helps explain visible greenery and open-space claims.', 'Use landscape plan or builder disclosure.', '3.1', 'Enter a non-negative value and select a unit.', 8),
('PROJECT_MASTER_PLAN', 'waterSource', 'Water Source', 'Primary water source for the project.', 'Stores authority or source name such as municipal supply, borewell, BWSSB, tanker, recycled/STP mix.', 'Shown as a key mobile stat when available.', 'Use builder disclosure, utility approval, or resident handover documents.', 'BWSSB', 'Keep under 120 characters.', 9),
('PROJECT_MASTER_PLAN', 'parkingType', 'Parking Type', 'Parking arrangement type.', 'Select the closest parking type such as open, covered, basement, stilt, mechanical, or mixed.', 'Helps buyers understand parking planning from the site layout.', 'Use approved parking plan or builder disclosure.', 'BASEMENT', 'Select one of the supported values.', 10),
('PROJECT_MASTER_PLAN', 'openSpacePercent', 'Open Space %', 'Open space percentage of the project.', 'Percentage of the project reserved for open spaces when disclosed or verified.', 'Useful for comparing density and livability.', 'Use approved plan or verified calculation.', '65', 'Enter 0 to 100.', 11),
('PROJECT_MASTER_PLAN', 'greenCoveragePercent', 'Green Coverage %', 'Green coverage percentage.', 'Percentage of project area covered by landscaping or green spaces.', 'Adds trust to landscape/open-space claims.', 'Use landscape plan or verified calculation.', '35', 'Enter 0 to 100.', 12),
('PROJECT_MASTER_PLAN', 'sourceLabel', 'Source Label', 'Public source label for this data.', 'Short label describing the source shown to users, such as Builder Disclosure or Approved Layout.', 'Improves transparency on where the data came from.', 'Use the most accurate source name.', 'Builder Disclosure', 'Keep under 180 characters.', 13),
('PROJECT_MASTER_PLAN', 'verified', 'Verified', 'Marks whether the master plan data is verified.', 'Verified means the image/stats have been checked against a trusted source.', 'Improves trust in the Master Plan section.', 'Reviewer should mark verified only after checking source documents.', 'true', 'Only mark verified after review.', 14)
ON CONFLICT (module, field_key) DO UPDATE SET
    field_label = EXCLUDED.field_label,
    short_help = EXCLUDED.short_help,
    detailed_help = EXCLUDED.detailed_help,
    why_needed = EXCLUDED.why_needed,
    source_hint = EXCLUDED.source_hint,
    example_value = EXCLUDED.example_value,
    validation_hint = EXCLUDED.validation_hint,
    display_order = EXCLUDED.display_order,
    active = TRUE;
