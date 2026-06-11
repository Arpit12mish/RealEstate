-- =========================================================
-- V80__add_icon_key_to_builder_after_sales_upgrade.sql
-- Adds UI icon key for After-Sales Upgrade cards.
-- =========================================================

ALTER TABLE builder_after_sales_upgrade
    ADD COLUMN IF NOT EXISTS icon_key VARCHAR(80);