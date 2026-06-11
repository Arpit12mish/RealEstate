-- =========================================================
-- V77__create_builder_improvement_tables.sql
-- Builder Improvement Journey / Commitment 2026 foundation
-- =========================================================

-- =========================================================
-- 1. Builder Improvement Profile
-- One profile can be builder-level OR project-specific.
-- project_id NULL     => builder-level profile
-- project_id NOT NULL => project-specific profile
-- =========================================================

CREATE TABLE IF NOT EXISTS builder_improvement_profile (
    id BIGSERIAL PRIMARY KEY,

    builder_id BIGINT NOT NULL,
    project_id BIGINT,

    badge_text VARCHAR(120) NOT NULL DEFAULT 'Commitment 2026',
    hero_title TEXT,
    hero_subtitle TEXT,
    hero_image_url TEXT,

    verified_by_text VARCHAR(180) DEFAULT 'Verified by SFS Trust Engine',
    summary TEXT,

    builder_response_title VARCHAR(180),
    builder_response_person_name VARCHAR(150),
    builder_response_person_designation VARCHAR(150),
    builder_response_text TEXT,

    overall_status VARCHAR(40) NOT NULL DEFAULT 'UNDER_REVIEW',
    evidence_status VARCHAR(40) NOT NULL DEFAULT 'EVIDENCE_PENDING',

    review_status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',

    active BOOLEAN NOT NULL DEFAULT TRUE,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    display_order INT NOT NULL DEFAULT 0,

    created_by_dashboard_user_id BIGINT,
    updated_by_dashboard_user_id BIGINT,
    submitted_by_dashboard_user_id BIGINT,
    reviewed_by_dashboard_user_id BIGINT,
    published_by_dashboard_user_id BIGINT,

    submitted_at TIMESTAMPTZ,
    reviewed_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    last_reviewed_at TIMESTAMPTZ,

    review_remarks TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_builder_improvement_profile_builder
        FOREIGN KEY (builder_id) REFERENCES builder(id),

    CONSTRAINT fk_builder_improvement_profile_project
        FOREIGN KEY (project_id) REFERENCES project(id),

    CONSTRAINT fk_builder_improvement_profile_created_by
        FOREIGN KEY (created_by_dashboard_user_id) REFERENCES dashboard_users(id) ON DELETE SET NULL,

    CONSTRAINT fk_builder_improvement_profile_updated_by
        FOREIGN KEY (updated_by_dashboard_user_id) REFERENCES dashboard_users(id) ON DELETE SET NULL,

    CONSTRAINT fk_builder_improvement_profile_submitted_by
        FOREIGN KEY (submitted_by_dashboard_user_id) REFERENCES dashboard_users(id) ON DELETE SET NULL,

    CONSTRAINT fk_builder_improvement_profile_reviewed_by
        FOREIGN KEY (reviewed_by_dashboard_user_id) REFERENCES dashboard_users(id) ON DELETE SET NULL,

    CONSTRAINT fk_builder_improvement_profile_published_by
        FOREIGN KEY (published_by_dashboard_user_id) REFERENCES dashboard_users(id) ON DELETE SET NULL,

    CONSTRAINT chk_builder_improvement_overall_status
        CHECK (overall_status IN (
            'STRONG_IMPROVEMENT',
            'IMPROVING',
            'STABLE',
            'UNDER_REVIEW',
            'PAUSED'
        )),

    CONSTRAINT chk_builder_improvement_evidence_status
        CHECK (evidence_status IN (
            'EVIDENCE_PENDING',
            'BUILDER_SUBMITTED',
            'SFS_REVIEWED',
            'SFS_VERIFIED',
            'PUBLIC_RECORD_VERIFIED',
            'NOT_APPLICABLE'
        )),

    CONSTRAINT chk_builder_improvement_review_status
        CHECK (review_status IN (
            'DRAFT',
            'PENDING_REVIEW',
            'RECHECK',
            'APPROVED',
            'REJECTED'
        ))
);

CREATE INDEX IF NOT EXISTS idx_builder_improvement_profile_builder
    ON builder_improvement_profile(builder_id);

CREATE INDEX IF NOT EXISTS idx_builder_improvement_profile_project
    ON builder_improvement_profile(project_id);

CREATE INDEX IF NOT EXISTS idx_builder_improvement_profile_public
    ON builder_improvement_profile(builder_id, project_id, published, active, deleted);

CREATE INDEX IF NOT EXISTS idx_builder_improvement_profile_review_status
    ON builder_improvement_profile(review_status);

CREATE UNIQUE INDEX IF NOT EXISTS ux_builder_improvement_profile_builder_level
    ON builder_improvement_profile(builder_id)
    WHERE project_id IS NULL AND deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_builder_improvement_profile_project_level
    ON builder_improvement_profile(builder_id, project_id)
    WHERE project_id IS NOT NULL AND deleted = FALSE;


