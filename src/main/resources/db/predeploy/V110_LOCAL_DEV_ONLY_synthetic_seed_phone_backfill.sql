-- =============================================================================
-- LOCAL DEV ONLY - NEVER PRODUCTION.
--
-- This script bulk-rewrites phone_number for rows that are unambiguously
-- synthetic local seed/test accounts (see the predicate below), so that
-- V110__enforce_canonical_phone_number_format.sql can run on a local
-- database seeded with throwaway data. It must never be pointed at a
-- production DATABASE_URL. See README_V110_PHONE_CANONICAL_CLEANUP.md
-- section "1. Local / dev cleanup" for the rule this file follows.
--
-- Why this exists (and why V110_phone_canonical_safe_backfill.sql doesn't
-- cover it): that script is production-safe by design - it only derives a
-- canonical form from digits that were plausibly a real, mis-formatted
-- Indian mobile number, and refuses to touch anything else. On this local
-- database, all 117 non-canonical rows are fake seed accounts created with
-- sequential/placeholder digit strings (e.g. "+911234567891") that are not
-- real phone numbers at all - there is no real number to "recover" here, so
-- the production script correctly classifies every one of them as
-- MANUAL_REVIEW. For local seed data specifically, the fix is to replace the
-- fake number with a different, canonical-shaped fake number, not to decide
-- what real number a real user meant to type.
--
-- Scope guard: this script ONLY touches rows where BOTH are true:
--   1) phone_number does not already match ^\+91[6-9][0-9]{9}$
--   2) email matches the synthetic seed placeholder pattern
--      ^phone_[0-9]+@phone\.local$
-- Any non-canonical row with a different email shape is left completely
-- untouched (it is not a recognizable local-seed placeholder, and must go
-- through the manual review process in README_V110_PHONE_CANONICAL_CLEANUP.md
-- instead, exactly as it would on a real environment).
--
-- Replacement scheme: +91 + '7' + the user's own id, zero-padded to 9
-- digits (e.g. id=7 -> +917000000007). This is deterministic, trivially
-- unique (id is the primary key), and collision-checked against both other
-- proposed values and any already-canonical phone number before anything is
-- written.
--
-- Never deletes users, never sets phone_number to NULL, never touches email.
--
-- Run:
--   psql -h localhost -U sfs_user -d sfs_db \
--     -f src/main/resources/db/predeploy/V110_LOCAL_DEV_ONLY_synthetic_seed_phone_backfill.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

-- ============================================================================
-- Step 0: rows this script will touch (informational)
-- ============================================================================
SELECT
    id,
    email,
    phone_number AS current_phone_number,
    '+91' || '7' || lpad(id::text, 9, '0') AS new_phone_number
FROM users
WHERE phone_number IS NOT NULL
  AND phone_number !~ '^\+91[6-9][0-9]{9}$'
  AND email ~ '^phone_[0-9]+@phone\.local$'
ORDER BY id;

-- ============================================================================
-- Step 1: collision guard - aborts with zero rows changed if anything collides
-- ============================================================================
DO $$
DECLARE
    collision_count INTEGER;
BEGIN
    WITH targets AS (
        SELECT id, '+91' || '7' || lpad(id::text, 9, '0') AS new_phone_number
        FROM users
        WHERE phone_number IS NOT NULL
          AND phone_number !~ '^\+91[6-9][0-9]{9}$'
          AND email ~ '^phone_[0-9]+@phone\.local$'
    ),
    among_targets AS (
        SELECT new_phone_number
        FROM targets
        GROUP BY new_phone_number
        HAVING count(*) > 1
    ),
    against_existing AS (
        SELECT DISTINCT t.new_phone_number
        FROM targets t
        JOIN users u
          ON u.phone_number = t.new_phone_number
         AND u.id <> t.id
    )
    SELECT count(*) INTO collision_count
    FROM (
        SELECT new_phone_number FROM among_targets
        UNION
        SELECT new_phone_number FROM against_existing
    ) collisions;

    IF collision_count > 0 THEN
        RAISE EXCEPTION
            'LOCAL_DEV_ONLY backfill aborted: % generated phone value(s) would collide. No rows were changed.',
            collision_count
            USING ERRCODE = '23505';
    END IF;
END $$;

-- ============================================================================
-- Step 2: rewrite only the scoped synthetic-seed rows
-- ============================================================================
UPDATE users u
SET phone_number = '+91' || '7' || lpad(u.id::text, 9, '0')
WHERE u.phone_number IS NOT NULL
  AND u.phone_number !~ '^\+91[6-9][0-9]{9}$'
  AND u.email ~ '^phone_[0-9]+@phone\.local$';

-- ============================================================================
-- Step 3: anything still non-canonical requires manual review (should be
-- empty on a database where every non-canonical row is a synthetic seed
-- account; any row printed here needs the manual process, not this script)
-- ============================================================================
SELECT
    id,
    email,
    phone_number,
    role,
    onboarding_status
FROM users
WHERE phone_number IS NOT NULL
  AND phone_number !~ '^\+91[6-9][0-9]{9}$'
ORDER BY id;

-- ============================================================================
-- Step 4: remaining invalid count - must be 0 before Flyway V110 can succeed
-- ============================================================================
SELECT count(*) AS remaining_invalid_count
FROM users
WHERE phone_number IS NOT NULL
  AND phone_number !~ '^\+91[6-9][0-9]{9}$';

COMMIT;
