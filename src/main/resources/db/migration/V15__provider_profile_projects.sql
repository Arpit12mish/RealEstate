-- Add owner link to business
ALTER TABLE business
ADD COLUMN IF NOT EXISTS owner_user_id BIGINT NULL;

CREATE INDEX IF NOT EXISTS idx_business_owner_user_id ON business(owner_user_id);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_business_owner_user'
  ) THEN
    ALTER TABLE business
      ADD CONSTRAINT fk_business_owner_user
      FOREIGN KEY (owner_user_id) REFERENCES users(id);
  END IF;
END $$;


-- Provider profile
CREATE TABLE IF NOT EXISTS provider_profile (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  business_id BIGINT NULL UNIQUE,

  provider_type VARCHAR(20) NOT NULL, -- WORKER/BRAND
  display_name VARCHAR(120) NOT NULL,
  headline VARCHAR(180),
  bio TEXT,

  primary_category_id BIGINT NOT NULL,
  experience_years INT,

  business_name VARCHAR(150),
  gst_number VARCHAR(30),

  verification_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
  is_featured BOOLEAN NOT NULL DEFAULT FALSE,

  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_provider_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_provider_business FOREIGN KEY (business_id) REFERENCES business(id),
  CONSTRAINT fk_provider_category FOREIGN KEY (primary_category_id) REFERENCES category(id)
);

CREATE INDEX IF NOT EXISTS idx_provider_category ON provider_profile(primary_category_id);

-- Provider service areas
CREATE TABLE IF NOT EXISTS provider_service_area (
  id BIGSERIAL PRIMARY KEY,
  provider_id BIGINT NOT NULL,
  city_id BIGINT NOT NULL,
  locality VARCHAR(120),
  pincode VARCHAR(12),
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,

  CONSTRAINT fk_area_provider FOREIGN KEY (provider_id) REFERENCES provider_profile(id),
  CONSTRAINT fk_area_city FOREIGN KEY (city_id) REFERENCES city(id)
);

CREATE INDEX IF NOT EXISTS idx_area_city ON provider_service_area(city_id);
CREATE INDEX IF NOT EXISTS idx_area_provider ON provider_service_area(provider_id);

-- Projects
CREATE TABLE IF NOT EXISTS provider_project (
  id BIGSERIAL PRIMARY KEY,
  provider_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  description TEXT,
  category_id BIGINT NOT NULL,

  city_id BIGINT,
  locality VARCHAR(120),

  budget_min INT,
  budget_max INT,

  visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_project_provider FOREIGN KEY (provider_id) REFERENCES provider_profile(id),
  CONSTRAINT fk_project_category FOREIGN KEY (category_id) REFERENCES category(id),
  CONSTRAINT fk_project_city FOREIGN KEY (city_id) REFERENCES city(id)
);

CREATE INDEX IF NOT EXISTS idx_project_provider ON provider_project(provider_id);

-- Project media
CREATE TABLE IF NOT EXISTS provider_project_media (
  id BIGSERIAL PRIMARY KEY,
  project_id BIGINT NOT NULL,
  media_type VARCHAR(10) NOT NULL,
  url TEXT NOT NULL,
  thumbnail_url TEXT,
  sort_order INT NOT NULL DEFAULT 0,

  CONSTRAINT fk_media_project FOREIGN KEY (project_id) REFERENCES provider_project(id)
);

CREATE INDEX IF NOT EXISTS idx_media_project ON provider_project_media(project_id);
