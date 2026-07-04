-- Align mobile authentication persistence with the current JPA entities.
-- Forward-only and additive: existing tables/columns are preserved.

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    name VARCHAR(80),
    password VARCHAR(255),
    phone_number VARCHAR(20) NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    role VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at TIMESTAMPTZ,
    onboarding_status VARCHAR(40) NOT NULL DEFAULT 'ROLE_PENDING',
    role_selected_at TIMESTAMPTZ,
    profile_photo_url TEXT,
    profile_photo_storage_key VARCHAR(500)
);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS name VARCHAR(80),
    ADD COLUMN IF NOT EXISTS password VARCHAR(255),
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS role VARCHAR(40),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS onboarding_status VARCHAR(40) NOT NULL DEFAULT 'ROLE_PENDING',
    ADD COLUMN IF NOT EXISTS role_selected_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS profile_photo_url TEXT,
    ADD COLUMN IF NOT EXISTS profile_photo_storage_key VARCHAR(500);

DO $$
DECLARE
    duplicate_count INTEGER;
BEGIN
    WITH canonical_users AS (
        SELECT
            id,
            phone_number,
            CASE
                WHEN length(regexp_replace(phone_number, '\D', '', 'g')) = 10
                     AND regexp_replace(phone_number, '\D', '', 'g') ~ '^[6-9][0-9]{9}$'
                    THEN '+91' || regexp_replace(phone_number, '\D', '', 'g')
                WHEN length(regexp_replace(phone_number, '\D', '', 'g')) = 11
                     AND regexp_replace(phone_number, '\D', '', 'g') ~ '^0[6-9][0-9]{9}$'
                    THEN '+91' || substring(regexp_replace(phone_number, '\D', '', 'g') FROM 2)
                WHEN length(regexp_replace(phone_number, '\D', '', 'g')) = 12
                     AND regexp_replace(phone_number, '\D', '', 'g') ~ '^91[6-9][0-9]{9}$'
                    THEN '+91' || substring(regexp_replace(phone_number, '\D', '', 'g') FROM 3)
                ELSE phone_number
            END AS canonical_phone
        FROM users
        WHERE phone_number IS NOT NULL
    ),
    duplicate_groups AS (
        SELECT canonical_phone
        FROM canonical_users
        GROUP BY canonical_phone
        HAVING count(*) > 1
    )
    SELECT count(*) INTO duplicate_count
    FROM duplicate_groups;

    IF duplicate_count > 0 THEN
        RAISE EXCEPTION
            'V107 aborted: duplicate equivalent users.phone_number values detected. Run src/main/resources/db/predeploy/V107_phone_duplicate_check.sql and manually merge users before deploying.'
            USING ERRCODE = '23505';
    END IF;
END $$;

WITH candidates AS (
    SELECT
        id,
        phone_number,
        regexp_replace(phone_number, '\D', '', 'g') AS digits
    FROM users
    WHERE phone_number IS NOT NULL
)
UPDATE users u
SET phone_number = CASE
    WHEN length(c.digits) = 10 AND c.digits ~ '^[6-9][0-9]{9}$'
        THEN '+91' || c.digits
    WHEN length(c.digits) = 11 AND c.digits ~ '^0[6-9][0-9]{9}$'
        THEN '+91' || substring(c.digits FROM 2)
    WHEN length(c.digits) = 12 AND c.digits ~ '^91[6-9][0-9]{9}$'
        THEN '+91' || substring(c.digits FROM 3)
    ELSE u.phone_number
END
FROM candidates c
WHERE u.id = c.id
  AND u.phone_number <> CASE
      WHEN length(c.digits) = 10 AND c.digits ~ '^[6-9][0-9]{9}$'
          THEN '+91' || c.digits
      WHEN length(c.digits) = 11 AND c.digits ~ '^0[6-9][0-9]{9}$'
          THEN '+91' || substring(c.digits FROM 2)
      WHEN length(c.digits) = 12 AND c.digits ~ '^91[6-9][0-9]{9}$'
          THEN '+91' || substring(c.digits FROM 3)
      ELSE u.phone_number
  END
  AND NOT EXISTS (
      SELECT 1
      FROM users existing
      WHERE existing.id <> u.id
        AND existing.phone_number = CASE
            WHEN length(c.digits) = 10 AND c.digits ~ '^[6-9][0-9]{9}$'
                THEN '+91' || c.digits
            WHEN length(c.digits) = 11 AND c.digits ~ '^0[6-9][0-9]{9}$'
                THEN '+91' || substring(c.digits FROM 2)
            WHEN length(c.digits) = 12 AND c.digits ~ '^91[6-9][0-9]{9}$'
                THEN '+91' || substring(c.digits FROM 3)
            ELSE u.phone_number
        END
  );

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_phone_number_normalized
    ON users (phone_number)
    WHERE phone_number IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email
    ON users (email)
    WHERE email IS NOT NULL;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(256) NOT NULL,
    device_id VARCHAR(255),
    fcm_token TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS user_id BIGINT,
    ADD COLUMN IF NOT EXISTS token VARCHAR(256),
    ADD COLUMN IF NOT EXISTS device_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS fcm_token TEXT,
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS revoked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE UNIQUE INDEX IF NOT EXISTS ux_refresh_tokens_token_hash
    ON refresh_tokens (token);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_device_active
    ON refresh_tokens (user_id, device_id, revoked, expires_at);

CREATE TABLE IF NOT EXISTS otps (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    type VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_otps_user_verified_expires
    ON otps (user_id, verified, expires_at);

COMMENT ON TABLE otps IS 'Legacy/local OTP table. Current production OTP flow uses Twilio Verify for generation, expiry, invalidation, and reuse prevention.';
COMMENT ON COLUMN refresh_tokens.token IS 'SHA-256 hex digest of the raw refresh token; raw refresh tokens are never persisted.';
