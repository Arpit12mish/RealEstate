-- Allow reviewers/admins to record APPROVED -> PENDING_REVIEW rollback events.

ALTER TABLE dashboard_content_review_history
    DROP CONSTRAINT IF EXISTS chk_dashboard_review_history_action_type;

ALTER TABLE dashboard_content_review_history
    ADD CONSTRAINT chk_dashboard_review_history_action_type
        CHECK (
            action_type IN (
                'CREATED',
                'UPDATED',
                'SUBMITTED_FOR_REVIEW',
                'FIELD_MARKED_RECHECK',
                'FIELD_MARKED_WRONG',
                'FIELD_MARKED_FIXED',
                'APPROVED',
                'APPROVAL_ROLLED_BACK',
                'REJECTED',
                'REOPENED',
                'MOVED_TO_RECHECK',
                'PUBLISHED',
                'UNPUBLISHED',
                'ARCHIVED',
                'RESTORED',
                'FIELD_ISSUE_DELETED'
            )
        );
