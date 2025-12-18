ALTER TABLE business
    ADD COLUMN whatsapp_phone   VARCHAR(20),
    ADD COLUMN open_time        TIME,
    ADD COLUMN close_time       TIME,
    ADD COLUMN established_year INT,
    ADD COLUMN highlight_badge  VARCHAR(150),
    ADD COLUMN is_top_rated     BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN is_near_and_fast BOOLEAN NOT NULL DEFAULT FALSE;
