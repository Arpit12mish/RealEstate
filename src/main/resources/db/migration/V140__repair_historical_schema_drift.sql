-- Forward-only reconciliation for installations that already have V1-V134 history.
-- Fresh installations use B134__sfs_schema_baseline.sql and then execute this
-- migration. This migration never invents fixed-ID business data and never
-- deletes application rows.

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

CREATE TABLE IF NOT EXISTS company (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL UNIQUE,
    company_type VARCHAR(40) NOT NULL,
    logo_url TEXT,
    cover_image_url TEXT,
    description TEXT,
    specialization_text VARCHAR(255),
    info_line_1 TEXT,
    info_line_2 TEXT,
    city_id BIGINT REFERENCES city(id),
    address_line TEXT,
    services_offered TEXT,
    phone VARCHAR(20),
    whatsapp VARCHAR(20),
    email VARCHAR(150),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    priority INTEGER NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE company
    ADD COLUMN IF NOT EXISTS city_id BIGINT,
    ADD COLUMN IF NOT EXISTS address_line TEXT,
    ADD COLUMN IF NOT EXISTS services_offered TEXT;

CREATE TABLE IF NOT EXISTS builder (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    logo_url VARCHAR(255),
    description TEXT,
    phone VARCHAR(20),
    whatsapp VARCHAR(20),
    email VARCHAR(150),
    address_line TEXT,
    city_id BIGINT REFERENCES city(id),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    priority INTEGER NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS project (
    id BIGSERIAL PRIMARY KEY,
    builder_id BIGINT NOT NULL REFERENCES builder(id),
    name VARCHAR(180) NOT NULL,
    slug VARCHAR(220) UNIQUE,
    description TEXT,
    city_id BIGINT REFERENCES city(id),
    address_line TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    price_min BIGINT,
    price_max BIGINT,
    monthly_emi_min BIGINT,
    monthly_emi_max BIGINT,
    average_price_per_sqft BIGINT,
    start_date DATE,
    possession_date DATE,
    rera_number VARCHAR(60),
    status VARCHAR(40),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    priority INTEGER NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    review_status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    created_by_dashboard_user_id BIGINT,
    submitted_by_dashboard_user_id BIGINT,
    submitted_at TIMESTAMPTZ,
    reviewed_by_dashboard_user_id BIGINT,
    reviewed_at TIMESTAMPTZ,
    review_remarks TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS project_property_types (
    project_id BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    property_type VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS project_media (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    media_type VARCHAR(30) NOT NULL,
    url TEXT NOT NULL,
    caption VARCHAR(200),
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS home_project_analytics (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES category(id),
    builder_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    priority INTEGER NOT NULL DEFAULT 1,
    caption VARCHAR(255)
);

ALTER TABLE home_project_analytics
    ADD COLUMN IF NOT EXISTS builder_id BIGINT,
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS priority INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS caption VARCHAR(255);

CREATE TABLE IF NOT EXISTS company_stat (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES company(id),
    label VARCHAR(120) NOT NULL,
    value VARCHAR(180) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    icon_key VARCHAR(60),
    public_visible BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE company_stat
    ADD COLUMN IF NOT EXISTS icon_key VARCHAR(60),
    ADD COLUMN IF NOT EXISTS public_visible BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS company_certificate (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES company(id),
    title VARCHAR(180) NOT NULL,
    issuer VARCHAR(180),
    certificate_url TEXT,
    description TEXT,
    certificate_file_url TEXT,
    year INTEGER,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    public_visible BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE company_certificate
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS certificate_file_url TEXT,
    ADD COLUMN IF NOT EXISTS year INTEGER,
    ADD COLUMN IF NOT EXISTS verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS public_visible BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS company_award (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES company(id),
    title VARCHAR(180) NOT NULL,
    subtitle VARCHAR(180),
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS login_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    login_time TIMESTAMPTZ NOT NULL DEFAULT now(),
    device_id TEXT,
    fcm_token TEXT,
    ip_address TEXT,
    user_agent TEXT,
    login_type TEXT NOT NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE
);

-- Login history is intentionally retained after profile deletion. The service
-- nulls this association before deleting a user, so the column must be nullable.
ALTER TABLE login_history ALTER COLUMN user_id DROP NOT NULL;

CREATE TABLE IF NOT EXISTS guest_sessions (
    id BIGSERIAL PRIMARY KEY,
    installation_id VARCHAR(120) NOT NULL UNIQUE,
    device_model VARCHAR(100),
    device_name VARCHAR(100),
    platform VARCHAR(20),
    os_version VARCHAR(30),
    app_version VARCHAR(30),
    active BOOLEAN NOT NULL,
    linked_user_id BIGINT REFERENCES users(id),
    linked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS feed_section_config (
    id BIGSERIAL PRIMARY KEY,
    screen VARCHAR(30) NOT NULL,
    category_id BIGINT,
    entity_id BIGINT,
    city_id BIGINT,
    section_type VARCHAR(80) NOT NULL,
    title VARCHAR(150),
    sort_order INTEGER NOT NULL DEFAULT 0,
    max_items INTEGER NOT NULL DEFAULT 10,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    param1 JSONB,
    param2 VARCHAR(200),
    param3 VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS feed_section_item (
    id BIGSERIAL PRIMARY KEY,
    config_id BIGINT NOT NULL REFERENCES feed_section_config(id),
    item_type VARCHAR(30) NOT NULL,
    ref_id BIGINT NOT NULL,
    title VARCHAR(150),
    subtitle VARCHAR(255),
    image_url TEXT,
    logo_url TEXT,
    group_key VARCHAR(50),
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE provider_media
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
UPDATE provider_media SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE provider_media
    ALTER COLUMN updated_at SET DEFAULT now(),
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE home_section_item
    ADD COLUMN IF NOT EXISTS group_key VARCHAR(50);

ALTER TABLE distributor
    ADD COLUMN IF NOT EXISTS logo_url TEXT;

ALTER TABLE company_project
    ADD COLUMN IF NOT EXISTS client_name VARCHAR(180),
    ADD COLUMN IF NOT EXISTS project_area VARCHAR(100),
    ADD COLUMN IF NOT EXISTS detail3 VARCHAR(180),
    ADD COLUMN IF NOT EXISTS tags TEXT;

DO $$
DECLARE
    column_record RECORD;
BEGIN
    FOR column_record IN
        SELECT *
        FROM (VALUES
            ('business', 'avg_rating'),
            ('business', 'latitude'),
            ('business', 'longitude'),
            ('city', 'latitude'),
            ('city', 'longitude'),
            ('city', 'growth_percent'),
            ('app_screen_content', 'aspect_ratio')
        ) AS expected(table_name, column_name)
    LOOP
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns c
            WHERE c.table_schema = 'public'
              AND c.table_name = column_record.table_name
              AND c.column_name = column_record.column_name
              AND c.udt_name <> 'float8'
        ) THEN
            IF NOT EXISTS (
                SELECT 1
                FROM information_schema.columns c
                WHERE c.table_schema = 'public'
                  AND c.table_name = column_record.table_name
                  AND c.column_name = column_record.column_name
                  AND c.data_type IN ('smallint', 'integer', 'bigint', 'numeric', 'real')
            ) THEN
                RAISE EXCEPTION
                    'V140 cannot safely convert %.% to double precision because its current type is not numeric',
                    column_record.table_name, column_record.column_name;
            END IF;

            EXECUTE format(
                'ALTER TABLE public.%I ALTER COLUMN %I TYPE DOUBLE PRECISION USING %I::double precision',
                column_record.table_name,
                column_record.column_name,
                column_record.column_name
            );
        END IF;
    END LOOP;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'company'::regclass
          AND contype = 'f'
          AND conkey = ARRAY[(SELECT attnum FROM pg_attribute
                              WHERE attrelid = 'company'::regclass AND attname = 'city_id')]::smallint[]
    ) THEN
        ALTER TABLE company
            ADD CONSTRAINT fk_company_city FOREIGN KEY (city_id) REFERENCES city(id);
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_company_city ON company(city_id);
CREATE INDEX IF NOT EXISTS idx_builder_city_id ON builder(city_id);
CREATE INDEX IF NOT EXISTS idx_builder_public ON builder(published, active, deleted);
CREATE INDEX IF NOT EXISTS idx_project_builder_id ON project(builder_id);
CREATE INDEX IF NOT EXISTS idx_project_city_id ON project(city_id);
CREATE INDEX IF NOT EXISTS idx_project_property_types_project_id ON project_property_types(project_id);
CREATE INDEX IF NOT EXISTS idx_proj_media_project_id ON project_media(project_id);
CREATE INDEX IF NOT EXISTS idx_proj_media_active_lookup ON project_media(project_id, active, deleted, sort_order);
CREATE INDEX IF NOT EXISTS idx_home_project_analytics_cat_builder ON home_project_analytics(category_id, builder_id);
CREATE INDEX IF NOT EXISTS idx_company_stat_company ON company_stat(company_id);
CREATE INDEX IF NOT EXISTS idx_company_certificate_company ON company_certificate(company_id);
CREATE INDEX IF NOT EXISTS idx_company_award_company ON company_award(company_id);
CREATE INDEX IF NOT EXISTS idx_feed_section_screen ON feed_section_config(screen);
CREATE INDEX IF NOT EXISTS idx_feed_section_category_id ON feed_section_config(category_id);
CREATE INDEX IF NOT EXISTS idx_feed_section_city_id ON feed_section_config(city_id);
CREATE INDEX IF NOT EXISTS idx_feed_section_item_config ON feed_section_item(config_id);
