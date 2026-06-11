-- The project_property_types_property_type_check constraint was created when
-- PropertyType only had 4 values (APARTMENT, VILLA, PLOT, COMMERCIAL).
-- The Java enum has since grown to 30+ values. Drop the stale constraint so
-- all current and future enum values can be persisted.
-- Hibernate's @Enumerated(STRING) already guarantees only valid enum names are written.

ALTER TABLE project_property_types
    DROP CONSTRAINT IF EXISTS project_property_types_property_type_check;
