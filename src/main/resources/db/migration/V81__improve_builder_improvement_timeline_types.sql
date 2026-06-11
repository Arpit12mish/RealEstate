-- =========================================================
-- V81__improve_builder_improvement_timeline_types.sql
-- Improves builder improvement timeline for construction,
-- tower/floor, amenity, issue-resolution and delivery updates.
-- =========================================================

ALTER TABLE builder_improvement_timeline
    ADD COLUMN IF NOT EXISTS icon_key VARCHAR(80);

ALTER TABLE builder_improvement_timeline
    DROP CONSTRAINT IF EXISTS chk_builder_improvement_timeline_type;

ALTER TABLE builder_improvement_timeline
    ADD CONSTRAINT chk_builder_improvement_timeline_type
        CHECK (timeline_type IN (
            'DELAY_OBSERVATION',
            'AUDIT_INITIATED',
            'TRANSPARENCY_PROTOCOL',
            'MILESTONE_VERIFICATION',
            'DELIVERY_CERTIFICATION',
            'CUSTOMER_SUPPORT_UPDATE',
            'DOCUMENTATION_UPDATE',
            'ISSUE_RESOLUTION',

            'CONSTRUCTION_PROGRESS',
            'TOWER_COMPLETION',
            'FLOOR_COMPLETION',
            'AMENITY_PROGRESS',
            'APPROVAL_PROGRESS',
            'QUALITY_CHECK',
            'HANDOVER_PROGRESS',
            'POST_DELIVERY_SUPPORT',
            'COMMUNITY_CONTRIBUTION',

            'OTHER'
        ));