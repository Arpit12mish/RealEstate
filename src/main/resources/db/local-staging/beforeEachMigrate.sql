-- Local-staging compatibility callback only. V6 historically assumes city id
-- 1 exists, while V1 creates the city table without inserting that row. The
-- historical V12_loginHistory.sql filename is not a valid Flyway versioned
-- migration name, V13 references users even though users is first created by
-- V107, no versioned migration creates builder/company/project, V40 alters the
-- legacy home_project_analytics table that no earlier migration creates, V76
-- seeds company_stat/company_certificate without creation migrations, and V92
-- alters project_property_types without a creation migration. Recreate those
-- historical prerequisites only for a fresh disposable local-staging database.
-- This callback location is enabled only by application-local-staging.yml and
-- never participates in production Flyway runs.
DO $$
DECLARE
    city_missing BOOLEAN;
BEGIN
    IF to_regclass('public.city') IS NOT NULL THEN
        EXECUTE 'SELECT NOT EXISTS (SELECT 1 FROM city WHERE id = 1)'
            INTO city_missing;
        IF city_missing THEN
            EXECUTE $insert$
                INSERT INTO city (id, name, state, country_code)
                VALUES (1, 'Local Staging City', 'Local Staging', 'IN')
            $insert$;
            PERFORM setval(pg_get_serial_sequence('city', 'id'), 1, true);
        END IF;
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.category') IS NOT NULL
       AND to_regclass('public.home_project_analytics') IS NULL THEN
        CREATE TABLE home_project_analytics (
            id BIGSERIAL PRIMARY KEY,
            category_id BIGINT NOT NULL REFERENCES category(id)
        );
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.provider_media') IS NOT NULL THEN
        ALTER TABLE provider_media
            ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.home_section_item') IS NOT NULL THEN
        ALTER TABLE home_section_item
            ADD COLUMN IF NOT EXISTS group_key VARCHAR(50);
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.distributor') IS NOT NULL THEN
        ALTER TABLE distributor
            ADD COLUMN IF NOT EXISTS logo_url TEXT;
    END IF;
