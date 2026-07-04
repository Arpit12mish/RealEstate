CREATE TABLE IF NOT EXISTS instagram_reel (
  id BIGSERIAL PRIMARY KEY,
  instagram_media_id VARCHAR(100),
  caption TEXT,
  title VARCHAR(180),
  instagram_url VARCHAR(500) NOT NULL,
  thumbnail_url VARCHAR(1000),
  preview_video_url VARCHAR(1000),
  media_type VARCHAR(50),
  media_product_type VARCHAR(50),
  published_at TIMESTAMP WITH TIME ZONE,
  view_count BIGINT NOT NULL DEFAULT 0,
  like_count BIGINT NOT NULL DEFAULT 0,
  comment_count BIGINT NOT NULL DEFAULT 0,
  share_count BIGINT NOT NULL DEFAULT 0,
  save_count BIGINT NOT NULL DEFAULT 0,
  trending_score NUMERIC(14,2) NOT NULL DEFAULT 0,
  category_override VARCHAR(50),
  display_order INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  synced_from_meta BOOLEAN NOT NULL DEFAULT TRUE,
  last_synced_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT chk_instagram_reel_category
    CHECK (category_override IS NULL OR category_override IN (
      'LATEST',
      'TRENDING',
      'INTERVIEWS',
      'MOST_VIEWED',
      'MANUAL'
    ))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_instagram_reel_media_id_active
ON instagram_reel (instagram_media_id)
WHERE deleted = FALSE AND instagram_media_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_instagram_reel_url_active
ON instagram_reel (instagram_url)
WHERE deleted = FALSE AND instagram_url IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_instagram_reel_active_published
ON instagram_reel (active, deleted, published_at DESC);

CREATE INDEX IF NOT EXISTS idx_instagram_reel_active_trending
ON instagram_reel (active, deleted, trending_score DESC);

CREATE INDEX IF NOT EXISTS idx_instagram_reel_active_view_count
ON instagram_reel (active, deleted, view_count DESC);

CREATE INDEX IF NOT EXISTS idx_instagram_reel_category_override
ON instagram_reel (category_override);

CREATE INDEX IF NOT EXISTS idx_instagram_reel_display_order
ON instagram_reel (display_order);

INSERT INTO home_section_config (
  home_category_id,
  section_type,
  title,
  subtitle,
  enabled,
  sort_order,
  max_items,
  created_at,
  updated_at
)
SELECT
  0,
  'INSTAGRAM_REELS',
  'Instagram Reels',
  'See our latest reels on instagram',
  TRUE,
  28,
  10,
  NOW(),
  NOW()
WHERE EXISTS (
  SELECT 1 FROM category WHERE id = 0
)
AND NOT EXISTS (
  SELECT 1
  FROM home_section_config
  WHERE home_category_id = 0
    AND section_type = 'INSTAGRAM_REELS'
);
