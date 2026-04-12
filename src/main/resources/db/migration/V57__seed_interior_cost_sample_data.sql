insert into interior_cost_rule
(company_id, city_name, property_type, area_unit, bhk_type, package_type, scope_type, min_area, max_area,
 base_rate_per_unit, minimum_project_cost, contingency_percent, tax_percent, active, effective_from, effective_to, source_note)
values

-- =====================================================
-- COMPANY IDS FROM YOUR DATABASE
-- 1 = Morphogenesis
-- 3 = Urban scape
-- 4 = Studio A Architects
-- 5 = DesignCraft Interiors
-- =====================================================

-- =========================
-- GURUGRAM | APARTMENT | 2 BHK | FULL_HOME
-- =========================
(1, 'Gurugram', 'APARTMENT', 'SQFT', 'TWO_BHK', 'STANDARD', 'FULL_HOME', 600, 1400, 1900.00, 850000.00, 7.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(1, 'Gurugram', 'APARTMENT', 'SQFT', 'TWO_BHK', 'PREMIUM',  'FULL_HOME', 600, 1400, 2500.00, 1200000.00, 7.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),

(3, 'Gurugram', 'APARTMENT', 'SQFT', 'TWO_BHK', 'STANDARD', 'FULL_HOME', 600, 1400, 1750.00, 750000.00, 5.50, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(3, 'Gurugram', 'APARTMENT', 'SQFT', 'TWO_BHK', 'PREMIUM',  'FULL_HOME', 600, 1400, 2200.00, 950000.00, 5.50, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),

(4, 'Gurugram', 'APARTMENT', 'SQFT', 'TWO_BHK', 'STANDARD', 'FULL_HOME', 600, 1400, 1800.00, 780000.00, 6.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(4, 'Gurugram', 'APARTMENT', 'SQFT', 'TWO_BHK', 'PREMIUM',  'FULL_HOME', 600, 1400, 2300.00, 980000.00, 6.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),

(5, 'Gurugram', 'APARTMENT', 'SQFT', 'TWO_BHK', 'STANDARD', 'FULL_HOME', 600, 1400, 1650.00, 700000.00, 5.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(5, 'Gurugram', 'APARTMENT', 'SQFT', 'TWO_BHK', 'PREMIUM',  'FULL_HOME', 600, 1400, 2100.00, 900000.00, 5.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),

-- =========================
-- GURUGRAM | APARTMENT | 3 BHK | FULL_HOME
-- =========================
(1, 'Gurugram', 'APARTMENT', 'SQFT', 'THREE_BHK', 'STANDARD', 'FULL_HOME', 1000, 2200, 1950.00, 1000000.00, 7.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(1, 'Gurugram', 'APARTMENT', 'SQFT', 'THREE_BHK', 'PREMIUM',  'FULL_HOME', 1000, 2200, 2600.00, 1400000.00, 7.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),

(3, 'Gurugram', 'APARTMENT', 'SQFT', 'THREE_BHK', 'STANDARD', 'FULL_HOME', 1000, 2200, 1780.00, 930000.00, 5.50, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(3, 'Gurugram', 'APARTMENT', 'SQFT', 'THREE_BHK', 'PREMIUM',  'FULL_HOME', 1000, 2200, 2250.00, 1180000.00, 5.50, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),

(4, 'Gurugram', 'APARTMENT', 'SQFT', 'THREE_BHK', 'STANDARD', 'FULL_HOME', 1000, 2200, 1850.00, 950000.00, 6.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(4, 'Gurugram', 'APARTMENT', 'SQFT', 'THREE_BHK', 'PREMIUM',  'FULL_HOME', 1000, 2200, 2350.00, 1220000.00, 6.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),

(5, 'Gurugram', 'APARTMENT', 'SQFT', 'THREE_BHK', 'STANDARD', 'FULL_HOME', 1000, 2200, 1700.00, 900000.00, 5.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(5, 'Gurugram', 'APARTMENT', 'SQFT', 'THREE_BHK', 'PREMIUM',  'FULL_HOME', 1000, 2200, 2150.00, 1150000.00, 5.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),

-- =========================
-- NOIDA | APARTMENT | 2 BHK | FULL_HOME
-- =========================
(1, 'Noida', 'APARTMENT', 'SQFT', 'TWO_BHK', 'STANDARD', 'FULL_HOME', 600, 1400, 1800.00, 800000.00, 7.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(1, 'Noida', 'APARTMENT', 'SQFT', 'TWO_BHK', 'PREMIUM',  'FULL_HOME', 600, 1400, 2350.00, 1100000.00, 7.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),

(3, 'Noida', 'APARTMENT', 'SQFT', 'TWO_BHK', 'STANDARD', 'FULL_HOME', 600, 1400, 1600.00, 700000.00, 5.50, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(3, 'Noida', 'APARTMENT', 'SQFT', 'TWO_BHK', 'PREMIUM',  'FULL_HOME', 600, 1400, 2050.00, 900000.00, 5.50, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),

(5, 'Noida', 'APARTMENT', 'SQFT', 'TWO_BHK', 'STANDARD', 'FULL_HOME', 600, 1400, 1500.00, 650000.00, 5.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(5, 'Noida', 'APARTMENT', 'SQFT', 'TWO_BHK', 'PREMIUM',  'FULL_HOME', 600, 1400, 1950.00, 850000.00, 5.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),

-- =========================
-- DELHI | APARTMENT | 3 BHK | FULL_HOME
-- =========================
(1, 'Delhi', 'APARTMENT', 'SQFT', 'THREE_BHK', 'STANDARD', 'FULL_HOME', 1000, 2200, 2100.00, 1150000.00, 7.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(1, 'Delhi', 'APARTMENT', 'SQFT', 'THREE_BHK', 'PREMIUM',  'FULL_HOME', 1000, 2200, 2800.00, 1500000.00, 7.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),

(4, 'Delhi', 'APARTMENT', 'SQFT', 'THREE_BHK', 'STANDARD', 'FULL_HOME', 1000, 2200, 1850.00, 950000.00, 6.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(4, 'Delhi', 'APARTMENT', 'SQFT', 'THREE_BHK', 'PREMIUM',  'FULL_HOME', 1000, 2200, 2350.00, 1200000.00, 6.00, 18.00, true, '2026-01-01', null, 'Sample dummy seed data');

insert into interior_cost_addon_rule
(company_id, city_name, package_type, addon_type, unit_price, active, effective_from, effective_to, source_note)
values

-- =========================
-- MORPHOGENESIS
-- =========================
(1, 'Gurugram', 'STANDARD', 'KITCHEN', 240000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(1, 'Gurugram', 'STANDARD', 'WARDROBE', 65000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(1, 'Gurugram', 'STANDARD', 'BATHROOM', 95000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(1, 'Gurugram', 'PREMIUM',  'KITCHEN', 320000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(1, 'Gurugram', 'PREMIUM',  'WARDROBE', 90000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(1, 'Gurugram', 'PREMIUM',  'BATHROOM', 125000.00, true, '2026-01-01', null, 'Sample dummy seed data'),

(1, 'Noida', 'STANDARD', 'KITCHEN', 220000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(1, 'Noida', 'STANDARD', 'WARDROBE', 60000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(1, 'Noida', 'STANDARD', 'BATHROOM', 90000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(1, 'Noida', 'PREMIUM',  'KITCHEN', 295000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(1, 'Noida', 'PREMIUM',  'WARDROBE', 85000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(1, 'Noida', 'PREMIUM',  'BATHROOM', 120000.00, true, '2026-01-01', null, 'Sample dummy seed data'),

-- =========================
-- URBAN SCAPE
-- =========================
(3, 'Gurugram', 'STANDARD', 'KITCHEN', 190000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(3, 'Gurugram', 'STANDARD', 'WARDROBE', 50000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(3, 'Gurugram', 'STANDARD', 'BATHROOM', 70000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(3, 'Gurugram', 'PREMIUM',  'KITCHEN', 250000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(3, 'Gurugram', 'PREMIUM',  'WARDROBE', 68000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(3, 'Gurugram', 'PREMIUM',  'BATHROOM', 95000.00, true, '2026-01-01', null, 'Sample dummy seed data'),

(3, 'Noida', 'STANDARD', 'KITCHEN', 175000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(3, 'Noida', 'STANDARD', 'WARDROBE', 47000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(3, 'Noida', 'STANDARD', 'BATHROOM', 65000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(3, 'Noida', 'PREMIUM',  'KITCHEN', 235000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(3, 'Noida', 'PREMIUM',  'WARDROBE', 65000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(3, 'Noida', 'PREMIUM',  'BATHROOM', 90000.00, true, '2026-01-01', null, 'Sample dummy seed data'),

-- =========================
-- STUDIO A ARCHITECTS
-- =========================
(4, 'Gurugram', 'STANDARD', 'KITCHEN', 200000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(4, 'Gurugram', 'STANDARD', 'WARDROBE', 52000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(4, 'Gurugram', 'STANDARD', 'BATHROOM', 75000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(4, 'Gurugram', 'PREMIUM',  'KITCHEN', 265000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(4, 'Gurugram', 'PREMIUM',  'WARDROBE', 72000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(4, 'Gurugram', 'PREMIUM',  'BATHROOM', 98000.00, true, '2026-01-01', null, 'Sample dummy seed data'),

-- =========================
-- DESIGNCRAFT INTERIORS
-- =========================
(5, 'Gurugram', 'STANDARD', 'KITCHEN', 180000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(5, 'Gurugram', 'STANDARD', 'WARDROBE', 45000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(5, 'Gurugram', 'STANDARD', 'BATHROOM', 65000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(5, 'Gurugram', 'PREMIUM',  'KITCHEN', 240000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(5, 'Gurugram', 'PREMIUM',  'WARDROBE', 65000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(5, 'Gurugram', 'PREMIUM',  'BATHROOM', 90000.00, true, '2026-01-01', null, 'Sample dummy seed data'),

(5, 'Noida', 'STANDARD', 'KITCHEN', 170000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(5, 'Noida', 'STANDARD', 'WARDROBE', 42000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(5, 'Noida', 'STANDARD', 'BATHROOM', 60000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(5, 'Noida', 'PREMIUM',  'KITCHEN', 225000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(5, 'Noida', 'PREMIUM',  'WARDROBE', 62000.00, true, '2026-01-01', null, 'Sample dummy seed data'),
(5, 'Noida', 'PREMIUM',  'BATHROOM', 85000.00, true, '2026-01-01', null, 'Sample dummy seed data');