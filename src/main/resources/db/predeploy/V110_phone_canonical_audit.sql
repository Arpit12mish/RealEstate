-- Pre-deploy audit report for V110__enforce_canonical_phone_number_format.sql.
--
-- Read-only. Does not mutate any data.
--
-- Lists every users row whose phone_number does not already match the
-- canonical shape V110 enforces (^\+91[6-9][0-9]{9}$), shows the digits
-- extracted from the stored value, proposes the canonical +91XXXXXXXXXX form
-- where it can be derived unambiguously (same rules as
-- PhoneNumberNormalizer.normalize / the V107 backfill logic), and classifies
-- each row as either:
--
--   SAFE_TO_BACKFILL - a canonical form can be derived, it doesn't collide
--                       with another row's proposed canonical form, and it
--                       doesn't collide with an already-canonical phone
--                       number belonging to a different user.
--   MANUAL_REVIEW    - no canonical form could be derived, or applying it
--                       would create a duplicate. These require a human
--                       decision (see V107_phone_duplicate_cleanup_template.sql
--                       for the survivor-merge pattern) before V110 can run.
--
-- Run this against production before deploying V110:
--   psql "$DATABASE_URL" -f src/main/resources/db/predeploy/V110_phone_canonical_audit.sql
--
-- Next steps: V110_phone_canonical_safe_backfill.sql, then this audit again,
-- then manually resolve any remaining MANUAL_REVIEW rows, then Flyway V110.

WITH invalid AS (
    SELECT
        id,
        email,
        phone_number,
        created_at,
        last_login_at,
        role,
        onboarding_status,
        regexp_replace(phone_number, '\D', '', 'g') AS digits
    FROM users
    WHERE phone_number IS NOT NULL
      AND phone_number !~ '^\+91[6-9][0-9]{9}$'
),
proposed AS (
    SELECT
        *,
        CASE
            WHEN length(digits) = 10 AND digits ~ '^[6-9][0-9]{9}$'
                THEN '+91' || digits
            WHEN length(digits) = 11 AND digits ~ '^0[6-9][0-9]{9}$'
                THEN '+91' || substring(digits FROM 2)
            WHEN length(digits) = 12 AND digits ~ '^91[6-9][0-9]{9}$'
                THEN '+91' || substring(digits FROM 3)
            ELSE NULL
        END AS proposed_canonical_phone
    FROM invalid
),
proposal_collisions AS (
    -- Two or more non-canonical rows would map to the same canonical phone.
    SELECT proposed_canonical_phone, count(*) AS collision_count
    FROM proposed
    WHERE proposed_canonical_phone IS NOT NULL
    GROUP BY proposed_canonical_phone
    HAVING count(*) > 1
),
existing_canonical_collisions AS (
    -- The proposed canonical phone is already owned by a different user.
    SELECT DISTINCT p.id, p.proposed_canonical_phone
    FROM proposed p
    JOIN users u
      ON u.phone_number = p.proposed_canonical_phone
     AND u.id <> p.id
    WHERE p.proposed_canonical_phone IS NOT NULL
)
SELECT
    p.id,
    p.email,
    p.phone_number AS current_phone_number,
    p.digits AS extracted_digits,
    p.proposed_canonical_phone,
    CASE
        WHEN p.proposed_canonical_phone IS NULL THEN 'MANUAL_REVIEW'
        WHEN pc.collision_count > 1 THEN 'MANUAL_REVIEW'
        WHEN ecc.id IS NOT NULL THEN 'MANUAL_REVIEW'
        ELSE 'SAFE_TO_BACKFILL'
    END AS classification,
    CASE
        WHEN p.proposed_canonical_phone IS NULL
            THEN 'no canonical +91XXXXXXXXXX form could be derived from the extracted digits'
        WHEN pc.collision_count > 1
            THEN 'another non-canonical row would map to the same canonical phone'
        WHEN ecc.id IS NOT NULL
            THEN 'canonical phone is already used by a different user'
        ELSE NULL
    END AS review_reason,
    p.created_at,
    p.last_login_at,
    p.role,
    p.onboarding_status
FROM proposed p
LEFT JOIN proposal_collisions pc ON pc.proposed_canonical_phone = p.proposed_canonical_phone
LEFT JOIN existing_canonical_collisions ecc ON ecc.id = p.id
ORDER BY classification DESC, p.id;

-- Summary counts (also printed at the end of the same run).
WITH invalid AS (
    SELECT
        id,
        phone_number,
        regexp_replace(phone_number, '\D', '', 'g') AS digits
    FROM users
    WHERE phone_number IS NOT NULL
      AND phone_number !~ '^\+91[6-9][0-9]{9}$'
),
proposed AS (
    SELECT
        *,
        CASE
            WHEN length(digits) = 10 AND digits ~ '^[6-9][0-9]{9}$'
                THEN '+91' || digits
            WHEN length(digits) = 11 AND digits ~ '^0[6-9][0-9]{9}$'
                THEN '+91' || substring(digits FROM 2)
            WHEN length(digits) = 12 AND digits ~ '^91[6-9][0-9]{9}$'
                THEN '+91' || substring(digits FROM 3)
            ELSE NULL
        END AS proposed_canonical_phone
    FROM invalid
),
proposal_collisions AS (
    SELECT proposed_canonical_phone
    FROM proposed
    WHERE proposed_canonical_phone IS NOT NULL
    GROUP BY proposed_canonical_phone
    HAVING count(*) > 1
),
existing_canonical_collisions AS (
    SELECT DISTINCT p.id
    FROM proposed p
    JOIN users u
      ON u.phone_number = p.proposed_canonical_phone
     AND u.id <> p.id
    WHERE p.proposed_canonical_phone IS NOT NULL
)
SELECT
    count(*) AS total_invalid_rows,
    count(*) FILTER (
        WHERE proposed_canonical_phone IS NOT NULL
          AND proposed_canonical_phone NOT IN (SELECT proposed_canonical_phone FROM proposal_collisions)
          AND id NOT IN (SELECT id FROM existing_canonical_collisions)
    ) AS safe_to_backfill_count,
    count(*) FILTER (
        WHERE proposed_canonical_phone IS NULL
           OR proposed_canonical_phone IN (SELECT proposed_canonical_phone FROM proposal_collisions)
           OR id IN (SELECT id FROM existing_canonical_collisions)
    ) AS manual_review_count
FROM proposed;
