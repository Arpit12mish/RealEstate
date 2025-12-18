CREATE TABLE business_event (
    id              BIGSERIAL PRIMARY KEY,
    business_id     BIGINT      NOT NULL REFERENCES business(id),
    city_id         BIGINT      REFERENCES city(id),
    category_id     BIGINT      REFERENCES category(id),

    event_type      VARCHAR(50) NOT NULL,   -- CARD_VIEW, CALL_CLICK, etc.
    source          VARCHAR(50),            -- SEARCH, CATEGORY_PAGE, BANNER
    listing_position INT,                   -- position in the list (1-based)

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_business_event_business ON business_event (business_id);
CREATE INDEX idx_business_event_type     ON business_event (event_type);
CREATE INDEX idx_business_event_created  ON business_event (created_at);
