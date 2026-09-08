-- Records when the current Google-fetch reservation (fetch_status = 'FETCHING')
-- was taken, so a sync attempt can tell a fresh in-flight reservation from one
-- abandoned by a crashed/restarted process and reclaim only the latter after a
-- configurable lease (google.places.fetch-lease-seconds). Deliberately a
-- dedicated column rather than reusing updated_at: updated_at is bumped by
-- unrelated place edits (e.g. PublicReviewServiceImpl#attachGooglePlace
-- toggling active/category), which would make a genuinely stale reservation
-- look fresh again and block legitimate reclamation.
ALTER TABLE public_review_place
    ADD COLUMN IF NOT EXISTS fetch_started_at TIMESTAMPTZ;
