WITH city_project_counts AS (
  SELECT
    c.id,
    row_number() OVER (ORDER BY count(p.id) DESC, c.name ASC) AS display_rank
  FROM city c
  JOIN project p
    ON p.city_id = c.id
   AND p.published = TRUE
   AND p.active = TRUE
   AND p.deleted = FALSE
   AND p.review_status = 'APPROVED'
  WHERE c.active = TRUE
  GROUP BY c.id, c.name
)
UPDATE city c
SET homepage_featured = TRUE,
    display_order = city_project_counts.display_rank,
    updated_at = NOW()
FROM city_project_counts
WHERE c.id = city_project_counts.id
  AND c.homepage_featured = FALSE;

WITH city_cover AS (
  SELECT DISTINCT ON (c.id)
    c.id,
    pm.url
  FROM city c
  JOIN project p
    ON p.city_id = c.id
   AND p.published = TRUE
   AND p.active = TRUE
   AND p.deleted = FALSE
   AND p.review_status = 'APPROVED'
  JOIN project_media pm
    ON pm.project_id = p.id
   AND pm.active = TRUE
   AND pm.deleted = FALSE
   AND pm.media_type = 'IMAGE'
  WHERE c.active = TRUE
    AND c.homepage_featured = TRUE
    AND (c.cover_image_url IS NULL OR trim(c.cover_image_url) = '')
  ORDER BY c.id, p.priority ASC, p.id DESC, pm.sort_order ASC, pm.id ASC
)
UPDATE city c
SET cover_image_url = city_cover.url,
    updated_at = NOW()
FROM city_cover
WHERE c.id = city_cover.id;