END
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'business'
          AND column_name = 'avg_rating' AND data_type = 'numeric'
    ) THEN
        ALTER TABLE business
            ALTER COLUMN avg_rating TYPE DOUBLE PRECISION
            USING avg_rating::double precision,
            ALTER COLUMN latitude TYPE DOUBLE PRECISION
            USING latitude::double precision,
            ALTER COLUMN longitude TYPE DOUBLE PRECISION
            USING longitude::double precision;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'city'
          AND column_name = 'latitude' AND data_type = 'numeric'
    ) THEN
        ALTER TABLE city
            ALTER COLUMN latitude TYPE DOUBLE PRECISION
            USING latitude::double precision,
            ALTER COLUMN longitude TYPE DOUBLE PRECISION
            USING longitude::double precision;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'city'
          AND column_name = 'growth_percent' AND data_type = 'numeric'
    ) THEN
        ALTER TABLE city
            ALTER COLUMN growth_percent TYPE DOUBLE PRECISION
            USING growth_percent::double precision;
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.app_screen_content') IS NOT NULL
       AND EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE table_schema = 'public'
             AND table_name = 'app_screen_content'
             AND column_name = 'aspect_ratio'
             AND data_type = 'numeric'
       ) THEN
        ALTER TABLE app_screen_content
            ALTER COLUMN aspect_ratio TYPE DOUBLE PRECISION
            USING aspect_ratio::double precision;
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.company_project') IS NOT NULL THEN
        ALTER TABLE company_project
            ADD COLUMN IF NOT EXISTS client_name VARCHAR(180),
            ADD COLUMN IF NOT EXISTS project_area VARCHAR(100),
            ADD COLUMN IF NOT EXISTS detail3 VARCHAR(180),
            ADD COLUMN IF NOT EXISTS tags TEXT;
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.city') IS NOT NULL
       AND to_regclass('public.company') IS NULL THEN
        CREATE TABLE company (
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

        CREATE INDEX idx_company_city ON company(city_id);
    END IF;

    IF to_regclass('public.company') IS NOT NULL THEN
        INSERT INTO company (id, name, slug, company_type)
        VALUES
            (1, 'Morphogenesis', 'local-morphogenesis', 'ARCHITECT&DESIGNERS'),
            (3, 'Urban Scape', 'local-urban-scape', 'ARCHITECT&DESIGNERS'),
            (4, 'Studio A Architects', 'local-studio-a-architects', 'ARCHITECT&DESIGNERS'),
            (5, 'DesignCraft Interiors', 'local-designcraft-interiors', 'ARCHITECT&DESIGNERS')
        ON CONFLICT (id) DO NOTHING;

        PERFORM setval(pg_get_serial_sequence('company', 'id'),
            GREATEST((SELECT max(id) FROM company), 1), true);

        IF to_regclass('public.company_stat') IS NULL THEN
            CREATE TABLE company_stat (
                id BIGSERIAL PRIMARY KEY,
                company_id BIGINT NOT NULL REFERENCES company(id),
                label VARCHAR(120) NOT NULL,
                value VARCHAR(180) NOT NULL,
                display_order INTEGER NOT NULL DEFAULT 0,
                active BOOLEAN NOT NULL DEFAULT TRUE,
                deleted BOOLEAN NOT NULL DEFAULT FALSE,
                created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
            );
            CREATE INDEX idx_company_stat_company ON company_stat(company_id);
        END IF;

        IF to_regclass('public.company_certificate') IS NULL THEN
            CREATE TABLE company_certificate (
                id BIGSERIAL PRIMARY KEY,
                company_id BIGINT NOT NULL REFERENCES company(id),
                title VARCHAR(180) NOT NULL,
                issuer VARCHAR(180),
                certificate_url TEXT,
                display_order INTEGER NOT NULL DEFAULT 0,
                active BOOLEAN NOT NULL DEFAULT TRUE,
                deleted BOOLEAN NOT NULL DEFAULT FALSE,
                created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
            );
            CREATE INDEX idx_company_certificate_company
                ON company_certificate(company_id);
        END IF;

        IF to_regclass('public.company_award') IS NULL THEN
            CREATE TABLE company_award (
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
            CREATE INDEX idx_company_award_company ON company_award(company_id);
        END IF;
    END IF;
END
$$;

DO $$
BEGIN
    IF to_regclass('public.city') IS NOT NULL
       AND to_regclass('public.builder') IS NOT NULL
       AND to_regclass('public.project') IS NULL THEN
        CREATE TABLE project (
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
            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
        );

        CREATE INDEX idx_project_builder_id ON project(builder_id);
        CREATE INDEX idx_project_city_id ON project(city_id);
    END IF;

    IF to_regclass('public.project') IS NOT NULL
       AND to_regclass('public.project_property_types') IS NULL THEN
        CREATE TABLE project_property_types (
            project_id BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
            property_type VARCHAR(30) NOT NULL
        );

        CREATE INDEX idx_project_property_types_project_id
            ON project_property_types(project_id);
    END IF;

    IF to_regclass('public.project') IS NOT NULL
       AND to_regclass('public.project_media') IS NULL THEN
        CREATE TABLE project_media (
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

        CREATE INDEX idx_proj_media_project_id ON project_media(project_id);
        CREATE INDEX idx_proj_media_active_lookup
            ON project_media(project_id, active, deleted, sort_order);
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    name VARCHAR(80),
    password VARCHAR(255),
    phone_number VARCHAR(20) NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    role VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS login_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    login_time TIMESTAMPTZ DEFAULT now(),
    device_id TEXT,
    fcm_token TEXT,
    ip_address TEXT,
    user_agent TEXT,
    login_type TEXT NOT NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

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

CREATE INDEX IF NOT EXISTS idx_feed_section_screen
    ON feed_section_config(screen);
CREATE INDEX IF NOT EXISTS idx_feed_section_category_id
    ON feed_section_config(category_id);
CREATE INDEX IF NOT EXISTS idx_feed_section_city_id
    ON feed_section_config(city_id);

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

CREATE INDEX IF NOT EXISTS idx_feed_section_item_config
    ON feed_section_item(config_id);

DO $$
BEGIN
    IF to_regclass('public.city') IS NOT NULL
       AND to_regclass('public.builder') IS NULL THEN
        CREATE TABLE builder (
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

        CREATE INDEX idx_builder_city_id ON builder(city_id);
        CREATE INDEX idx_builder_public
            ON builder(published, active, deleted);
    END IF;
END
$$;
