insert into stamp_duty_rule
(state_name, city_name, buyer_type, property_category, stamp_duty_percent, registration_percent, local_body_tax_percent, effective_from, effective_to, active, source_note)
values

-- =========================
-- NOIDA
-- =========================
('Uttar Pradesh', 'Noida', 'MALE',   'RESIDENTIAL', 7.00, 1.00, 0.00, '2026-01-01', null, true, 'Sample dummy seed data'),
('Uttar Pradesh', 'Noida', 'FEMALE', 'RESIDENTIAL', 6.00, 1.00, 0.00, '2026-01-01', null, true, 'Sample dummy seed data'),
('Uttar Pradesh', 'Noida', 'JOINT',  'RESIDENTIAL', 6.50, 1.00, 0.00, '2026-01-01', null, true, 'Sample dummy seed data'),

('Uttar Pradesh', 'Noida', 'MALE',   'COMMERCIAL', 7.50, 1.00, 0.50, '2026-01-01', null, true, 'Sample dummy seed data'),
('Uttar Pradesh', 'Noida', 'FEMALE', 'COMMERCIAL', 7.25, 1.00, 0.50, '2026-01-01', null, true, 'Sample dummy seed data'),
('Uttar Pradesh', 'Noida', 'JOINT',  'COMMERCIAL', 7.35, 1.00, 0.50, '2026-01-01', null, true, 'Sample dummy seed data'),

-- =========================
-- GURUGRAM
-- =========================
('Haryana', 'Gurugram', 'MALE',   'RESIDENTIAL', 6.00, 1.00, 0.00, '2026-01-01', null, true, 'Sample dummy seed data'),
('Haryana', 'Gurugram', 'FEMALE', 'RESIDENTIAL', 5.00, 1.00, 0.00, '2026-01-01', null, true, 'Sample dummy seed data'),
('Haryana', 'Gurugram', 'JOINT',  'RESIDENTIAL', 5.50, 1.00, 0.00, '2026-01-01', null, true, 'Sample dummy seed data'),

('Haryana', 'Gurugram', 'MALE',   'COMMERCIAL', 7.00, 1.00, 0.50, '2026-01-01', null, true, 'Sample dummy seed data'),
('Haryana', 'Gurugram', 'FEMALE', 'COMMERCIAL', 6.75, 1.00, 0.50, '2026-01-01', null, true, 'Sample dummy seed data'),
('Haryana', 'Gurugram', 'JOINT',  'COMMERCIAL', 6.85, 1.00, 0.50, '2026-01-01', null, true, 'Sample dummy seed data'),

-- =========================
-- DELHI
-- =========================
('Delhi', 'Delhi', 'MALE',   'RESIDENTIAL', 6.00, 1.00, 0.00, '2026-01-01', null, true, 'Sample dummy seed data'),
('Delhi', 'Delhi', 'FEMALE', 'RESIDENTIAL', 4.00, 1.00, 0.00, '2026-01-01', null, true, 'Sample dummy seed data'),
('Delhi', 'Delhi', 'JOINT',  'RESIDENTIAL', 5.00, 1.00, 0.00, '2026-01-01', null, true, 'Sample dummy seed data'),

('Delhi', 'Delhi', 'MALE',   'COMMERCIAL', 6.50, 1.00, 0.50, '2026-01-01', null, true, 'Sample dummy seed data'),
('Delhi', 'Delhi', 'FEMALE', 'COMMERCIAL', 6.25, 1.00, 0.50, '2026-01-01', null, true, 'Sample dummy seed data'),
('Delhi', 'Delhi', 'JOINT',  'COMMERCIAL', 6.35, 1.00, 0.50, '2026-01-01', null, true, 'Sample dummy seed data');