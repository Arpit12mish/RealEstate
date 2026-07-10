-- Extends the existing generic public-review system (V82) to support COMPANY
-- targets (architect/interior designer profiles), alongside PROJECT and BUILDER.
-- target_id is already a plain BIGINT (not a FK to a specific table), so no
-- schema change is needed beyond relaxing these three CHECK constraints -
-- PublicReviewServiceImpl gets the corresponding COMPANY branches in Java.

ALTER TABLE public_review_place
  DROP CONSTRAINT IF EXISTS chk_public_review_place_target_type;
ALTER TABLE public_review_place
  ADD CONSTRAINT chk_public_review_place_target_type
  CHECK (target_type IN ('PROJECT', 'BUILDER', 'COMPANY'));

ALTER TABLE public_review_summary
  DROP CONSTRAINT IF EXISTS chk_public_review_summary_target_type;
ALTER TABLE public_review_summary
  ADD CONSTRAINT chk_public_review_summary_target_type
  CHECK (target_type IN ('PROJECT', 'BUILDER', 'COMPANY'));

ALTER TABLE public_review_sample
  DROP CONSTRAINT IF EXISTS chk_public_review_sample_target_type;
ALTER TABLE public_review_sample
  ADD CONSTRAINT chk_public_review_sample_target_type
  CHECK (target_type IN ('PROJECT', 'BUILDER', 'COMPANY'));
