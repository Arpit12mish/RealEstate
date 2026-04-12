INSERT INTO app_content_page (slug, title, content, content_type, active)
VALUES
('privacy-policy', 'Privacy Policy', 'Add your privacy policy content here.', 'PLAIN_TEXT', TRUE),
('terms-of-use', 'Terms of Use', 'Add your terms of use content here.', 'PLAIN_TEXT', TRUE),
('about-us', 'About Us', 'Add your about us content here.', 'PLAIN_TEXT', TRUE);

INSERT INTO app_setting (setting_key, setting_value, description, active)
VALUES
('share_android_url', 'https://play.google.com/store', 'Android app share link', TRUE),
('share_ios_url', 'https://apps.apple.com/', 'iOS app share link', TRUE),
('contact_email', 'support@example.com', 'Support email', TRUE),
('contact_phone', '+91XXXXXXXXXX', 'Support phone number', TRUE),
('contact_whatsapp', '+91XXXXXXXXXX', 'Support WhatsApp number', TRUE);