-- One guest_session row is one anonymous identity epoch. A converted epoch is
-- closed permanently; the same installation may later create a new epoch.

CREATE TABLE guest_identity_link (
    id                  BIGSERIAL PRIMARY KEY,
    guest_session_id    BIGINT NOT NULL,
    user_id             BIGINT NOT NULL,
    installation_id     VARCHAR(120) NOT NULL,
    linked_at            TIMESTAMPTZ NOT NULL,
    link_type            VARCHAR(30) NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_guest_identity_link_session UNIQUE (guest_session_id),
    CONSTRAINT fk_guest_identity_link_session
        FOREIGN KEY (guest_session_id) REFERENCES guest_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_guest_identity_link_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_guest_identity_link_type
        CHECK (link_type = 'OTP_CONVERSION')
);

-- Preserve legacy conversions before closing their reusable guest epochs.
INSERT INTO guest_identity_link (
    guest_session_id,
    user_id,
    installation_id,
    linked_at,
    link_type,
    created_at
)
SELECT
    id,
    linked_user_id,
    installation_id,
    COALESCE(linked_at, updated_at, created_at, now()),
    'OTP_CONVERSION',
    COALESCE(linked_at, updated_at, created_at, now())
FROM guest_sessions
WHERE linked_user_id IS NOT NULL
ON CONFLICT (guest_session_id) DO NOTHING;

UPDATE guest_sessions
SET active = FALSE
WHERE linked_user_id IS NOT NULL
  AND active = TRUE;

ALTER TABLE guest_sessions
    DROP CONSTRAINT IF EXISTS guest_sessions_installation_id_key;

CREATE UNIQUE INDEX ux_guest_sessions_one_active_epoch_per_installation
    ON guest_sessions (installation_id)
    WHERE active = TRUE AND linked_user_id IS NULL;

ALTER TABLE guest_sessions
    ADD CONSTRAINT chk_guest_sessions_active_epoch_unconverted
    CHECK (NOT active OR linked_user_id IS NULL);

CREATE INDEX idx_guest_identity_link_user
    ON guest_identity_link (user_id);

CREATE INDEX idx_guest_identity_link_installation
    ON guest_identity_link (installation_id, linked_at DESC);

-- Conversion history may be erased by row deletion for account privacy, but
-- it cannot be reassigned or rewritten in place.
CREATE FUNCTION reject_guest_identity_link_update()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'guest_identity_link rows are append-only';
END;
$$;

CREATE TRIGGER trg_guest_identity_link_append_only
    BEFORE UPDATE ON guest_identity_link
    FOR EACH ROW
    EXECUTE FUNCTION reject_guest_identity_link_update();

CREATE FUNCTION reject_guest_session_relink()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.linked_user_id IS NOT NULL
       AND (
           NEW.linked_user_id IS DISTINCT FROM OLD.linked_user_id
           OR NEW.linked_at IS DISTINCT FROM OLD.linked_at
       ) THEN
        RAISE EXCEPTION 'a converted guest session cannot be relinked';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_guest_session_link_immutable
    BEFORE UPDATE ON guest_sessions
    FOR EACH ROW
    EXECUTE FUNCTION reject_guest_session_relink();
