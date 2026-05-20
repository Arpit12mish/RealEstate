CREATE TABLE dashboard_action_audit (
    id                  BIGSERIAL PRIMARY KEY,
    dashboard_user_id   BIGINT,
    dashboard_user_name VARCHAR(150),
    dashboard_user_role VARCHAR(50),
    action              VARCHAR(80)  NOT NULL,
    entity_type         VARCHAR(80)  NOT NULL,
    entity_id           BIGINT       NOT NULL,
    project_id          BIGINT,
    summary             TEXT,
    ip_address          VARCHAR(100),
    user_agent          TEXT,
    created_at          TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_dashboard_action_audit_user_id    ON dashboard_action_audit (dashboard_user_id);
CREATE INDEX idx_dashboard_action_audit_entity     ON dashboard_action_audit (entity_type, entity_id);
CREATE INDEX idx_dashboard_action_audit_project_id ON dashboard_action_audit (project_id);
CREATE INDEX idx_dashboard_action_audit_action     ON dashboard_action_audit (action);
CREATE INDEX idx_dashboard_action_audit_created_at ON dashboard_action_audit (created_at);
