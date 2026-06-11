-- =========================================================
-- V79__add_delay_reason_to_builder_improvement_issue.sql
-- Adds delay reason for in-progress/pending issue closure cards.
-- =========================================================

ALTER TABLE builder_improvement_issue
    ADD COLUMN IF NOT EXISTS delay_reason TEXT;