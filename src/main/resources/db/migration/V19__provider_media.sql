CREATE TABLE IF NOT EXISTS provider_media (
  id BIGSERIAL PRIMARY KEY,
  provider_id BIGINT NOT NULL,

  media_type VARCHAR(20) NOT NULL, -- PROFILE_PHOTO / GALLERY
  url TEXT NOT NULL,
  thumbnail_url TEXT,
  sort_order INT NOT NULL DEFAULT 0,

  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_provider_media_provider
    FOREIGN KEY (provider_id) REFERENCES provider_profile(id)
    ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_provider_media_provider_id ON provider_media(provider_id);
CREATE INDEX IF NOT EXISTS idx_provider_media_provider_type ON provider_media(provider_id, media_type);
