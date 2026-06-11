-- =========================================================
-- V75__fix_review_history_action_type_constraint.sql
--
-- The original V66 constraint was written before REOPENED
-- and FIELD_ISSUE_DELETED existed in ReviewActionType.
-- Inserting a history row with action_type = 'REOPENED'
-- violated the constraint and caused a 409 on every reopen.
-- =========================================================

ALTER TABLE dashboard_content_review_history
    DROP CONSTRAINT chk_dashboard_review_history_action_type;

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
