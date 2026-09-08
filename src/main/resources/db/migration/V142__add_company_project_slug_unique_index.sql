DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM (
            SELECT slug
            FROM company_project
            WHERE slug IS NOT NULL
            GROUP BY slug
            HAVING COUNT(*) > 1
        ) duplicate_slugs
    ) THEN
        RAISE EXCEPTION
            'Cannot enforce company_project slug uniqueness: duplicate slugs exist';
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_company_project_slug
    ON company_project (slug);
