ALTER TABLE instagram_reel
  ADD COLUMN IF NOT EXISTS source_thumbnail_url TEXT,
  ADD COLUMN IF NOT EXISTS cached_thumbnail_url TEXT,
  ADD COLUMN IF NOT EXISTS cached_thumbnail_storage_key VARCHAR(500),
  ADD COLUMN IF NOT EXISTS thumbnail_cached_at TIMESTAMP WITH TIME ZONE,
  ADD COLUMN IF NOT EXISTS meta_fetched_at TIMESTAMP WITH TIME ZONE,
  ADD COLUMN IF NOT EXISTS last_sync_status VARCHAR(40),
  ADD COLUMN IF NOT EXISTS last_sync_error TEXT;

UPDATE instagram_reel
SET source_thumbnail_url = thumbnail_url
WHERE source_thumbnail_url IS NULL
  AND thumbnail_url IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_instagram_reel_missing_cached_thumbnail
ON instagram_reel (active, deleted, cached_thumbnail_url)
WHERE active = TRUE
  AND deleted = FALSE
  AND cached_thumbnail_url IS NULL;
