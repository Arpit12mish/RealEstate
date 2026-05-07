-- ============================================================
-- INTERIOR COST COVERAGE MATRIX V2
-- Broad seed for Delhi, Gurugram, Noida
-- Covers residential + office combinations
-- Uses 4 real companies:
-- 1 = Morphogenesis
-- 3 = Urban scape
-- 4 = Studio A Architects
-- 5 = DesignCraft Interiors
-- ============================================================

-- ------------------------------------------------------------
-- 1. Clean previous sample/dummy interior-cost seed
-- ------------------------------------------------------------
delete from interior_cost_addon_rule
where source_note like 'Sample dummy seed data%'
   or source_note like 'Coverage seed v2%';

delete from interior_cost_rule
where source_note like 'Sample dummy seed data%'
   or source_note like 'Coverage seed v2%';

-- ------------------------------------------------------------
-- 2. Residential coverage matrix
-- ------------------------------------------------------------
with company_factor(company_id, rate_mult, contingency_pct) as (
    values
        (1, 1.28::numeric, 7.00::numeric),   -- Morphogenesis
        (3, 1.08::numeric, 5.50::numeric),   -- Urban scape
        (4, 1.15::numeric, 6.00::numeric),   -- Studio A Architects
        (5, 1.00::numeric, 5.00::numeric)    -- DesignCraft Interiors
),
city_package(city_name, package_type, base_rate, package_min_project) as (
    values
        ('Delhi',     'BASIC',    1400::numeric,  350000::numeric),
        ('Delhi',     'STANDARD', 2200::numeric,  700000::numeric),
        ('Delhi',     'PREMIUM',  3200::numeric, 1250000::numeric),
        ('Delhi',     'LUXURY',   5200::numeric, 2500000::numeric),

        ('Gurugram',  'BASIC',    1300::numeric,  300000::numeric),
        ('Gurugram',  'STANDARD', 2100::numeric,  650000::numeric),
        ('Gurugram',  'PREMIUM',  3000::numeric, 1150000::numeric),
        ('Gurugram',  'LUXURY',   5000::numeric, 2300000::numeric),

        ('Noida',     'BASIC',    1200::numeric,  250000::numeric),
        ('Noida',     'STANDARD', 1900::numeric,  550000::numeric),
        ('Noida',     'PREMIUM',  2800::numeric, 1000000::numeric),
        ('Noida',     'LUXURY',   4600::numeric, 2000000::numeric)
),
property_factor(property_type, property_mult) as (
    values
        ('APARTMENT',      1.00::numeric),
        ('BUILDER_FLOOR',  1.08::numeric),
        ('VILLA',          1.20::numeric)
),
bhk_factor(bhk_type, bhk_mult) as (
    values
        ('STUDIO',         0.88::numeric),
        ('ONE_BHK',        0.94::numeric),
        ('TWO_BHK',        1.00::numeric),
        ('THREE_BHK',      1.08::numeric),
        ('FOUR_BHK_PLUS',  1.18::numeric)
),
scope_factor(scope_type, scope_mult, min_project_mult) as (
    values
        ('FULL_HOME',    1.00::numeric, 1.00::numeric),
        ('ROOMS_ONLY',   0.92::numeric, 0.75::numeric),
        ('KITCHEN_ONLY', 1.10::numeric, 0.45::numeric)
),
res_bands(min_area, max_area, band_mult) as (
    values
        (300::numeric,   699::numeric, 1.18::numeric),
        (700::numeric,   950::numeric, 1.10::numeric),
        (951::numeric,  1400::numeric, 1.00::numeric),
        (1401::numeric, 2200::numeric, 0.94::numeric),
        (2201::numeric, 4000::numeric, 0.90::numeric),
        (4001::numeric, 8000::numeric, 0.86::numeric)
)
insert into interior_cost_rule (
    company_id,
    city_name,
    property_type,
    area_unit,
    bhk_type,
    package_type,
    scope_type,
    min_area,
    max_area,
    base_rate_per_unit,
    minimum_project_cost,
    contingency_percent,
    tax_percent,
    active,
    effective_from,
    effective_to,
    source_note
)
select
    c.company_id,
    cp.city_name,
    pf.property_type,
    'SQFT',
    bf.bhk_type,
    cp.package_type,
    sf.scope_type,
    rb.min_area,
    rb.max_area,
    round((cp.base_rate * c.rate_mult * pf.property_mult * bf.bhk_mult * sf.scope_mult * rb.band_mult)::numeric, 2),
    round((cp.package_min_project * pf.property_mult * sf.min_project_mult)::numeric, 2),
    c.contingency_pct,
    18.00,
    true,
    date '2026-01-01',
    null,
    'Coverage seed v2 - residential'
from company_factor c
cross join city_package cp
cross join property_factor pf
cross join bhk_factor bf
cross join scope_factor sf
cross join res_bands rb;

