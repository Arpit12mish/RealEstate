-- V96: Add pg_trgm GIN indexes for case-insensitive LIKE search performance.
--
-- Context: public search uses lower(name) LIKE lower('%query%') patterns across
-- project, builder, business, and company tables. Without trigram indexes these
-- degrade to full sequential scans as row counts grow.
--
-- All indexes are on lower(column) to match the exact LIKE predicate used in
-- JPQL queries (lower(p.name) like lower(concat('%', :query, '%'))).
-- PostgreSQL's pg_trgm GIN operator class supports leading-wildcard LIKE,
-- which a standard B-tree index cannot.
--
-- Safe to re-run: all statements use IF NOT EXISTS.
-- Does not drop or modify any existing index.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- project.name — searched in ProjectSearchRepository and ProjectRepository
CREATE INDEX IF NOT EXISTS idx_project_name_trgm
    ON project USING gin (lower(name) gin_trgm_ops);

-- builder.name — searched in BuilderSearchRepository; also joined in project search
CREATE INDEX IF NOT EXISTS idx_builder_name_trgm
    ON builder USING gin (lower(name) gin_trgm_ops);

-- business.name — searched via NameContainingIgnoreCase derived queries
CREATE INDEX IF NOT EXISTS idx_business_name_trgm
    ON business USING gin (lower(name) gin_trgm_ops);

-- company.name + company.company_type — both searched in CompanySearchRepository
-- Wrapped in DO blocks: CREATE INDEX requires table ownership; skip gracefully if not owner.
DO $$
BEGIN
    CREATE INDEX IF NOT EXISTS idx_company_name_trgm
        ON company USING gin (lower(name) gin_trgm_ops);
EXCEPTION WHEN insufficient_privilege THEN
    RAISE WARNING 'Skipping idx_company_name_trgm: run "ALTER TABLE company OWNER TO <app_user>;" as superuser first.';
END
$$;

DO $$
BEGIN
    CREATE INDEX IF NOT EXISTS idx_company_type_trgm
        ON company USING gin (lower(company_type) gin_trgm_ops);
EXCEPTION WHEN insufficient_privilege THEN
    RAISE WARNING 'Skipping idx_company_type_trgm: run "ALTER TABLE company OWNER TO <app_user>;" as superuser first.';
END
$$;
