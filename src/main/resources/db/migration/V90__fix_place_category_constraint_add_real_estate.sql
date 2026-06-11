-- V90: Add REAL_ESTATE to chk_public_review_place_category
-- V82 created the constraint without REAL_ESTATE.
-- The live DB was patched manually in a previous session; this migration
-- makes the fix permanent so all environments (fresh or existing) are correct.

ALTER TABLE public_review_place
    DROP CONSTRAINT IF EXISTS chk_public_review_place_category;

ALTER TABLE public_review_place
    ADD CONSTRAINT chk_public_review_place_category
        CHECK (place_category IN (
            'PROJECT_SITE',
            'REAL_ESTATE',
            'BUILDER_OFFICE',
            'SALES_OFFICE',
            'SOCIETY',
            'CLUBHOUSE',
            'OTHER'
        ));
