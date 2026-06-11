ALTER TABLE project
    DROP CONSTRAINT IF EXISTS project_status_check;

ALTER TABLE project
    ADD CONSTRAINT project_status_check
    CHECK (
        status IS NULL
        OR status IN ('UPCOMING', 'UNDER_CONSTRUCTION', 'READY_TO_MOVE', 'COMPLETED')
    );
