-- V5__seed_missing_categories.sql
-- Add detailed subcategories under existing blocks.

-- ===============================
-- 1) HOME REPAIR & MAINTENANCE (parent_id = 11)
-- ===============================
INSERT INTO category (name, slug, parent_id, priority)
VALUES
    ('Plumbers',                 'plumbers',                 11, 1),
    ('Carpenters',               'carpenters',               11, 2),
    ('Electricians',             'electricians',             11, 3),
    ('Masons',                   'masons',                   11, 4),
    ('Painters',                 'painters',                 11, 5),
    ('Welders',                  'welders',                  11, 6),
    ('AC Repair & Services',     'ac-repair-services',       11, 7),
    ('RO Repair Services',       'ro-repair-services',       11, 8),
    ('CCTV Installation',        'cctv-installation',        11, 9),
    ('Appliances Repair',        'appliances-repair',        11, 10),
    ('Home Deep Cleaning',       'home-cleaning',            11, 11),
    ('Pest Control',             'pest-control',             11, 12);


-- ===============================
-- 2) FLOORING & TILES (parent_id = 7)
-- ===============================
INSERT INTO category (name, slug, parent_id, priority)
VALUES
    ('Tiles & Flooring',         'tiles-flooring',           7, 1),
    ('Marble & Granite',         'marble-granite',           7, 2),
    ('Wooden Flooring',          'wooden-flooring',          7, 3),
    ('Carpet Flooring',          'carpet-flooring',          7, 4),
    ('Industrial Flooring',      'industrial-flooring',      7, 5);


-- ===============================
-- 3) MOVING & LOGISTICS (parent_id = 12)
-- ===============================
INSERT INTO category (name, slug, parent_id, priority)
VALUES
    ('Movers & Packers',         'movers-packers',           12, 1),
    ('Truck Transport Services', 'truck-transport',          12, 2),
    ('Labor & Loading Services', 'labor-loading',            12, 3),
    ('Courier Services',         'courier-services',         12, 4),
    ('Household Shifting',       'household-shifting',       12, 5);


-- ===============================
-- 4) HOME SERVICES & UTILITIES (parent_id = 13)
-- ===============================
INSERT INTO category (name, slug, parent_id, priority)
VALUES
    ('Water Tank Cleaning',      'water-tank-cleaning',      13, 1),
    ('Gas Pipeline Services',    'gas-pipeline-services',    13, 2),
    ('Solar Panel Installation', 'solar-installation',       13, 3),
    ('Generator Services',       'generator-services',       13, 4),
    ('Water Proofing',           'waterproofing',            13, 5),
    ('Interior Deep Cleaning',   'deep-cleaning',            13, 6);


-- ===============================
-- 5) SANITARY & BATH (parent_id = 6)
-- ===============================
INSERT INTO category (name, slug, parent_id, priority)
VALUES
    ('Bathroom Fittings',        'bathroom-fittings',        6, 1),
    ('Water Heaters',            'water-heaters',            6, 2),
    ('Plumbing Hardware',        'plumbing-hardware',        6, 3),
    ('Water Tanks',              'water-tanks',              6, 4);


-- ===============================
-- 6) CONSTRUCTION MATERIALS (parent_id = 10)
-- ===============================
INSERT INTO category (name, slug, parent_id, priority)
VALUES
    ('Cement',                   'cement',                   10, 1),
    ('Bricks & Blocks',          'bricks-blocks',            10, 2),
    ('Sand & Aggregates',        'sand-aggregates',          10, 3),
    ('Steel & Rods',             'steel-rods',               10, 4),
    ('Ready Mix Concrete',       'ready-mix-concrete',       10, 5);
