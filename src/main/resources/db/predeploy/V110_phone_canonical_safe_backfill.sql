-- Safe backfill for V110__enforce_canonical_phone_number_format.sql.
--
-- BEFORE RUNNING:
--   1) Take a production database backup.
--   2) Run V110_phone_canonical_audit.sql and review every MANUAL_REVIEW row.
--
-- This script:
--   1) Detects collisions first and ABORTS the whole transaction (no rows
--      changed) if any non-canonical row's proposed canonical phone would
--      collide with another proposal or with an already-canonical phone
--      already owned by a different user.
--   2) Updates ONLY rows that are safely, unambiguously convertible:
--        - 10 digits starting 6-9            -> +91XXXXXXXXXX
--        - 12 digits starting "91" + valid    -> +91XXXXXXXXXX
--        - 11 digits starting "0" + valid     -> +91XXXXXXXXXX
--      "valid" = the remaining 10 digits start with 6-9 (Indian mobile),
--      matching PhoneNumberNormalizer.normalize and the V107 backfill logic.
--   3) Does NOT touch rows that cannot be safely canonicalized (e.g.
--      landline-shaped numbers, garbage input, foreign numbers) - those stay
--      exactly as they are for manual handling.
--   4) Never deletes users, never sets phone_number to NULL, never touches
--      email.
--   5) Prints the remaining invalid rows and count after the update, so you
--      can see immediately whether manual cleanup is still required.
--
-- Run:
--   psql "$DATABASE_URL" -f src/main/resources/db/predeploy/V110_phone_canonical_safe_backfill.sql
--
-- After this script reports remaining_invalid_count = 0, re-run
-- V110_phone_canonical_audit.sql to confirm, then run Flyway to apply V110.

\set ON_ERROR_STOP on

BEGIN;

-- ============================================================================
-- Step 0: Collision detail report (informational - always runs, changes nothing)
-- ============================================================================
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
)
SELECT
    p.proposed_canonical_phone,
    array_agg(p.id ORDER BY p.id) AS colliding_user_ids,
    'multiple non-canonical rows map to the same canonical phone' AS collision_reason
FROM proposed p
WHERE p.proposed_canonical_phone IS NOT NULL
GROUP BY p.proposed_canonical_phone
HAVING count(*) > 1
UNION ALL
SELECT
    p.proposed_canonical_phone,
    array_agg(DISTINCT p.id ORDER BY p.id) AS colliding_user_ids,
    'proposed canonical phone already belongs to a different existing user' AS collision_reason
FROM proposed p
JOIN users u
  ON u.phone_number = p.proposed_canonical_phone
 AND u.id <> p.id
WHERE p.proposed_canonical_phone IS NOT NULL
GROUP BY p.proposed_canonical_phone;

-- ============================================================================
-- Step 1: Hard collision guard - aborts the transaction if anything collides
-- ============================================================================
DO $$
DECLARE
    collision_count INTEGER;
BEGIN
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
    among_proposals AS (
        SELECT proposed_canonical_phone
        FROM proposed
        WHERE proposed_canonical_phone IS NOT NULL
        GROUP BY proposed_canonical_phone
        HAVING count(*) > 1
    ),
    against_existing AS (
        SELECT DISTINCT p.proposed_canonical_phone
        FROM proposed p
        JOIN users u
          ON u.phone_number = p.proposed_canonical_phone
         AND u.id <> p.id
        WHERE p.proposed_canonical_phone IS NOT NULL
    )
    SELECT count(*) INTO collision_count
    FROM (
        SELECT proposed_canonical_phone FROM among_proposals
        UNION
        SELECT proposed_canonical_phone FROM against_existing
    ) collisions;

    IF collision_count > 0 THEN
        RAISE EXCEPTION
            'Safe backfill aborted: % canonical phone value(s) would collide (see the collision report above). No rows were changed. Resolve these manually (survivor merge, see V107_phone_duplicate_cleanup_template.sql) before rerunning this script.',
            collision_count
            USING ERRCODE = '23505';
    END IF;
END $$;

-- ============================================================================
-- Step 2: Update only the safe, unambiguous, collision-free rows
-- ============================================================================
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
        id,
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
)
UPDATE users u
SET phone_number = p.proposed_canonical_phone
FROM proposed p
WHERE u.id = p.id
  AND p.proposed_canonical_phone IS NOT NULL;

-- ============================================================================
-- Step 3: Remaining invalid rows requiring manual review
-- ============================================================================
SELECT
    id,
    email,
    phone_number,
    regexp_replace(phone_number, '\D', '', 'g') AS extracted_digits,
    created_at,
    last_login_at,
    role,
    onboarding_status
FROM users
WHERE phone_number IS NOT NULL
  AND phone_number !~ '^\+91[6-9][0-9]{9}$'
ORDER BY id;

-- ============================================================================
-- Step 4: Remaining invalid count - must be 0 before Flyway V110 can succeed
-- ============================================================================
SELECT count(*) AS remaining_invalid_count
FROM users
WHERE phone_number IS NOT NULL
  AND phone_number !~ '^\+91[6-9][0-9]{9}$';

COMMIT;
