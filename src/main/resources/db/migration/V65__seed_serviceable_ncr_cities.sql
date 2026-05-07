WITH seed_city(name, state, country_code, latitude, longitude) AS (
    VALUES
        ('New Delhi', 'Delhi', 'IN', 28.6139, 77.2090),
        ('Delhi', 'Delhi', 'IN', 28.7041, 77.1025),

        ('Noida', 'Uttar Pradesh', 'IN', 28.5355, 77.3910),
        ('Greater Noida', 'Uttar Pradesh', 'IN', 28.4744, 77.5040),
        ('Greater Noida West', 'Uttar Pradesh', 'IN', 28.6080, 77.4260),
        ('Ghaziabad', 'Uttar Pradesh', 'IN', 28.6692, 77.4538),

        ('Gurugram', 'Haryana', 'IN', 28.4595, 77.0266),
        ('Faridabad', 'Haryana', 'IN', 28.4089, 77.3178),
        ('Sonipat', 'Haryana', 'IN', 28.9931, 77.0151),
        ('Bahadurgarh', 'Haryana', 'IN', 28.6924, 76.9239),

        ('Bhiwadi', 'Rajasthan', 'IN', 28.2102, 76.8606)
)
INSERT INTO city (
    name,
    state,
    country_code,
    latitude,
    longitude,
    created_at,
    updated_at
)
SELECT
    s.name,
    s.state,
    s.country_code,
    s.latitude,
    s.longitude,
    NOW(),
    NOW()
FROM seed_city s
WHERE NOT EXISTS (
    SELECT 1
    FROM city c
    WHERE LOWER(c.name) = LOWER(s.name)
      AND LOWER(COALESCE(c.state, '')) = LOWER(COALESCE(s.state, ''))
);

WITH seed_city(name, state, latitude, longitude) AS (
    VALUES
        ('New Delhi', 'Delhi', 28.6139, 77.2090),
        ('Delhi', 'Delhi', 28.7041, 77.1025),

        ('Noida', 'Uttar Pradesh', 28.5355, 77.3910),
        ('Greater Noida', 'Uttar Pradesh', 28.4744, 77.5040),
        ('Greater Noida West', 'Uttar Pradesh', 28.6080, 77.4260),
        ('Ghaziabad', 'Uttar Pradesh', 28.6692, 77.4538),

        ('Gurugram', 'Haryana', 28.4595, 77.0266),
        ('Faridabad', 'Haryana', 28.4089, 77.3178),
        ('Sonipat', 'Haryana', 28.9931, 77.0151),
        ('Bahadurgarh', 'Haryana', 28.6924, 76.9239),

        ('Bhiwadi', 'Rajasthan', 28.2102, 76.8606)
)
UPDATE city c
SET latitude = s.latitude,
    longitude = s.longitude,
    updated_at = NOW()
FROM seed_city s
WHERE LOWER(c.name) = LOWER(s.name)
  AND LOWER(COALESCE(c.state, '')) = LOWER(COALESCE(s.state, ''));