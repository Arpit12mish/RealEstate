-- Enforce the canonical Indian mobile phone number shape at the database level.
--
-- V107 backfilled every existing users.phone_number to +91XXXXXXXXXX and added a
-- unique index on the column, and PhoneNumberNormalizer enforces the same shape on
-- every application write path — but nothing previously stopped a direct/bulk write
-- (an admin import, a one-off data-fix script, a future code path that forgets to
-- normalize) from inserting a non-canonical duplicate the unique index wouldn't catch
-- (e.g. "9876543210" alongside an existing "+919876543210"). This migration makes the
-- canonical shape a schema-level invariant instead of an app-only convention.
--
-- This app is India-only today, so the constraint is intentionally narrow:
-- +91 followed by a 10-digit Indian mobile number starting with 6-9.
--
-- Safety:
-- 1) Fails fast with a clear message (instead of a cryptic constraint-violation error)
--    if any existing row is not already canonical, so a bad deploy can be caught and
--    the offending rows fixed/backfilled before retrying.
-- 2) Adds the constraint NOT VALID first, then VALIDATes it in a separate statement.
--    NOT VALID only requires a brief ACCESS EXCLUSIVE lock to add the constraint
--    metadata (no table scan). VALIDATE CONSTRAINT then scans existing rows while
--    holding only SHARE UPDATE EXCLUSIVE, which still allows concurrent reads/writes —
--    avoiding a long full-table lock on `users` during deploy.

DO $$
DECLARE
    non_canonical_count INTEGER;
BEGIN
    SELECT count(*) INTO non_canonical_count
    FROM users
    WHERE phone_number IS NOT NULL
      AND phone_number !~ '^\+91[6-9][0-9]{9}$';

    IF non_canonical_count > 0 THEN
        RAISE EXCEPTION
            'V110 aborted: % users.phone_number value(s) are not in canonical +91XXXXXXXXXX form. Review/backfill these rows (see the V107 backfill logic) before retrying this migration.',
            non_canonical_count
            USING ERRCODE = '23514';
    END IF;
END $$;

ALTER TABLE users
    ADD CONSTRAINT ck_users_phone_number_canonical_e164
    CHECK (phone_number ~ '^\+91[6-9][0-9]{9}$') NOT VALID;

ALTER TABLE users
    VALIDATE CONSTRAINT ck_users_phone_number_canonical_e164;
