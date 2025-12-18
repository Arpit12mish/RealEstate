-- PROMO BANNERS PER CATEGORY

CREATE TABLE promo_banner (
    id           BIGSERIAL PRIMARY KEY,
    category_id  BIGINT       NOT NULL,

    title        VARCHAR(150) NOT NULL,
    subtitle     VARCHAR(255),
    image_url    VARCHAR(500) NOT NULL,       -- CDN or dummy URL
    target_url   VARCHAR(500),                -- where to open on click

    priority     INT          NOT NULL DEFAULT 0,  -- lower = higher priority
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,

    start_at     TIMESTAMPTZ,
    end_at       TIMESTAMPTZ,

    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_banner_category
        FOREIGN KEY (category_id) REFERENCES category (id)
);

CREATE INDEX idx_banner_category ON promo_banner (category_id);
CREATE INDEX idx_banner_active   ON promo_banner (is_active);

-- DUMMY DATA (replace URLs later with real CDN links)
INSERT INTO promo_banner (category_id, title, subtitle, image_url, target_url, priority)
VALUES
-- Category 2: Real Estate Services
(2,
 'Find Top Builders in Delhi NCR',
 'New launches • RERA approved • Verified listings',
 'https://dummy-cdn.example.com/banners/real-estate-services-1.jpg',
 'https://app.example.com/cat/real-estate-services',
 1),

(14,
 'Book Site Visit for New Projects',
 'Schedule a visit with trusted builders',
 'https://dummy-cdn.example.com/banners/builders-projects-1.jpg',
 'https://app.example.com/cat/builders-projects-brokers',
 1),

-- Category 3: Home Décor & Interior
(3,
 'Get Flat 20% Off on Interior Design Consultation',
 'Limited period offer with top interior designers',
 'https://dummy-cdn.example.com/banners/interior-1.jpg',
 'https://app.example.com/cat/home-decor-interior',
 1),

(16,
 'Premium Interior Designers in Your City',
 'Concept to completion – turnkey projects',
 'https://dummy-cdn.example.com/banners/interiors-2.jpg',
 'https://app.example.com/cat/interior-designers',
 2),

-- Category 11: Home Repair & Maintenance
(11,
 'One-stop Home Repair Services',
 'Electrician • Plumber • Carpenter • AC repair',
 'https://dummy-cdn.example.com/banners/home-repair-1.jpg',
 'https://app.example.com/cat/home-repair-maintenance',
 1);
