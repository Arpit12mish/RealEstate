-- V89: Add authenticated user ownership columns to project_review
-- user_id: FK to users table (nullable — admin-created reviews have no user)
-- user_phone_hash: SHA-256 of the authenticated user's phone number at submission time
-- submitted_by_user: true only for mobile user-submitted reviews

ALTER TABLE project_review
    ADD COLUMN IF NOT EXISTS user_id          BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS user_phone_hash  VARCHAR(255),
    ADD COLUMN IF NOT EXISTS submitted_by_user BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_project_review_user_id
    ON project_review (user_id);

CREATE INDEX IF NOT EXISTS idx_project_review_user_project
    ON project_review (user_id, project_id);

CREATE INDEX IF NOT EXISTS idx_project_review_user_status
    ON project_review (user_id, verification_status);
