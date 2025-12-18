CREATE TABLE login_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    login_time TIMESTAMPTZ DEFAULT NOW(),
    device_id TEXT,
    fcm_token TEXT,
    ip_address TEXT,
    user_agent TEXT,
    login_type TEXT NOT NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);
