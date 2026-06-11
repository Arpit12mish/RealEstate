#!/usr/bin/env node
/**
 * SFS Dashboard — REVIEWER Role API Test Runner
 *
 * Sections:
 *   A — Setup (ADMIN creates builder + two projects + submits both for review)
 *   B — REVIEWER allowed reads (list/get endpoints)
 *   C — REVIEWER review actions
 *         Project A: mark field issue → reject → REJECTED → reopen → DRAFT
 *         Project B: no field issues → approve → APPROVED
 *   D — REVIEWER forbidden writes (must all return 403)
 *   Cleanup — ADMIN deletes both projects and builder
 *
 * Two separate projects are necessary because an active field-review issue on
 * a project blocks approval. Project A carries the field-issue/reject/reopen
 * workflow; Project B is kept clean so the approve test has no interference.
 *
 * Prerequisites: Node 18+ (uses built-in fetch)
 *
 * Usage:
 *   BASE_URL=http://localhost:8080 \
 *   REVIEWER_EMAIL=reviewer@squarefootstory.com \
 *   REVIEWER_PASSWORD=Reviewer@12345 \
 *   ADMIN_EMAIL=admin@squarefootstory.com \
 *   ADMIN_PASSWORD=Admin@12345 \
 *   node scripts/dashboard-reviewer-api-test.js
 *
 * Exit code: 0 = all tests passed, 1 = one or more tests failed.
 */

'use strict';

const { is2xx, request: req, assertStatus, assertEqual, login, banner, printSummary } = require('./lib/test-helpers');

// ─── Environment ──────────────────────────────────────────────────────────────

const BASE_URL       = (process.env.BASE_URL || '').replace(/\/$/, '');
const REV_EMAIL      = process.env.REVIEWER_EMAIL;
const REV_PASSWORD   = process.env.REVIEWER_PASSWORD;
const ADMIN_EMAIL    = process.env.ADMIN_EMAIL;
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD;

if (!BASE_URL || !REV_EMAIL || !REV_PASSWORD || !ADMIN_EMAIL || !ADMIN_PASSWORD) {
  console.error(
    '\nERROR: Required environment variables:\n' +
    '  BASE_URL          e.g. http://localhost:8080\n' +
    '  REVIEWER_EMAIL\n' +
    '  REVIEWER_PASSWORD\n' +
    '  ADMIN_EMAIL       (used for test setup and cleanup only)\n' +
    '  ADMIN_PASSWORD\n'
  );
  process.exit(1);
}

const results = [];
const TS = Date.now();

let currentToken = null;
async function r(method, path, body) {
  return req(BASE_URL, currentToken, method, path, body);
}

// ─── Main ─────────────────────────────────────────────────────────────────────