-- =========================================================
-- 2. Concrete Actions Taken
-- Accordion cards:
-- Context -> Action -> Result
-- =========================================================

CREATE TABLE IF NOT EXISTS builder_improvement_action (
    id BIGSERIAL PRIMARY KEY,

    profile_id BIGINT NOT NULL,

    action_type VARCHAR(60) NOT NULL,
    title VARCHAR(180) NOT NULL,
    subtitle VARCHAR(255),

    context_text TEXT,
    action_text TEXT,
    result_text TEXT,

    icon_key VARCHAR(80),

    impact_level VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    evidence_status VARCHAR(40) NOT NULL DEFAULT 'EVIDENCE_PENDING',

    display_order INT NOT NULL DEFAULT 0,

    active BOOLEAN NOT NULL DEFAULT TRUE,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_builder_improvement_action_profile
        FOREIGN KEY (profile_id) REFERENCES builder_improvement_profile(id) ON DELETE CASCADE,

    CONSTRAINT chk_builder_improvement_action_type
        CHECK (action_type IN (
            'TIMELINE_OPTIMIZATION',
            'CUSTOMER_SUPPORT_HUB',
            'DOCUMENTATION_TRANSPARENCY',
            'PROJECT_MONITORING',
            'CONTRACTOR_CHANGED',
            'SITE_MANPOWER_INCREASED',
            'APPROVAL_CLEARED',
            'MONTHLY_BUYER_UPDATES',
            'PENDING_COMPLAINTS_REDUCED',
            'SITE_PROGRESS_TRACKING',
            'COMMUNITY_CONTRIBUTION',
            'OTHER'
        )),

    CONSTRAINT chk_builder_improvement_action_impact
        CHECK (impact_level IN ('LOW', 'MEDIUM', 'HIGH')),

    CONSTRAINT chk_builder_improvement_action_evidence
        CHECK (evidence_status IN (
            'EVIDENCE_PENDING',
            'BUILDER_SUBMITTED',
            'SFS_REVIEWED',
            'SFS_VERIFIED',
            'PUBLIC_RECORD_VERIFIED',
            'NOT_APPLICABLE'
        ))
);

CREATE INDEX IF NOT EXISTS idx_builder_improvement_action_profile
    ON builder_improvement_action(profile_id);

CREATE INDEX IF NOT EXISTS idx_builder_improvement_action_public_sort
    ON builder_improvement_action(profile_id, published, active, deleted, display_order, id);


-- =========================================================
-- 3. Issue Closure Momentum
-- Store issue records only.
-- Summary numbers will be calculated dynamically.
-- =========================================================

CREATE TABLE IF NOT EXISTS builder_improvement_issue (
    id BIGSERIAL PRIMARY KEY,

    profile_id BIGINT NOT NULL,

    issue_type VARCHAR(60) NOT NULL,
    title VARCHAR(180) NOT NULL,
    description TEXT,

    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',

    reported_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    target_resolution_date DATE,

    resolution_summary TEXT,

    evidence_status VARCHAR(40) NOT NULL DEFAULT 'EVIDENCE_PENDING',

    display_order INT NOT NULL DEFAULT 0,

    active BOOLEAN NOT NULL DEFAULT TRUE,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_builder_improvement_issue_profile
        FOREIGN KEY (profile_id) REFERENCES builder_improvement_profile(id) ON DELETE CASCADE,

    CONSTRAINT chk_builder_improvement_issue_type
        CHECK (issue_type IN (
            'BUYER_COMPLAINT',
            'HANDOVER_DEFECT',
            'DOCUMENTATION_GAP',
            'AMENITY_DELAY',
            'CONSTRUCTION_IMPACT',
            'MAINTENANCE_SUPPORT',
            'COMMUNICATION_DELAY',
            'OTHER'
        )),

    CONSTRAINT chk_builder_improvement_issue_status
        CHECK (status IN (
            'PENDING',
            'IN_PROGRESS',
            'RESOLVED',
            'REOPENED',
            'CANCELLED'
        )),

    CONSTRAINT chk_builder_improvement_issue_evidence
        CHECK (evidence_status IN (
            'EVIDENCE_PENDING',
            'BUILDER_SUBMITTED',
            'SFS_REVIEWED',
            'SFS_VERIFIED',
            'PUBLIC_RECORD_VERIFIED',
            'NOT_APPLICABLE'
        ))
);

CREATE INDEX IF NOT EXISTS idx_builder_improvement_issue_profile
    ON builder_improvement_issue(profile_id);

