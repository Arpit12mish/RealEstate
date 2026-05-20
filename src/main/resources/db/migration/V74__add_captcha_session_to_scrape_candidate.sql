-- Stores the in-memory scrape session ID so the dashboard can show
-- session expiry state and the backend can look up the live browser session.
ALTER TABLE dashboard_scrape_candidate
    ADD COLUMN IF NOT EXISTS captcha_session_id varchar(100);
