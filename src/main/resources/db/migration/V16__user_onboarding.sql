-- V16__user_onboarding.sql

ALTER TABLE users
ADD COLUMN IF NOT EXISTS onboarding_status VARCHAR(40) NOT NULL DEFAULT 'ROLE_PENDING';

ALTER TABLE users
ADD COLUMN IF NOT EXISTS role_selected_at TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_users_onboarding_status ON users(onboarding_status);
