-- Add dashboard-manageable pricing fields to project table.
-- monthly_emi_min / monthly_emi_max: override the computed EMI shown in the mobile app.
-- average_price_per_sqft: override the ProjectMeter-derived average area price.
-- All nullable so existing projects are unaffected.

ALTER TABLE project
    ADD COLUMN IF NOT EXISTS monthly_emi_min       BIGINT,
    ADD COLUMN IF NOT EXISTS monthly_emi_max       BIGINT,
    ADD COLUMN IF NOT EXISTS average_price_per_sqft BIGINT;
