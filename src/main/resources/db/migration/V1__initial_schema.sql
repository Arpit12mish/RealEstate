-- CITY MASTER
CREATE TABLE city (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    state        VARCHAR(150),
    country_code VARCHAR(10)  NOT NULL DEFAULT 'IN',
    latitude     NUMERIC(10,7),
    longitude    NUMERIC(10,7),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_city_name_state
    ON city (LOWER(name), LOWER(COALESCE(state, '')));


-- CATEGORY MASTER (HIERARCHICAL)
CREATE TABLE category (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    slug       VARCHAR(180) NOT NULL,
    parent_id  BIGINT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_category_parent
        FOREIGN KEY (parent_id) REFERENCES category (id)
);

CREATE UNIQUE INDEX uk_category_slug ON category (slug);


-- BUSINESS LISTING
CREATE TABLE business (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    primary_phone   VARCHAR(20),
    secondary_phone VARCHAR(20),
    email           VARCHAR(255),
    website         VARCHAR(255),

    address_line1   VARCHAR(255),
    address_line2   VARCHAR(255),
    landmark        VARCHAR(255),
    pincode         VARCHAR(10),

    city_id         BIGINT       NOT NULL,
    latitude        NUMERIC(10,7),
    longitude       NUMERIC(10,7),

    category_id     BIGINT       NOT NULL,

    avg_rating      NUMERIC(2,1) NOT NULL DEFAULT 0.0,
    total_ratings   INT          NOT NULL DEFAULT 0,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_business_city
        FOREIGN KEY (city_id) REFERENCES city (id),

    CONSTRAINT fk_business_category
        FOREIGN KEY (category_id) REFERENCES category (id)
);

CREATE INDEX idx_business_city      ON business (city_id);
CREATE INDEX idx_business_category  ON business (category_id);
CREATE INDEX idx_business_name_trgm ON business (LOWER(name));