async function main() {
  console.log('\n' + '═'.repeat(64));
  console.log('  SFS Dashboard — REVIEWER API Test Runner');
  console.log(`  Target  : ${BASE_URL}`);
  console.log(`  Run ID  : ${TS}`);
  console.log('═'.repeat(64));

  // ════════════════════════════════════════════════════════════════════════════
  //  SECTION A — SETUP
  //  ADMIN creates one builder + two projects, submits both for review.
  //  Project A — used for field-issue / reject / reopen workflow.
  //  Project B — kept clean (no field issues) for the approve workflow.
  // ════════════════════════════════════════════════════════════════════════════
  banner('SECTION A — Setup (ADMIN creates 2 projects in PENDING_REVIEW)');

  currentToken = await login(BASE_URL, ADMIN_EMAIL, ADMIN_PASSWORD);
  console.log('  ✓  ADMIN login OK');

  const citiesRes = await r('GET', '/api/dashboard/cities');
  const citiesArray = Array.isArray(citiesRes.body) ? citiesRes.body
    : Array.isArray(citiesRes.body?.content) ? citiesRes.body.content : [];
  const cityId = citiesArray[0]?.id ?? null;

  // Shared builder
  const builderRes = await r('POST', '/api/dashboard/builders', {
    name: `Rev Builder ${TS}`, priority: 0, active: true,
    ...(cityId ? { cityId } : {}),
  });
  if (!is2xx(builderRes.status)) { console.error('Cannot create builder. Aborting.'); process.exit(1); }
  const builderId = builderRes.body.id;

  const projectBase = {
    status: 'UNDER_CONSTRUCTION', propertyTypes: ['APARTMENT'],
    priority: 0, active: true,
    ...(cityId ? { cityId } : {}),
  };

  // Project A — for field-issue / reject / reopen
  const projARes = await r('POST', `/api/dashboard/builders/${builderId}/projects`, {
    ...projectBase,
    name: `Rev Project A ${TS}`, slug: `rev-project-a-${TS}`,
  });
  if (!is2xx(projARes.status)) { console.error('Cannot create Project A. Aborting.'); process.exit(1); }
  const projectAId = projARes.body.id;

  // Add media to Project A so workspace reads have content
  const mediaRes = await r('POST', `/api/dashboard/projects/${projectAId}/media`, {
    mediaType: 'IMAGE', url: 'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/test/rev-image.jpg',
    sortOrder: 0, active: true,
  });
  const mediaId = mediaRes.body?.id ?? null;

  const submitARes = await r('POST', `/api/dashboard/projects/${projectAId}/submit-review`, {
    remarks: `Reviewer test A setup ${TS}`,
  });
  if (!is2xx(submitARes.status)) {
    console.error(`  ✗  submit-review for Project A returned ${submitARes.status}. Aborting.`);
    process.exit(1);
  }
  console.log(`  ✓  Project A (id=${projectAId}) is PENDING_REVIEW`);

  // Project B — for approve (no field issues will be created on this project)
  const projBRes = await r('POST', `/api/dashboard/builders/${builderId}/projects`, {
    ...projectBase,
    name: `Rev Project B ${TS}`, slug: `rev-project-b-${TS}`,
  });
  if (!is2xx(projBRes.status)) { console.error('Cannot create Project B. Aborting.'); process.exit(1); }
  const projectBId = projBRes.body.id;

  const submitBRes = await r('POST', `/api/dashboard/projects/${projectBId}/submit-review`, {
    remarks: `Reviewer test B setup ${TS}`,
  });
  if (!is2xx(submitBRes.status)) {
    console.error(`  ✗  submit-review for Project B returned ${submitBRes.status}. Aborting.`);
    process.exit(1);
  }
  console.log(`  ✓  Project B (id=${projectBId}) is PENDING_REVIEW`);

  console.log(`  ✓  builderId=${builderId}  projectAId=${projectAId}  projectBId=${projectBId}`);

  // ════════════════════════════════════════════════════════════════════════════
  //  SECTION B — REVIEWER ALLOWED READS
  // ════════════════════════════════════════════════════════════════════════════
  banner('SECTION B — REVIEWER allowed reads');

  currentToken = await login(BASE_URL, REV_EMAIL, REV_PASSWORD);
  console.log('  ✓  REVIEWER login OK');

  const me = await r('GET', '/api/dashboard/auth/me');
  assertStatus(results, 'GET /api/dashboard/auth/me', 'GET', '/api/dashboard/auth/me', 200, me.status, me.body);
  if (me.body?.role !== 'REVIEWER') {
    console.error(`  ✗  Expected role REVIEWER, got "${me.body?.role}". Aborting.`);
    process.exit(1);
  }
  console.log('  ✓  Confirmed: role = REVIEWER');

  await r('GET', '/api/dashboard/overview').then(res =>
    assertStatus(results, 'GET /api/dashboard/overview', 'GET', '/api/dashboard/overview', 200, res.status, res.body)
  );

  // Builders (read-only for REVIEWER)
  await r('GET', '/api/dashboard/builders?page=0&size=5').then(res =>
    assertStatus(results, 'GET /api/dashboard/builders', 'GET', '/api/dashboard/builders', 200, res.status, res.body)
  );
  await r('GET', `/api/dashboard/builders/${builderId}`).then(res =>
    assertStatus(results, 'GET /api/dashboard/builders/{id}', 'GET', `/api/dashboard/builders/${builderId}`, 200, res.status, res.body)
  );

  // Projects (read-only for REVIEWER)
  await r('GET', '/api/dashboard/projects?page=0&size=5').then(res =>
    assertStatus(results, 'GET /api/dashboard/projects', 'GET', '/api/dashboard/projects', 200, res.status, res.body)
  );
  await r('GET', `/api/dashboard/projects/${projectAId}`).then(res =>
    assertStatus(results, 'GET /api/dashboard/projects/{id} (A)', 'GET', `/api/dashboard/projects/${projectAId}`, 200, res.status, res.body)
  );

  // Workspace
  const workspaceRes = await r('GET', `/api/dashboard/projects/${projectAId}/workspace`);
  assertStatus(results, 'GET /projects/{id}/workspace (A)', 'GET', `/api/dashboard/projects/${projectAId}/workspace`, 200, workspaceRes.status, workspaceRes.body);
  if (workspaceRes.body) {
    assertEqual(results, 'workspace.project exists', 'object', typeof workspaceRes.body.project);
    assertEqual(results, 'workspace.review exists', 'object', typeof workspaceRes.body.review);
    assertEqual(results, 'workspace.media is array', 'true', String(Array.isArray(workspaceRes.body.media)));
  }

  await r('GET', `/api/dashboard/projects/${projectAId}/review-status`).then(res =>
    assertStatus(results, 'GET /review-status (A)', 'GET', `/api/dashboard/projects/${projectAId}/review-status`, 200, res.status, res.body)
  );
  await r('GET', `/api/dashboard/reviews/field-issues?entityType=PROJECT&entityId=${projectAId}&activeOnly=false`).then(res =>
    assertStatus(results, 'GET /reviews/field-issues (A)', 'GET', `/api/dashboard/reviews/field-issues`, 200, res.status, res.body)
  );
  await r('GET', `/api/dashboard/reviews/history?entityType=PROJECT&entityId=${projectAId}`).then(res =>
    assertStatus(results, 'GET /reviews/history (A)', 'GET', `/api/dashboard/reviews/history`, 200, res.status, res.body)
  );

  // Reference data
  await r('GET', '/api/dashboard/categories').then(res =>
    assertStatus(results, 'GET /api/dashboard/categories', 'GET', '/api/dashboard/categories', 200, res.status, res.body)
  );
  await r('GET', '/api/dashboard/cities').then(res =>
    assertStatus(results, 'GET /api/dashboard/cities', 'GET', '/api/dashboard/cities', 200, res.status, res.body)
  );
  await r('GET', '/api/dashboard/field-help?module=PROJECT_BASIC').then(res =>
    assertStatus(results, 'GET /api/dashboard/field-help', 'GET', '/api/dashboard/field-help', 200, res.status, res.body)
  );

  await r('GET', `/api/dashboard/projects/${projectAId}/media`).then(res =>
    assertStatus(results, 'GET /projects/{id}/media (A)', 'GET', `/api/dashboard/projects/${projectAId}/media`, 200, res.status, res.body)
  );
  await r('GET', '/api/dashboard/scraping/candidates?page=0&size=5').then(res =>
    assertStatus(results, 'GET /api/dashboard/scraping/candidates', 'GET', '/api/dashboard/scraping/candidates', 200, res.status, res.body)
  );

  // ════════════════════════════════════════════════════════════════════════════
  //  SECTION C — REVIEWER REVIEW ACTIONS
  //
  //  Project A: mark field issue → reject → verify REJECTED
  //             → reopen → verify DRAFT (reopen only valid from REJECTED)
  //  Project B: no active field issues → approve → verify APPROVED
  // ════════════════════════════════════════════════════════════════════════════
  banner('SECTION C — REVIEWER review actions');

  // ── Project A: mark field issue ──────────────────────────────────────────────
  console.log('\n  [Project A — field-issue / reject / reopen]');

  const fieldIssueRes = await r('POST', '/api/dashboard/reviews/field-issues', {
    entityType: 'PROJECT',
    entityId  : projectAId,
    fieldKey  : 'name',
    status    : 'WRONG',
    note      : 'Project name is not descriptive enough — reviewer test',
  });
  assertStatus(results, 'POST /reviews/field-issues on A (mark WRONG)', 'POST', '/api/dashboard/reviews/field-issues', 200, fieldIssueRes.status, fieldIssueRes.body);
  const fieldIssueId = fieldIssueRes.body?.id ?? null;
  if (is2xx(fieldIssueRes.status) && fieldIssueRes.body) {
    assertEqual(results, 'field-issue.entityType=PROJECT', 'PROJECT', fieldIssueRes.body?.entityType);
    assertEqual(results, 'field-issue.status=WRONG',       'WRONG',   fieldIssueRes.body?.status);
  }

  // ── Project A: reject (PENDING_REVIEW → REJECTED) ───────────────────────────
  const rejectARes = await r('POST', `/api/dashboard/projects/${projectAId}/reject`, {
    remarks: 'Rejected by reviewer test — name not descriptive',
  });
  assertStatus(results, 'POST /reject A → REJECTED', 'POST', `/api/dashboard/projects/${projectAId}/reject`, 200, rejectARes.status, rejectARes.body);
  if (is2xx(rejectARes.status)) {
    assertEqual(results, 'Project A reviewStatus=REJECTED', 'REJECTED', rejectARes.body?.reviewStatus);
  }

  // ── Project A: reopen (REJECTED → DRAFT) ────────────────────────────────────
  // Reopen is valid only from REJECTED — project is now REJECTED, so this must succeed.
  const reopenARes = await r('POST', `/api/dashboard/projects/${projectAId}/reopen`, {
    remarks: 'Reviewer reopening to let DATA_ENTRY fix issues',
  });
  assertStatus(results, 'POST /reopen A (REJECTED → DRAFT)', 'POST', `/api/dashboard/projects/${projectAId}/reopen`, 200, reopenARes.status, reopenARes.body);
  if (is2xx(reopenARes.status)) {
    assertEqual(results, 'Project A reviewStatus=DRAFT after reopen', 'DRAFT', reopenARes.body?.reviewStatus);
  }

  // ── Project A: delete the field issue (REVIEWER can remove their own issues) ──
  if (fieldIssueId) {
    const deleteIssueRes = await r('DELETE', `/api/dashboard/reviews/field-issues/${fieldIssueId}`);
    assertStatus(results, `DELETE /reviews/field-issues/${fieldIssueId}`, 'DELETE', `/api/dashboard/reviews/field-issues/${fieldIssueId}`, [200, 204], deleteIssueRes.status, deleteIssueRes.body);
  }

  // ── Project B: approve (PENDING_REVIEW, no active field issues → APPROVED) ───
  console.log('\n  [Project B — approve]');

  const approveRes = await r('POST', `/api/dashboard/projects/${projectBId}/approve`, {
    remarks: 'Approved by reviewer test — Project B is clean',
  });
  assertStatus(results, 'POST /approve B → APPROVED', 'POST', `/api/dashboard/projects/${projectBId}/approve`, 200, approveRes.status, approveRes.body);
  if (is2xx(approveRes.status)) {
    assertEqual(results, 'Project B reviewStatus=APPROVED', 'APPROVED', approveRes.body?.reviewStatus);
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  SECTION D — REVIEWER FORBIDDEN  (write operations must return 403)
  // ════════════════════════════════════════════════════════════════════════════
  banner('SECTION D — REVIEWER forbidden writes (must all return 403)');

  // Builder writes
  await r('POST', '/api/dashboard/builders', { name: `Forbidden Builder ${TS}`, priority: 0, active: true }).then(res =>
    assertStatus(results, 'POST /builders must be 403', 'POST', '/api/dashboard/builders', 403, res.status, res.body)
  );
  await r('PUT', `/api/dashboard/builders/${builderId}`, { name: 'Forbidden Update', priority: 0, active: true }).then(res =>
    assertStatus(results, 'PUT /builders/{id} must be 403', 'PUT', `/api/dashboard/builders/${builderId}`, 403, res.status, res.body)
  );
  await r('PATCH', `/api/dashboard/builders/${builderId}/logo`, {
    logoUrl: 'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/test/forbidden.webp',
  }).then(res =>
    assertStatus(results, 'PATCH /builders/{id}/logo must be 403', 'PATCH', `/api/dashboard/builders/${builderId}/logo`, 403, res.status, res.body)
  );
  await r('PATCH', `/api/dashboard/builders/${builderId}/published?value=true`).then(res =>
    assertStatus(results, 'PATCH /builders/{id}/published must be 403', 'PATCH', `/api/dashboard/builders/${builderId}/published`, 403, res.status, res.body)
  );
  await r('DELETE', `/api/dashboard/builders/${builderId}`).then(res =>
    assertStatus(results, 'DELETE /builders/{id} must be 403', 'DELETE', `/api/dashboard/builders/${builderId}`, 403, res.status, res.body)
  );

  // Project writes (use Project A — now DRAFT, doesn't matter for 403 tests)
  await r('POST', `/api/dashboard/builders/${builderId}/projects`, {
    name: `Forbidden Project ${TS}`, slug: `forbidden-${TS}`,
    status: 'UNDER_CONSTRUCTION', propertyTypes: ['APARTMENT'], priority: 0, active: true,
  }).then(res =>
    assertStatus(results, 'POST /builders/{id}/projects must be 403', 'POST', `/api/dashboard/builders/${builderId}/projects`, 403, res.status, res.body)
  );
  await r('PUT', `/api/dashboard/projects/${projectAId}`, {
    name: 'Forbidden Update', slug: `rev-project-a-${TS}`,
    status: 'UNDER_CONSTRUCTION', propertyTypes: ['APARTMENT'], priority: 0, active: true,
  }).then(res =>
    assertStatus(results, 'PUT /projects/{id} must be 403', 'PUT', `/api/dashboard/projects/${projectAId}`, 403, res.status, res.body)
  );
  await r('PATCH', `/api/dashboard/projects/${projectAId}/published?value=true`).then(res =>
    assertStatus(results, 'PATCH /projects/{id}/published must be 403', 'PATCH', `/api/dashboard/projects/${projectAId}/published`, 403, res.status, res.body)
  );
  await r('PATCH', `/api/dashboard/projects/${projectAId}/active?value=false`).then(res =>
    assertStatus(results, 'PATCH /projects/{id}/active must be 403', 'PATCH', `/api/dashboard/projects/${projectAId}/active`, 403, res.status, res.body)
  );
  await r('DELETE', `/api/dashboard/projects/${projectAId}`).then(res =>
    assertStatus(results, 'DELETE /projects/{id} must be 403', 'DELETE', `/api/dashboard/projects/${projectAId}`, 403, res.status, res.body)
  );

  // Project data mutations
  await r('POST', `/api/dashboard/projects/${projectAId}/media`, {
    mediaType: 'IMAGE', url: 'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/test/forbidden.jpg', sortOrder: 0, active: true,
  }).then(res =>
    assertStatus(results, 'POST /projects/{id}/media must be 403', 'POST', `/api/dashboard/projects/${projectAId}/media`, 403, res.status, res.body)
  );
  if (mediaId) {
    await r('DELETE', `/api/dashboard/projects/${projectAId}/media/${mediaId}`).then(res =>
      assertStatus(results, 'DELETE /projects/{id}/media/{id} must be 403', 'DELETE', `/api/dashboard/projects/${projectAId}/media/${mediaId}`, 403, res.status, res.body)
    );
  }

  // submit-review is not allowed for REVIEWER
  await r('POST', `/api/dashboard/projects/${projectAId}/submit-review`, { remarks: 'Forbidden' }).then(res =>
    assertStatus(results, 'POST /submit-review must be 403', 'POST', `/api/dashboard/projects/${projectAId}/submit-review`, 403, res.status, res.body)
  );

  // Audit (ADMIN-only)
  await r('GET', '/api/dashboard/audit?page=0&size=5').then(res =>
    assertStatus(results, 'GET /api/dashboard/audit must be 403', 'GET', '/api/dashboard/audit', 403, res.status, res.body)
  );

  // Category / city writes
  await r('POST', '/api/dashboard/categories', { name: `Forbidden Cat ${TS}`, slug: `forbidden-cat-${TS}`, active: true }).then(res =>
    assertStatus(results, 'POST /categories must be 403', 'POST', '/api/dashboard/categories', 403, res.status, res.body)
  );
  await r('POST', '/api/dashboard/cities', { name: `Forbidden City ${TS}`, active: true }).then(res =>
    assertStatus(results, 'POST /cities must be 403', 'POST', '/api/dashboard/cities', 403, res.status, res.body)
  );

  // Field-help write
  await r('PUT', '/api/dashboard/field-help', {
    module: 'PROJECT_BASIC', fieldKey: 'name', fieldLabel: 'Forbidden', shortHelp: 'Forbidden',
  }).then(res =>
    assertStatus(results, 'PUT /field-help must be 403', 'PUT', '/api/dashboard/field-help', 403, res.status, res.body)
  );

  // Scraping write
  await r('POST', '/api/dashboard/scraping/rera/search-by-number', {
    sourceCode: 'HARYANA_RERA', reraNumber: 'FORBIDDEN-REV-123',
  }).then(res =>
    assertStatus(results, 'POST /scraping/rera/search-by-number must be 403', 'POST', '/api/dashboard/scraping/rera/search-by-number', 403, res.status, res.body)
  );

  // ─── Cleanup ─────────────────────────────────────────────────────────────────
  banner('Cleanup');
  currentToken = await login(BASE_URL, ADMIN_EMAIL, ADMIN_PASSWORD);
  await r('DELETE', `/api/dashboard/projects/${projectAId}`);
  await r('DELETE', `/api/dashboard/projects/${projectBId}`);
  await r('DELETE', `/api/dashboard/builders/${builderId}`);
  console.log('  ✓  Test data cleaned up (Project A, Project B, builder)');

  // ─── Summary ─────────────────────────────────────────────────────────────────
  const failed = printSummary(results, 'dashboard-reviewer-api-test.js');
  process.exit(failed > 0 ? 1 : 0);
}

main().catch(err => {
  console.error('\nUnhandled error:', err);
  process.exit(1);
});
