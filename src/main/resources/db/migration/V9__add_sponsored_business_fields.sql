-- V6__add_sponsored_business_fields.sql

ALTER TABLE business
    ADD COLUMN sponsored           BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN sponsored_priority  INT          NOT NULL DEFAULT 0,
    ADD COLUMN sponsored_start     TIMESTAMPTZ,
    ADD COLUMN sponsored_end       TIMESTAMPTZ;

-- Optional index to help sorting
CREATE INDEX idx_business_sponsored_priority
    ON business (sponsored DESC, sponsored_priority DESC);
