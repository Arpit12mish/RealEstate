-- =========================================================
-- V68__backfill_approved_review_status_for_published_projects.sql
-- Existing published projects should remain public after review hardening.
-- =========================================================

UPDATE project
SET
    review_status = 'APPROVED',
    reviewed_at = COALESCE(reviewed_at, updated_at, now()),
    review_remarks = COALESCE(
        review_remarks,
        'Backfilled as APPROVED because this project was already published before dashboard review workflow.'
    )
WHERE published = TRUE
  AND deleted = FALSE
  AND review_status = 'DRAFT';