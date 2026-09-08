-- Read-only production audit: publicly flagged projects whose builder cannot be public.
SELECT
    p.id AS project_id,
    p.name AS project_name,
    p.builder_id,
    b.name AS builder_name,
    p.published AS project_published,
    p.active AS project_active,
    p.deleted AS project_deleted,
    p.review_status,
    b.published AS builder_published,
    b.active AS builder_active,
    b.deleted AS builder_deleted
FROM project p
JOIN builder b ON b.id = p.builder_id
WHERE p.published = true
  AND p.active = true
  AND p.deleted = false
  AND p.review_status = 'APPROVED'
  AND (
      b.published IS DISTINCT FROM true
      OR b.active IS DISTINCT FROM true
      OR b.deleted IS DISTINCT FROM false
  )
ORDER BY p.id;

-- Broader audit: any row carrying published=true while any canonical
-- project-publication prerequisite is false.
SELECT
    p.id AS project_id,
    p.name AS project_name,
    p.builder_id,
    b.name AS builder_name,
    p.active AS project_active,
    p.deleted AS project_deleted,
    p.review_status,
    b.published AS builder_published,
    b.active AS builder_active,
    b.deleted AS builder_deleted
FROM project p
LEFT JOIN builder b ON b.id = p.builder_id
WHERE p.published = true
  AND (
      p.active IS DISTINCT FROM true
      OR p.deleted IS DISTINCT FROM false
      OR p.review_status IS DISTINCT FROM 'APPROVED'
      OR b.id IS NULL
      OR b.published IS DISTINCT FROM true
      OR b.active IS DISTINCT FROM true
      OR b.deleted IS DISTINCT FROM false
  )
ORDER BY p.id;

-- Orphan audit. The current schema has project.builder_id NOT NULL with an FK,
-- so this should return zero rows unless constraints were disabled or bypassed.
SELECT p.id AS project_id, p.name AS project_name, p.builder_id
FROM project p
LEFT JOIN builder b ON b.id = p.builder_id
WHERE b.id IS NULL
ORDER BY p.id;
