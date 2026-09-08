-- Phase 7B-G: stabilize the Company canonical-slug contract.
-- Amended Phase 7B-GA (migration safety audit) - see that phase's own
-- report for the full before/after reasoning; summarized inline below.
--
-- 1) Adds company.ever_published, a permanent (never-reset) marker of
--    whether a company has been published at least once. Distinct from the
--    existing `published` flag, which is freely toggled back and forth -
--    `ever_published` is what DashboardCompanyServiceImpl now checks before
--    allowing a slug edit, so an unpublish -> edit-slug -> republish cycle
--    can never bypass published-slug immutability (RISK-025).
--
--    Backfill policy for pre-existing rows (AMENDED - the original version
--    of this migration set ever_published only from the CURRENT `published`
--    value, which is unsafe: a company published in the past and later
--    unpublished before this migration ever ran would have been
--    incorrectly marked ever_published = false, leaving its slug editable
--    even though it was genuinely once public). This codebase has no
--    audit/history table wired to Company publish/unpublish transitions at
--    all (`DashboardActionAuditEntity` exists but `DashboardCompanyServiceImpl`
--    never writes to it - confirmed via a full grep for any Company write
--    path), so there is no reliable way to distinguish "genuinely always a
--    draft" from "was public once, then unpublished" for any row that
--    predates this migration. Per the conservative policy this demands:
--    EVERY pre-existing row - regardless of its current published, active,
--    or deleted state - is marked ever_published = true unconditionally.
--    A currently-published company was obviously public. A currently-
--    unpublished, currently-inactive, or currently-deleted company MAY
--    have been public before and cannot be proven otherwise, so it is
--    treated the same way. The accepted cost: a company that was truly
--    always a draft before this migration also gets its slug locked - a
--    false positive, but a safe direction to be wrong in (a locked slug on
--    a draft is an inconvenience fixable by deleting and recreating the
--    row, or a one-off manual `UPDATE company SET ever_published = false
--    WHERE id = ...` after confirming with the dashboard team that the
--    specific row was genuinely never public; an unlocked slug on a
--    company that really was public is a real, uncontrolled URL-breakage
--    risk with no such easy recovery). Only rows created AFTER this
--    migration runs get accurate, real-time ever_published tracking via
--    DashboardCompanyServiceImpl.create()/update() - this one-time backfill
--    only ever touches rows that already existed at migration-apply time.
--
-- 2) Defensively, idempotently reasserts the NOT NULL + UNIQUE constraints
--    CompanyEntity.slug has always declared via JPA annotations
--    (`nullable = false`, `unique = true`) but which - like the rest of the
--    `company` table itself - have no traceable origin anywhere in this
--    migration history (see GAP-033 in the website repo's backend-gaps.md;
--    this codebase has no ddl-auto validation, so those annotations were
--    never cross-checked against the real schema at startup either).
--    Deliberately NOT a backfill/rewrite: if a real environment already
--    satisfies these constraints (expected - the app has been running
--    against them), these statements are no-ops. If it does not, they fail
--    loudly here rather than silently normalizing or deduplicating an
--    already-published company's public URL out from under it.
--
-- 3) NEW (Phase 7B-GA): explicitly verifies every pre-existing slug value
--    is already canonical lowercase kebab-case
--    (^[a-z0-9]+(-[a-z0-9]+)*$ - the same pattern
--    DashboardCompanyServiceImpl.requireCanonicalSlug() now enforces for
--    every NEW dashboard-supplied custom slug going forward, and the same
--    shape CompanyPublicController's own slug route path already requires
--    for a lookup to match at all) and RAISES AN ACTIONABLE EXCEPTION,
--    naming every offending row, if any existing value does not conform -
--    rather than silently accepting a legacy non-canonical value with no
--    record that it was ever checked. No existing slug value is rewritten
--    or normalized by this migration, in either the passing or failing
--    case. This is not a DB CHECK constraint (a CHECK could not be proven
--    compatible with unknown existing production data without a live
--    read-only preflight - a CHECK failing would abort the ALTER
--    unhelpfully rather than name the offending rows) - it is a one-time
--    verification gate. Going forward, canonicality is enforced
--    continuously by four independent layers, not this migration alone:
--      a. This migration's own one-time precondition check (below).
--      b. DashboardCompanyServiceImpl.requireCanonicalSlug() - every new
--         dashboard-supplied custom slug (create and update).
--      c. DashboardCompanyServiceImpl.slugify() - every auto-generated
--         slug is canonical by construction.
--      d. CompanyPublicController's `/slug/{companySlug:[a-z0-9-]*[a-z][a-z0-9-]*}`
--         path pattern - a non-canonical slug can never even match the
--         public lookup route.
--    Remediation procedure if this migration ever fails on real production
--    data: do NOT edit this file or hand-patch the database to force it
--    through. Query `SELECT id, slug FROM company WHERE slug !~
--    '^[a-z0-9]+(-[a-z0-9]+)*\''` (the same predicate this migration uses)
--    against the real database to get the exact offending rows, decide
--    case-by-case with the dashboard/content team whether each one should
--    be corrected to a new canonical value (a genuine, deliberate slug
--    change - out of this migration's scope entirely) or is a case this
--    migration's canonicality assumption was wrong about, then add a
--    follow-up forward-only migration once that decision is made. This
--    migration must keep failing loudly until that follow-up lands - do
--    not weaken or remove the check below to force it through.

ALTER TABLE company
  ADD COLUMN IF NOT EXISTS ever_published BOOLEAN NOT NULL DEFAULT false;

UPDATE company
SET ever_published = true
WHERE ever_published = false;

DO $$
DECLARE
  null_ids TEXT;
BEGIN
  SELECT string_agg(id::text, ', ' ORDER BY id) INTO null_ids
  FROM company
  WHERE slug IS NULL;

  IF null_ids IS NOT NULL THEN
    RAISE EXCEPTION
      'V137: % existing company row(s) have a NULL slug: ids [%]. '
      'See this migration file''s own header comment for the required remediation procedure - '
      'do not edit this migration to force it through.',
      (SELECT count(*) FROM company WHERE slug IS NULL),
      null_ids;
  END IF;
END $$;

DO $$
DECLARE
  offending_ids TEXT;
BEGIN
  SELECT string_agg(id::text, ', ' ORDER BY id) INTO offending_ids
  FROM company
  WHERE slug !~ '^[a-z0-9]+(-[a-z0-9]+)*$';

  IF offending_ids IS NOT NULL THEN
    RAISE EXCEPTION
      'V137: % existing company row(s) have a non-canonical slug and were NOT modified: ids [%]. '
      'See this migration file''s own header comment for the required remediation procedure - '
      'do not edit this migration to force it through.',
      (SELECT count(*) FROM company WHERE slug !~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
      offending_ids;
  END IF;
END $$;

ALTER TABLE company
  ALTER COLUMN slug SET NOT NULL;

-- Duplicate check ahead of the ADD CONSTRAINT below: Postgres's own unique-
-- violation error on that statement would already fail loudly and not
-- silently deduplicate anything, but it names only the first colliding
-- value it happens to hit, not every duplicate group - this gives the same
-- actionable, name-every-offending-row treatment as the two checks above.
DO $$
DECLARE
  duplicate_report TEXT;
BEGIN
  SELECT string_agg(format('%L used by ids [%s]', slug, id_list), '; ') INTO duplicate_report
  FROM (
    SELECT slug, string_agg(id::text, ', ' ORDER BY id) AS id_list
    FROM company
    GROUP BY slug
    HAVING count(*) > 1
  ) dupes;

  IF duplicate_report IS NOT NULL THEN
    RAISE EXCEPTION
      'V137: pre-existing duplicate company slug value(s) found: %. '
      'See this migration file''s own header comment for the required remediation procedure - '
      'do not edit this migration to force it through.',
      duplicate_report;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'uk_company_slug' AND conrelid = 'company'::regclass
  ) THEN
    ALTER TABLE company
      ADD CONSTRAINT uk_company_slug UNIQUE (slug);
  END IF;
END $$;
