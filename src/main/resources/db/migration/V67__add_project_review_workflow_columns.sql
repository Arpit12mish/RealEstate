-- =========================================================
-- V68__add_project_review_workflow_columns.sql
-- Adds project-level dashboard review workflow columns
-- =========================================================

ALTER TABLE project
    ADD COLUMN review_status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN created_by_dashboard_user_id BIGINT,
    ADD COLUMN submitted_by_dashboard_user_id BIGINT,
    ADD COLUMN submitted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN reviewed_by_dashboard_user_id BIGINT,
    ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN review_remarks TEXT;

ALTER TABLE project
    ADD CONSTRAINT chk_project_review_status
        CHECK (review_status IN ('DRAFT', 'PENDING_REVIEW', 'RECHECK', 'APPROVED', 'REJECTED'));

ALTER TABLE project
    ADD CONSTRAINT fk_project_created_by_dashboard_user
        FOREIGN KEY (created_by_dashboard_user_id)
        REFERENCES dashboard_users (id)
        ON DELETE SET NULL;

ALTER TABLE project
    ADD CONSTRAINT fk_project_submitted_by_dashboard_user
        FOREIGN KEY (submitted_by_dashboard_user_id)
        REFERENCES dashboard_users (id)
        ON DELETE SET NULL;

ALTER TABLE project
    ADD CONSTRAINT fk_project_reviewed_by_dashboard_user
        FOREIGN KEY (reviewed_by_dashboard_user_id)
        REFERENCES dashboard_users (id)
        ON DELETE SET NULL;

CREATE INDEX idx_project_review_status
    ON project (review_status);

CREATE INDEX idx_project_created_by_dashboard_user_id
    ON project (created_by_dashboard_user_id);

CREATE INDEX idx_project_submitted_by_dashboard_user_id
    ON project (submitted_by_dashboard_user_id);

CREATE INDEX idx_project_reviewed_by_dashboard_user_id
    ON project (reviewed_by_dashboard_user_id);