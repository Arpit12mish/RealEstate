ALTER TABLE content_review_activity DROP CONSTRAINT chk_content_review_activity_action;
ALTER TABLE content_review_activity
    ADD CONSTRAINT chk_content_review_activity_action CHECK (action IN (
        'SUBMITTED_FOR_REVIEW', 'CHANGES_REQUESTED', 'APPROVED',
        'PUBLISHED', 'UNPUBLISHED', 'ARCHIVED', 'REOPENED'
    ));
