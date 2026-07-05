-- PRODUCTION manual-review report for V110__enforce_canonical_phone_number_format.sql.
--
-- READ-ONLY. This script contains no INSERT/UPDATE/DELETE - it cannot mutate
-- data. Safe to run against production at any time, as many times as needed.
--
-- Production has a small number of users, so every invalid row is expected to
-- be resolved one at a time by a human using
-- V110_phone_canonical_manual_resolution_template.sql - NOT by bulk
-- auto-generation of phone numbers, and NOT by running the local/dev-only
-- convenience backfill against production (see README, "Local/dev cleanup"
-- section, which is explicitly marked LOCAL DEV ONLY - NEVER PRODUCTION).
--
-- Output is CSV-friendly (run with `--csv` per the README) and includes, per
-- invalid row:
--   - id, email, current phone_number, extracted digits, proposed canonical
--     phone (only where derivable - never fabricated)
--   - classification: SAFE_CANONICALIZATION_CANDIDATE | DUPLICATE_COLLISION |
--     INVALID_NON_MOBILE | MANUAL_SUPPORT_REVIEW
--   - a human-readable reason
--   - created_at, last_login_at, role, onboarding_status
--   - is_verified, is_admin (the only status-like columns that actually exist
--     on `users` today - there is no is_active/status/deleted_at/soft-delete
--     column in this schema, confirmed against the migration history)
--   - whether the email looks like the app's own synthetic
--     "phone_<digits>@phone.local" placeholder (see
--     UserServiceImpl#findOrCreateVerifiedUserByPhone) - this is normal for
--     phone-only signups and is informational, not a red flag by itself
--   - a plain-English recommended_action - never executable SQL

WITH invalid AS (
    SELECT
        id,
        email,
        phone_number,
        created_at,
        last_login_at,
        role,
        onboarding_status,
        is_verified,
        is_admin,
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
proposal_collision_flag AS (
    -- Two or more invalid rows would map to the same canonical phone.
    SELECT proposed_canonical_phone
    FROM proposed
    WHERE proposed_canonical_phone IS NOT NULL
    GROUP BY proposed_canonical_phone
    HAVING count(*) > 1
),
existing_collision_flag AS (
    -- The proposed canonical phone already belongs to a different existing user.
    SELECT DISTINCT p.id
    FROM proposed p
    JOIN users u
      ON u.phone_number = p.proposed_canonical_phone
     AND u.id <> p.id
    WHERE p.proposed_canonical_phone IS NOT NULL
),
classified AS (
    SELECT
        p.*,
        (pc.proposed_canonical_phone IS NOT NULL) AS has_proposal_collision,
        (ec.id IS NOT NULL) AS has_existing_collision
    FROM proposed p
    LEFT JOIN proposal_collision_flag pc ON pc.proposed_canonical_phone = p.proposed_canonical_phone
    LEFT JOIN existing_collision_flag ec ON ec.id = p.id
)
SELECT
    id,
    email,
    phone_number AS current_phone_number,
    NULLIF(digits, '') AS extracted_digits,
    proposed_canonical_phone,
    CASE
        WHEN is_admin THEN 'MANUAL_SUPPORT_REVIEW'
        WHEN proposed_canonical_phone IS NOT NULL
             AND NOT has_proposal_collision
             AND NOT has_existing_collision
            THEN 'SAFE_CANONICALIZATION_CANDIDATE'
        WHEN proposed_canonical_phone IS NOT NULL
            THEN 'DUPLICATE_COLLISION'
        WHEN digits IS NOT NULL AND length(digits) > 0
            THEN 'INVALID_NON_MOBILE'
        ELSE 'MANUAL_SUPPORT_REVIEW'
    END AS classification,
    CASE
        WHEN is_admin
            THEN 'admin account (is_admin = true) - handle with extra caution regardless of derivable canonical form'
        WHEN proposed_canonical_phone IS NOT NULL AND NOT has_proposal_collision AND NOT has_existing_collision
            THEN 'unambiguous Indian mobile number stored in a non-canonical shape'
        WHEN proposed_canonical_phone IS NOT NULL AND has_proposal_collision
            THEN 'another invalid row would map to the same canonical phone - survivor must be chosen manually'
        WHEN proposed_canonical_phone IS NOT NULL AND has_existing_collision
            THEN 'canonical phone already belongs to a different existing user - survivor must be chosen manually'
        WHEN digits IS NOT NULL AND length(digits) > 0
            THEN 'digits present but do not match a valid Indian mobile shape (wrong length or leading digit) - likely landline, foreign number, or a data-entry error'
        ELSE 'no usable digits could be extracted from the stored value - value may be blank, a placeholder, or garbage data'
    END AS reason,
    created_at,
    last_login_at,
    role,
    onboarding_status,
    is_verified,
    is_admin,
    (email ~ '^phone_[0-9]+@phone\.local$') AS email_is_synthetic_phone_placeholder,
    CASE
        WHEN is_admin
            THEN 'Do NOT modify without explicit business sign-off. Confirm this is a genuine admin and not a leftover test/local account promoted by mistake before doing anything.'
        WHEN proposed_canonical_phone IS NOT NULL AND NOT has_proposal_collision AND NOT has_existing_collision
            THEN 'Confirm ownership with the user/support record, then apply Case A in V110_phone_canonical_manual_resolution_template.sql for this single user id.'
        WHEN proposed_canonical_phone IS NOT NULL
            THEN 'Apply Case B in V110_phone_canonical_manual_resolution_template.sql: inventory dependent rows, choose one survivor manually, revoke duplicate sessions, get business approval before merging or deleting anything.'
        WHEN digits IS NOT NULL AND length(digits) > 0
            THEN 'Contact the user or support to obtain the correct Indian mobile number, then apply Case A for this single user id once confirmed - never fabricate a replacement number.'
        ELSE 'Escalate to business/support: determine whether this is a real user with missing/garbled data or a local/test account left in production (see Case C). Do not auto-delete, do not null the phone, do not fabricate a number.'
    END AS recommended_action
FROM classified
ORDER BY
    CASE WHEN is_admin THEN 0 ELSE 1 END,
    classification,
    id;
