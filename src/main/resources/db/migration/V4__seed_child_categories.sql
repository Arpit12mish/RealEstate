-- Block 1: Real Estate Services
WITH block AS (
    SELECT id FROM category WHERE slug = 'real-estate-services'
)
INSERT INTO category (name, slug, parent_id, priority)
SELECT
    'Builders / New Launch / Top Projects / Top Brokers',
    'builders-projects-brokers',
    block.id,
    1
FROM block
UNION ALL
SELECT 'Architects', 'architects', block.id, 2 FROM block
UNION ALL
SELECT 'Interior Designers', 'interior-designers', block.id, 3 FROM block;

-- Block 2: Home Décor & Interior
WITH block AS (
    SELECT id FROM category WHERE slug = 'home-decor-interior'
)
INSERT INTO category (name, slug, parent_id, priority)
SELECT 'Home and Kitchenware', 'home-kitchenware', block.id, 1 FROM block
UNION ALL
SELECT 'Furnishings', 'furnishings', block.id, 2 FROM block
UNION ALL
SELECT 'Upholstery', 'upholstery', block.id, 3 FROM block;

-- Block 3: Electrical & Automation
WITH block AS (
    SELECT id FROM category WHERE slug = 'electrical-automation'
)
INSERT INTO category (name, slug, parent_id, priority)
SELECT 'Lights', 'lights', block.id, 1 FROM block
UNION ALL
SELECT 'Electrical', 'electrical', block.id, 2 FROM block
UNION ALL
SELECT 'Consumer Durables', 'consumer-durables', block.id, 3 FROM block
UNION ALL
SELECT 'Digital Locks & Safes', 'digital-locks-safes', block.id, 4 FROM block
UNION ALL
SELECT 'Automation', 'automation', block.id, 5 FROM block;
