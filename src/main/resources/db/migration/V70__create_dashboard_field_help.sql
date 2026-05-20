CREATE TABLE dashboard_field_help (
    id BIGSERIAL PRIMARY KEY,

    module VARCHAR(100) NOT NULL,
    field_key VARCHAR(150) NOT NULL,
    field_label VARCHAR(180) NOT NULL,

    short_help TEXT NOT NULL,
    detailed_help TEXT,
    why_needed TEXT,
    source_hint TEXT,
    example_value TEXT,
    validation_hint TEXT,

    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,

    created_by_dashboard_user_id BIGINT,
    updated_by_dashboard_user_id BIGINT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uq_dashboard_field_help_module_field
        UNIQUE (module, field_key),

    CONSTRAINT fk_dashboard_field_help_created_by
        FOREIGN KEY (created_by_dashboard_user_id)
        REFERENCES dashboard_users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_dashboard_field_help_updated_by
        FOREIGN KEY (updated_by_dashboard_user_id)
        REFERENCES dashboard_users(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_dashboard_field_help_module ON dashboard_field_help(module);
CREATE INDEX idx_dashboard_field_help_active ON dashboard_field_help(active);
