ALTER TABLE content_post
    ADD COLUMN reading_time_minutes INTEGER,
    ADD CONSTRAINT chk_content_post_reading_time CHECK (
        reading_time_minutes IS NULL OR reading_time_minutes BETWEEN 1 AND 180
    );

ALTER TABLE content_post_revision
    ADD COLUMN reading_time_minutes INTEGER,
    ADD CONSTRAINT chk_content_revision_reading_time CHECK (
        reading_time_minutes IS NULL OR reading_time_minutes BETWEEN 1 AND 180
    );
