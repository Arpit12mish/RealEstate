ALTER TABLE featured_carousel_config
ADD CONSTRAINT uq_featured_carousel_unique
UNIQUE (category_id, city_id, position);
