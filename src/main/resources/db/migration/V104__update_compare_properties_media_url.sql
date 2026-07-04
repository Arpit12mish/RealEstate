UPDATE home_section_config
SET param1      = 'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/home/comparision-animation.json',
    updated_at  = NOW()
WHERE home_category_id = 0
  AND section_type = 'COMPARE_PROPERTIES';