-- ------------------------------------------------------------
-- 3. Office coverage matrix
-- ------------------------------------------------------------
with company_factor(company_id, rate_mult, contingency_pct) as (
    values
        (1, 1.28::numeric, 7.00::numeric),
        (3, 1.08::numeric, 5.50::numeric),
        (4, 1.15::numeric, 6.00::numeric),
        (5, 1.00::numeric, 5.00::numeric)
),
city_package(city_name, package_type, base_rate, package_min_project) as (
    values
        ('Delhi',     'BASIC',    1200::numeric,  450000::numeric),
        ('Delhi',     'STANDARD', 1800::numeric,  900000::numeric),
        ('Delhi',     'PREMIUM',  2600::numeric, 1500000::numeric),
        ('Delhi',     'LUXURY',   4200::numeric, 3000000::numeric),

        ('Gurugram',  'BASIC',    1100::numeric,  400000::numeric),
        ('Gurugram',  'STANDARD', 1700::numeric,  850000::numeric),
        ('Gurugram',  'PREMIUM',  2400::numeric, 1400000::numeric),
        ('Gurugram',  'LUXURY',   3900::numeric, 2800000::numeric),

        ('Noida',     'BASIC',    1000::numeric,  350000::numeric),
        ('Noida',     'STANDARD', 1600::numeric,  750000::numeric),
        ('Noida',     'PREMIUM',  2300::numeric, 1300000::numeric),
        ('Noida',     'LUXURY',   3600::numeric, 2600000::numeric)
),
office_scope(scope_type, scope_mult, min_project_mult) as (
    values
        ('OFFICE_INTERIOR', 1.00::numeric, 1.00::numeric)
),
office_bhk(bhk_type, bhk_mult) as (
    values
        ('OFFICE_OPEN_PLAN', 1.00::numeric)
),
office_bands(min_area, max_area, band_mult) as (
    values
        (500::numeric,   1500::numeric, 1.08::numeric),
        (1501::numeric,  3000::numeric, 1.00::numeric),
        (3001::numeric,  8000::numeric, 0.92::numeric),
        (8001::numeric, 20000::numeric, 0.86::numeric)
)
insert into interior_cost_rule (
    company_id,
    city_name,
    property_type,
    area_unit,
    bhk_type,
    package_type,
    scope_type,
    min_area,
    max_area,
    base_rate_per_unit,
    minimum_project_cost,
    contingency_percent,
    tax_percent,
    active,
    effective_from,
    effective_to,
    source_note
)
select
    c.company_id,
    cp.city_name,
    'OFFICE',
    'SQFT',
    ob.bhk_type,
    cp.package_type,
    os.scope_type,
    oband.min_area,
    oband.max_area,
    round((cp.base_rate * c.rate_mult * ob.bhk_mult * os.scope_mult * oband.band_mult)::numeric, 2),
    round((cp.package_min_project * os.min_project_mult)::numeric, 2),
    c.contingency_pct,
    18.00,
    true,
    date '2026-01-01',
    null,
    'Coverage seed v2 - office'
from company_factor c
cross join city_package cp
cross join office_scope os
cross join office_bhk ob
cross join office_bands oband;

-- ------------------------------------------------------------
-- 4. Add-on pricing for all 4 plans, 3 cities, 4 companies
-- ------------------------------------------------------------
with company_factor(company_id, rate_mult) as (
    values
        (1, 1.28::numeric),
        (3, 1.08::numeric),
        (4, 1.15::numeric),
        (5, 1.00::numeric)
),
city_factor(city_name, city_mult) as (
    values
        ('Delhi',    1.10::numeric),
        ('Gurugram', 1.00::numeric),
        ('Noida',    0.92::numeric)
),
addon_base(package_type, addon_type, base_price) as (
    values
        ('BASIC',    'KITCHEN',  120000::numeric),
        ('BASIC',    'WARDROBE',  30000::numeric),
        ('BASIC',    'BATHROOM',  45000::numeric),

        ('STANDARD', 'KITCHEN',  180000::numeric),
        ('STANDARD', 'WARDROBE',  45000::numeric),
        ('STANDARD', 'BATHROOM',  65000::numeric),

        ('PREMIUM',  'KITCHEN',  260000::numeric),
        ('PREMIUM',  'WARDROBE',  70000::numeric),
        ('PREMIUM',  'BATHROOM', 100000::numeric),

        ('LUXURY',   'KITCHEN',  420000::numeric),
        ('LUXURY',   'WARDROBE', 125000::numeric),
        ('LUXURY',   'BATHROOM', 170000::numeric)
)
insert into interior_cost_addon_rule (
    company_id,
    city_name,
    package_type,
    addon_type,
    unit_price,
    active,
    effective_from,
    effective_to,
    source_note
)
select
    cf.company_id,
    city.city_name,
    ab.package_type,
    ab.addon_type,
    round((ab.base_price * city.city_mult * cf.rate_mult)::numeric, 2),
    true,
    date '2026-01-01',
    null,
    'Coverage seed v2 - addon'
from company_factor cf
cross join city_factor city
cross join addon_base ab;