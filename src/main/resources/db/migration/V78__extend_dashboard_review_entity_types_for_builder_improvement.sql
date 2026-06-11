-- =========================================================
-- V78__extend_dashboard_review_entity_types_for_builder_improvement.sql
--
-- Adds Builder Improvement entity types to dashboard review
-- history and field review issue constraints.
-- =========================================================

ALTER TABLE dashboard_content_review_history
    DROP CONSTRAINT IF EXISTS chk_dashboard_review_history_entity_type;

ALTER TABLE dashboard_content_review_history
    ADD CONSTRAINT chk_dashboard_review_history_entity_type
        CHECK (
            entity_type IN (
                'PROJECT',
                'PROJECT_MEDIA',
                'PROJECT_FLOOR_PLAN',
                'PROJECT_HIGHLIGHT',
                'PROJECT_CONNECTIVITY',
                'PROJECT_CONNECTIVITY_PLACE',
                'PROJECT_METER',
                'PROJECT_METER_SNAPSHOT',

                'BUILDER',
                'BUILDER_IMPROVEMENT_PROFILE',
                'BUILDER_IMPROVEMENT_ACTION',
                'BUILDER_IMPROVEMENT_ISSUE',
                'BUILDER_AFTER_SALES_UPGRADE',
                'BUILDER_IMPROVEMENT_TIMELINE',

                'COMPANY',
                'PROMO_BANNER',
                'APP_CONTENT',
                'CITY',
                'CATEGORY'
            )
        );


ALTER TABLE dashboard_field_review_issues
    DROP CONSTRAINT IF EXISTS chk_dashboard_field_review_entity_type;

ALTER TABLE dashboard_field_review_issues
    ADD CONSTRAINT chk_dashboard_field_review_entity_type
        CHECK (
            entity_type IN (
                'PROJECT',
                'PROJECT_MEDIA',
                'PROJECT_FLOOR_PLAN',
                'PROJECT_HIGHLIGHT',
                'PROJECT_CONNECTIVITY',
                'PROJECT_CONNECTIVITY_PLACE',
                'PROJECT_METER',
                'PROJECT_METER_SNAPSHOT',

                'BUILDER',
                'BUILDER_IMPROVEMENT_PROFILE',
                'BUILDER_IMPROVEMENT_ACTION',
                'BUILDER_IMPROVEMENT_ISSUE',
                'BUILDER_AFTER_SALES_UPGRADE',
                'BUILDER_IMPROVEMENT_TIMELINE',

                'COMPANY',
                'PROMO_BANNER',
                'APP_CONTENT',
                'CITY',
                'CATEGORY'
            )
        );