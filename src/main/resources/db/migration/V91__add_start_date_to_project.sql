-- V91: Add start_date to project table
-- Represents when construction / project launch began.
-- Nullable; existing rows default to NULL.

ALTER TABLE project
    ADD COLUMN IF NOT EXISTS start_date DATE;