CREATE INDEX IF NOT EXISTS idx_builder_improvement_issue_status
    ON builder_improvement_issue(profile_id, status);

CREATE INDEX IF NOT EXISTS idx_builder_improvement_issue_public_sort
    ON builder_improvement_issue(profile_id, published, active, deleted, display_order, id);


-- =========================================================
-- 4. After-Sales Upgrades
-- Post-delivery support and responsibility cards.
-- =========================================================

CREATE TABLE IF NOT EXISTS builder_after_sales_upgrade (
    id BIGSERIAL PRIMARY KEY,

    profile_id BIGINT NOT NULL,

    title VARCHAR(180) NOT NULL,
    short_summary TEXT,

    legacy_issue_title VARCHAR(180),
    legacy_issue_description TEXT,

    solution_title VARCHAR(180),
    solution_description TEXT,

    result_title VARCHAR(180),
    result_description TEXT,

    metric_value VARCHAR(80),
    metric_label VARCHAR(120),

    implemented_at DATE,

    impact_level VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    evidence_status VARCHAR(40) NOT NULL DEFAULT 'EVIDENCE_PENDING',

    display_order INT NOT NULL DEFAULT 0,

    active BOOLEAN NOT NULL DEFAULT TRUE,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_builder_after_sales_upgrade_profile
        FOREIGN KEY (profile_id) REFERENCES builder_improvement_profile(id) ON DELETE CASCADE,

    CONSTRAINT chk_builder_after_sales_upgrade_impact
        CHECK (impact_level IN ('LOW', 'MEDIUM', 'HIGH')),

    CONSTRAINT chk_builder_after_sales_upgrade_evidence
        CHECK (evidence_status IN (
            'EVIDENCE_PENDING',
            'BUILDER_SUBMITTED',
            'SFS_REVIEWED',
            'SFS_VERIFIED',
            'PUBLIC_RECORD_VERIFIED',
            'NOT_APPLICABLE'
        ))
);

CREATE INDEX IF NOT EXISTS idx_builder_after_sales_upgrade_profile
    ON builder_after_sales_upgrade(profile_id);

CREATE INDEX IF NOT EXISTS idx_builder_after_sales_upgrade_public_sort
    ON builder_after_sales_upgrade(profile_id, published, active, deleted, display_order, id);


-- =========================================================
-- 5. Improvement Timeline
-- Month-wise journey:
-- Problem Observed -> Action Taken -> Current Status
-- =========================================================

CREATE TABLE IF NOT EXISTS builder_improvement_timeline (
    id BIGSERIAL PRIMARY KEY,

    profile_id BIGINT NOT NULL,

    timeline_type VARCHAR(60) NOT NULL DEFAULT 'OTHER',

    event_date DATE,
    event_month_label VARCHAR(40),

    title VARCHAR(180) NOT NULL,
    short_description TEXT,

    problem_observed TEXT,
    action_taken TEXT,
    current_status TEXT,

    evidence_status VARCHAR(40) NOT NULL DEFAULT 'EVIDENCE_PENDING',

    display_order INT NOT NULL DEFAULT 0,

    active BOOLEAN NOT NULL DEFAULT TRUE,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_builder_improvement_timeline_profile
        FOREIGN KEY (profile_id) REFERENCES builder_improvement_profile(id) ON DELETE CASCADE,

    CONSTRAINT chk_builder_improvement_timeline_type
        CHECK (timeline_type IN (
            'DELAY_OBSERVATION',
            'AUDIT_INITIATED',
            'TRANSPARENCY_PROTOCOL',
            'MILESTONE_VERIFICATION',
            'DELIVERY_CERTIFICATION',
            'CUSTOMER_SUPPORT_UPDATE',
            'DOCUMENTATION_UPDATE',
            'ISSUE_RESOLUTION',
            'OTHER'
        )),

    CONSTRAINT chk_builder_improvement_timeline_evidence
        CHECK (evidence_status IN (
            'EVIDENCE_PENDING',
            'BUILDER_SUBMITTED',
            'SFS_REVIEWED',
            'SFS_VERIFIED',
            'PUBLIC_RECORD_VERIFIED',
            'NOT_APPLICABLE'
        ))
);

CREATE INDEX IF NOT EXISTS idx_builder_improvement_timeline_profile
    ON builder_improvement_timeline(profile_id);

CREATE INDEX IF NOT EXISTS idx_builder_improvement_timeline_public_sort
    ON builder_improvement_timeline(profile_id, published, active, deleted, display_order, id);


-- =========================================================
-- 6. Content version key
-- Used for app/dashboard cache invalidation later.
-- =========================================================

INSERT INTO content_version(key, version)
VALUES ('BUILDER_IMPROVEMENT', 1)
ON CONFLICT (key) DO NOTHING;