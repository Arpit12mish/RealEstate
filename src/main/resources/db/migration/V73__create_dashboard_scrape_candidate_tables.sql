-- V73: Scrape Candidate persistence tables.
-- Stores RERA scrape results for review before applying to project/builder drafts.
-- Nothing in these tables is public-facing. Apply flow goes through normal review workflow.

CREATE TABLE dashboard_scrape_candidate (
    id                              BIGSERIAL PRIMARY KEY,
    source_code                     VARCHAR(50)  NOT NULL,
    rera_number                     VARCHAR(150) NOT NULL,
    found                           BOOLEAN      NOT NULL DEFAULT FALSE,
    captcha_detected                BOOLEAN      NOT NULL DEFAULT FALSE,
    source_search_url               TEXT,
    source_detail_url               TEXT,
    final_url                       TEXT,
    page_title                      VARCHAR(300),
    raw_html_path                   TEXT,
    screenshot_path                 TEXT,
    confidence_score                INT,
    confidence_status               VARCHAR(50),
    total_expected_fields           INT,
    found_fields                    INT,
    missing_fields                  INT,
    status                          VARCHAR(50)  NOT NULL,
    linked_builder_id               BIGINT,
    linked_project_id               BIGINT,
    applied_project_id              BIGINT,
    created_by_dashboard_user_id    BIGINT,
    applied_by_dashboard_user_id    BIGINT,
    applied_at                      TIMESTAMPTZ,
    remarks                         TEXT,
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scrape_candidate_rera_number    ON dashboard_scrape_candidate (rera_number);
CREATE INDEX idx_scrape_candidate_source_code    ON dashboard_scrape_candidate (source_code);
CREATE INDEX idx_scrape_candidate_status         ON dashboard_scrape_candidate (status);
CREATE INDEX idx_scrape_candidate_linked_builder ON dashboard_scrape_candidate (linked_builder_id);
CREATE INDEX idx_scrape_candidate_linked_project ON dashboard_scrape_candidate (linked_project_id);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE dashboard_scrape_candidate_project (
    id                  BIGSERIAL PRIMARY KEY,
    candidate_id        BIGINT       NOT NULL REFERENCES dashboard_scrape_candidate (id) ON DELETE CASCADE,
    name                VARCHAR(255),
    description         TEXT,
    city_name           VARCHAR(150),
    address_line        TEXT,
    latitude            NUMERIC(10, 7),
    longitude           NUMERIC(10, 7),
    price_min           BIGINT,
    price_max           BIGINT,
    possession_date     DATE,
    rera_number         VARCHAR(150),
    project_status      VARCHAR(80),
    property_types      TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scrape_cand_project_cid ON dashboard_scrape_candidate_project (candidate_id);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE dashboard_scrape_candidate_builder (
    id                  BIGSERIAL PRIMARY KEY,
    candidate_id        BIGINT       NOT NULL REFERENCES dashboard_scrape_candidate (id) ON DELETE CASCADE,
    name                VARCHAR(255),
    phone               VARCHAR(80),
    email               VARCHAR(180),
    address_line        TEXT,
    city_name           VARCHAR(150),
    website             TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scrape_cand_builder_cid ON dashboard_scrape_candidate_builder (candidate_id);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE dashboard_scrape_candidate_compliance_item (
    id                  BIGSERIAL PRIMARY KEY,
    candidate_id        BIGINT       NOT NULL REFERENCES dashboard_scrape_candidate (id) ON DELETE CASCADE,
    item_group          VARCHAR(80),
    item_key            VARCHAR(120),
    item_label          VARCHAR(180),
    status              VARCHAR(80),
    value_text          TEXT,
    document_url        TEXT,
    remarks             TEXT,
    display_order       INT          NOT NULL DEFAULT 0,
    verified            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scrape_cand_compliance_cid ON dashboard_scrape_candidate_compliance_item (candidate_id);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE dashboard_scrape_candidate_field_result (
    id                  BIGSERIAL PRIMARY KEY,
    candidate_id        BIGINT       NOT NULL REFERENCES dashboard_scrape_candidate (id) ON DELETE CASCADE,
    section             VARCHAR(100),
    field_key           VARCHAR(150),
    field_label         VARCHAR(200),
    found               BOOLEAN      NOT NULL DEFAULT FALSE,
    value_text          TEXT,
    source_label        VARCHAR(255),
    confidence          INT,
    reason              TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scrape_cand_field_result_cid ON dashboard_scrape_candidate_field_result (candidate_id);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE dashboard_scrape_candidate_raw_value (
    id                  BIGSERIAL PRIMARY KEY,
    candidate_id        BIGINT       NOT NULL REFERENCES dashboard_scrape_candidate (id) ON DELETE CASCADE,
    raw_key             TEXT         NOT NULL,
    raw_value           TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scrape_cand_raw_value_cid ON dashboard_scrape_candidate_raw_value (candidate_id);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE dashboard_scrape_candidate_cost_breakdown (
    id                  BIGSERIAL PRIMARY KEY,
    candidate_id        BIGINT       NOT NULL REFERENCES dashboard_scrape_candidate (id) ON DELETE CASCADE,
    land_cost           BIGINT,
    construction_cost   BIGINT,
    infrastructure_cost BIGINT,
    other_cost          BIGINT,
    total_cost          BIGINT,
    source_label        VARCHAR(255),
    remarks             TEXT,
    verified            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (candidate_id)
);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE dashboard_scrape_candidate_land_utilization (
    id                      BIGSERIAL PRIMARY KEY,
    candidate_id            BIGINT        NOT NULL REFERENCES dashboard_scrape_candidate (id) ON DELETE CASCADE,
    total_land_area_sqm     NUMERIC(14, 2),
    residential_area_sqm    NUMERIC(14, 2),
    commercial_area_sqm     NUMERIC(14, 2),
    parks_area_sqm          NUMERIC(14, 2),
    open_area_sqm           NUMERIC(14, 2),
    parking_area_sqm        NUMERIC(14, 2),
    utility_area_sqm        NUMERIC(14, 2),
    source_label            VARCHAR(255),
    remarks                 TEXT,
    verified                BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (candidate_id)
);

-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE dashboard_scrape_candidate_document (
    id                  BIGSERIAL PRIMARY KEY,
    candidate_id        BIGINT       NOT NULL REFERENCES dashboard_scrape_candidate (id) ON DELETE CASCADE,
    document_type       VARCHAR(80)  NOT NULL,
    title               VARCHAR(255),
    document_url        TEXT,
    source_label        VARCHAR(255),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scrape_cand_document_cid ON dashboard_scrape_candidate_document (candidate_id);
