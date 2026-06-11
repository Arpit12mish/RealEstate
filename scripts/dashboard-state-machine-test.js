#!/usr/bin/env node
/**
 * SFS Dashboard — Review State Machine Test
 *
 * Walks the full project lifecycle using real HTTP calls:
 *   DRAFT → submit → PENDING_REVIEW
 *   PENDING_REVIEW → reject  → REJECTED
 *   REJECTED → submit → PENDING_REVIEW
 *   PENDING_REVIEW → approve → APPROVED
 *   APPROVED → direct publish remains reviewer-forbidden
 *   APPROVED → reopen is rejected by current workflow
 *
 * In addition, verifies that DATA_ENTRY is blocked (403) when they try to
 * edit a PENDING_REVIEW or APPROVED project.
 *
 * Prerequisites: Node 18+ (uses built-in fetch)
 *
 * Usage:
 *   BASE_URL=http://localhost:8080 \
 *   ADMIN_TOKEN=... \
 *   DATA_ENTRY_TOKEN=... \
 *   REVIEWER_TOKEN=... \
 *   node scripts/dashboard-state-machine-test.js
 *
 * Or let the script log in:
 *   BASE_URL=http://localhost:8080 \
 *   ADMIN_EMAIL=admin@squarefootstory.com \
 *   ADMIN_PASSWORD=Admin@12345 \
 *   DATA_ENTRY_EMAIL=data@squarefootstory.com \
 *   DATA_ENTRY_PASSWORD=DataEntry@12345 \
 *   REVIEWER_EMAIL=reviewer@squarefootstory.com \
 *   REVIEWER_PASSWORD=Reviewer@12345 \
 *   node scripts/dashboard-state-machine-test.js
 *
 * Exit code: 0 = all tests passed, 1 = one or more tests failed.
 */

'use strict';

const { is2xx, request: req, assertStatus, assertEqual, login, banner, printSummary } = require('./lib/test-helpers');

// ─── Environment ──────────────────────────────────────────────────────────────

const BASE_URL         = (process.env.BASE_URL || '').replace(/\/$/, '');
const ADMIN_TOKEN      = process.env.ADMIN_TOKEN;
const DE_TOKEN         = process.env.DATA_ENTRY_TOKEN;
const REV_TOKEN        = process.env.REVIEWER_TOKEN;
const ADMIN_EMAIL      = process.env.ADMIN_EMAIL;
const ADMIN_PASSWORD   = process.env.ADMIN_PASSWORD;
const DE_EMAIL         = process.env.DATA_ENTRY_EMAIL;
const DE_PASSWORD      = process.env.DATA_ENTRY_PASSWORD;
const REV_EMAIL        = process.env.REVIEWER_EMAIL;
const REV_PASSWORD     = process.env.REVIEWER_PASSWORD;

const HAS_ADMIN_AUTH = Boolean(ADMIN_TOKEN || (ADMIN_EMAIL && ADMIN_PASSWORD));
const HAS_DE_AUTH = Boolean(DE_TOKEN || (DE_EMAIL && DE_PASSWORD));
const HAS_REV_AUTH = Boolean(REV_TOKEN || (REV_EMAIL && REV_PASSWORD));

if (!BASE_URL || !HAS_ADMIN_AUTH || !HAS_DE_AUTH || !HAS_REV_AUTH) {
  console.error(
    '\nERROR: Required environment variables:\n' +
    '  BASE_URL           e.g. http://localhost:8080\n' +
    '  ADMIN_TOKEN or ADMIN_EMAIL + ADMIN_PASSWORD\n' +
    '  DATA_ENTRY_TOKEN or DATA_ENTRY_EMAIL + DATA_ENTRY_PASSWORD\n' +
    '  REVIEWER_TOKEN or REVIEWER_EMAIL + REVIEWER_PASSWORD\n'
  );
  process.exit(1);
}

const results = [];
const TS = Date.now();

// Thin wrappers that thread the current token
let currentToken = null;
async function r(method, path, body) {
  return req(BASE_URL, currentToken, method, path, body);
}

async function tokenFor(label, token, email, password) {
  if (token) {
    console.log(`  ✓  ${label} token loaded from environment`);
    return token;
  }
  const accessToken = await login(BASE_URL, email, password);
  console.log(`  ✓  ${label} login OK`);
  return accessToken;
}

// ─── Main ─────────────────────────────────────────────────────────────────────

async function main() {
  console.log('\n' + '═'.repeat(64));
  console.log('  SFS Dashboard — State Machine Test');
  console.log(`  Target  : ${BASE_URL}`);
  console.log(`  Run ID  : ${TS}`);
  console.log('═'.repeat(64));

  // ── Step 1: Login as DATA_ENTRY ──────────────────────────────────────────────
  banner('Step 1 — Login as DATA_ENTRY');
  currentToken = await tokenFor('DATA_ENTRY', DE_TOKEN, DE_EMAIL, DE_PASSWORD);

  // ── Step 2: Bootstrap — create builder + project ─────────────────────────────
  banner('Step 2 — Bootstrap: create builder + project');

  const builderRes = await r('POST', '/api/dashboard/builders', {
    name: `SM Builder ${TS}`, priority: 0, active: true,
  });
  assertStatus(results, 'POST /builders (setup)', 'POST', '/api/dashboard/builders', 200, builderRes.status, builderRes.body);
  if (!is2xx(builderRes.status)) { console.error('Cannot create builder. Aborting.'); process.exit(1); }
  const builderId = builderRes.body.id;

  const projectRes = await r('POST', `/api/dashboard/builders/${builderId}/projects`, {
    name: `SM Project ${TS}`, slug: `sm-project-${TS}`,
    status: 'UNDER_CONSTRUCTION', propertyTypes: ['APARTMENT'],
    priority: 0, active: true,
  });
  assertStatus(results, 'POST /projects (setup)', 'POST', `/api/dashboard/builders/${builderId}/projects`, 200, projectRes.status, projectRes.body);
  if (!is2xx(projectRes.status)) { console.error('Cannot create project. Aborting.'); process.exit(1); }
  const projectId = projectRes.body.id;
  console.log(`  ✓  builderId=${builderId}  projectId=${projectId}`);

  // ── Step 3: Verify initial DRAFT state ──────────────────────────────────────
  banner('Step 3 — Initial state must be DRAFT');

  const draftStatusRes = await r('GET', `/api/dashboard/projects/${projectId}/review-status`);
  assertStatus(results, 'GET /review-status initial', 'GET', `/api/dashboard/projects/${projectId}/review-status`, 200, draftStatusRes.status, draftStatusRes.body);
  assertEqual(results, 'initial reviewStatus=DRAFT', 'DRAFT', draftStatusRes.body?.reviewStatus);

  // ── Step 4: ADMIN cannot publish a DRAFT project ────────────────────────────
  banner('Step 4 — ADMIN cannot publish a DRAFT project');

  currentToken = await tokenFor('ADMIN', ADMIN_TOKEN, ADMIN_EMAIL, ADMIN_PASSWORD);

  const publishDraftRes = await r('PATCH', `/api/dashboard/projects/${projectId}/published?value=true`);
  assertStatus(results, 'PATCH /published=true (DRAFT — must be 400)', 'PATCH', `/api/dashboard/projects/${projectId}/published?value=true`, 400, publishDraftRes.status, publishDraftRes.body);

  currentToken = await tokenFor('DATA_ENTRY', DE_TOKEN, DE_EMAIL, DE_PASSWORD);

  // ── Step 5: DATA_ENTRY edits in DRAFT (allowed) ──────────────────────────────
  banner('Step 5 — DATA_ENTRY can edit in DRAFT');

  const editDraftRes = await r('PUT', `/api/dashboard/projects/${projectId}`, {
    name: `SM Project ${TS} (draft edit)`, slug: `sm-project-${TS}`,
    status: 'UNDER_CONSTRUCTION', propertyTypes: ['APARTMENT'],
    priority: 0, active: true,
  });
  assertStatus(results, 'PUT /projects (DRAFT edit — allowed)', 'PUT', `/api/dashboard/projects/${projectId}`, 200, editDraftRes.status, editDraftRes.body);

  // ── Step 6: Submit for review → PENDING_REVIEW ───────────────────────────────
  banner('Step 6 — Submit for review → PENDING_REVIEW');

  const submitRes = await r('POST', `/api/dashboard/projects/${projectId}/submit-review`, {
    remarks: `State machine test run ${TS}`,
  });
  assertStatus(results, 'POST /submit-review → PENDING_REVIEW', 'POST', `/api/dashboard/projects/${projectId}/submit-review`, 200, submitRes.status, submitRes.body);
  if (is2xx(submitRes.status)) {
    assertEqual(results, 'reviewStatus after submit=PENDING_REVIEW', 'PENDING_REVIEW', submitRes.body?.reviewStatus);
  }

  // ── Step 7: DATA_ENTRY blocked on PENDING_REVIEW project (edit + submit) ─────
  banner('Step 7 — DATA_ENTRY blocked in PENDING_REVIEW state');

  const editPendingRes = await r('PUT', `/api/dashboard/projects/${projectId}`, {
    name: `SM Project ${TS} (should fail)`, slug: `sm-project-${TS}`,
    status: 'UNDER_CONSTRUCTION', propertyTypes: ['APARTMENT'],
    priority: 0, active: true,
  });
  assertStatus(results, 'PUT /projects (PENDING_REVIEW — must be 403)', 'PUT', `/api/dashboard/projects/${projectId}`, 403, editPendingRes.status, editPendingRes.body);

  const submitAgainRes = await r('POST', `/api/dashboard/projects/${projectId}/submit-review`, {
    remarks: 'Should be blocked',
  });
  assertStatus(results, 'POST /submit-review (PENDING_REVIEW — must be 403)', 'POST', `/api/dashboard/projects/${projectId}/submit-review`, 403, submitAgainRes.status, submitAgainRes.body);

  // ── Step 8: REVIEWER can see project in review queue ────────────────────────
  banner('Step 8 — REVIEWER sees project in Review Queue');

  currentToken = await tokenFor('REVIEWER', REV_TOKEN, REV_EMAIL, REV_PASSWORD);

  const queueRes = await r('GET', '/api/dashboard/projects?reviewStatus=PENDING_REVIEW&page=0&size=50');
  assertStatus(results, 'GET /projects?reviewStatus=PENDING_REVIEW', 'GET', '/api/dashboard/projects?reviewStatus=PENDING_REVIEW&page=0&size=50', 200, queueRes.status, queueRes.body);
  const queueItems = Array.isArray(queueRes.body?.content) ? queueRes.body.content : [];
  assertEqual(results, 'review queue contains submitted project', 'true', String(queueItems.some(p => String(p.id) === String(projectId))));

  // ── Step 9: REVIEWER rejects → REJECTED ─────────────────────────────────────
  banner('Step 9 — REVIEWER rejects → REJECTED');

  const rejectRes = await r('POST', `/api/dashboard/projects/${projectId}/reject`, {
    remarks: 'Missing project details — rejected by state machine test',
  });
  assertStatus(results, 'POST /reject → REJECTED', 'POST', `/api/dashboard/projects/${projectId}/reject`, 200, rejectRes.status, rejectRes.body);
  if (is2xx(rejectRes.status)) {
    assertEqual(results, 'reviewStatus after reject=REJECTED', 'REJECTED', rejectRes.body?.reviewStatus);
  }

  // ── Step 10: DATA_ENTRY edits in REJECTED (allowed) ─────────────────────────
  banner('Step 10 — DATA_ENTRY can edit in REJECTED state');

  currentToken = await tokenFor('DATA_ENTRY', DE_TOKEN, DE_EMAIL, DE_PASSWORD);

  const editRejectedRes = await r('PUT', `/api/dashboard/projects/${projectId}`, {
    name: `SM Project ${TS} (fixed)`, slug: `sm-project-${TS}`,
    status: 'UNDER_CONSTRUCTION', propertyTypes: ['APARTMENT'],
    priority: 0, active: true, description: 'Fixed based on reviewer feedback',
  });
  assertStatus(results, 'PUT /projects (REJECTED edit — allowed)', 'PUT', `/api/dashboard/projects/${projectId}`, 200, editRejectedRes.status, editRejectedRes.body);

  // ── Step 11: Re-submit → PENDING_REVIEW ─────────────────────────────────────
  banner('Step 11 — Re-submit → PENDING_REVIEW');

  const resubmitRes = await r('POST', `/api/dashboard/projects/${projectId}/submit-review`, {
    remarks: `Re-submission after fix — run ${TS}`,
  });
  assertStatus(results, 'POST /submit-review (re-submit → PENDING_REVIEW)', 'POST', `/api/dashboard/projects/${projectId}/submit-review`, 200, resubmitRes.status, resubmitRes.body);
  if (is2xx(resubmitRes.status)) {
    assertEqual(results, 'reviewStatus after re-submit=PENDING_REVIEW', 'PENDING_REVIEW', resubmitRes.body?.reviewStatus);
  }

  // ── Step 12: REVIEWER approves → APPROVED ───────────────────────────────────
  banner('Step 12 — REVIEWER approves → APPROVED');

  currentToken = await tokenFor('REVIEWER', REV_TOKEN, REV_EMAIL, REV_PASSWORD);

  const approveRes = await r('POST', `/api/dashboard/projects/${projectId}/approve`, {
    remarks: 'All good — approved by state machine test',
  });
  assertStatus(results, 'POST /approve → APPROVED', 'POST', `/api/dashboard/projects/${projectId}/approve`, 200, approveRes.status, approveRes.body);
  if (is2xx(approveRes.status)) {
    assertEqual(results, 'reviewStatus after approve=APPROVED', 'APPROVED', approveRes.body?.reviewStatus);
    assertEqual(results, 'approved project is auto-published', 'true', String(approveRes.body?.published));
  }

  // ── Step 13: Public project detail is visible only after approval ───────────
  banner('Step 13 — Public detail visible after active + published + approved');

  currentToken = null;
  const publicGetRes = await r('GET', `/api/projects/${projectId}`);
  assertStatus(results, 'GET /api/projects/{id} after approval', 'GET', `/api/projects/${projectId}`, 200, publicGetRes.status, publicGetRes.body);

  // ── Step 14: DATA_ENTRY blocked on APPROVED project ─────────────────────────
  banner('Step 14 — DATA_ENTRY blocked in APPROVED state');

  currentToken = await tokenFor('DATA_ENTRY', DE_TOKEN, DE_EMAIL, DE_PASSWORD);

  const editApprovedRes = await r('PUT', `/api/dashboard/projects/${projectId}`, {
    name: `SM Project ${TS} (should fail again)`, slug: `sm-project-${TS}`,
    status: 'UNDER_CONSTRUCTION', propertyTypes: ['APARTMENT'],
    priority: 0, active: true,
  });
  assertStatus(results, 'PUT /projects (APPROVED — must be 403)', 'PUT', `/api/dashboard/projects/${projectId}`, 403, editApprovedRes.status, editApprovedRes.body);

  // ── Step 15: REVIEWER cannot directly publish/unpublish ─────────────────────
  banner('Step 15 — REVIEWER cannot directly publish/unpublish');

  currentToken = await tokenFor('REVIEWER', REV_TOKEN, REV_EMAIL, REV_PASSWORD);

  const reviewerPublishRes = await r('PATCH', `/api/dashboard/projects/${projectId}/published?value=true`);
  assertStatus(results, 'PATCH /published as REVIEWER — must be 403', 'PATCH', `/api/dashboard/projects/${projectId}/published?value=true`, 403, reviewerPublishRes.status, reviewerPublishRes.body);

  // ── Step 16: Approved projects cannot be reopened by current workflow ───────
  // Reopen is only valid from REJECTED status. From APPROVED it must return 400
  // with DASHBOARD_INVALID_WORKFLOW — DATA_ENTRY stays blocked.
  banner('Step 16 — Approved projects cannot be reopened (must return 400)');

  currentToken = await tokenFor('REVIEWER', REV_TOKEN, REV_EMAIL, REV_PASSWORD);

  const reopenApprovedRes = await r('POST', `/api/dashboard/projects/${projectId}/reopen`, {
    remarks: 'Attempting reopen from APPROVED — should be rejected by workflow',
  });
  assertStatus(results, 'POST /reopen from APPROVED — must be 400', 'POST', `/api/dashboard/projects/${projectId}/reopen`, 400, reopenApprovedRes.status, reopenApprovedRes.body);

  // ── Final state verification ─────────────────────────────────────────────────
  banner('Step 17 — Verify final state remains APPROVED');

  const finalStatusRes = await r('GET', `/api/dashboard/projects/${projectId}/review-status`);
  assertStatus(results, 'GET /review-status (final)', 'GET', `/api/dashboard/projects/${projectId}/review-status`, 200, finalStatusRes.status, finalStatusRes.body);
  assertEqual(results, 'final reviewStatus=APPROVED', 'APPROVED', finalStatusRes.body?.reviewStatus);

  // ── Cleanup ─────────────────────────────────────────────────────────────────
  banner('Step 18 — Cleanup test project and builder');

  currentToken = await tokenFor('ADMIN', ADMIN_TOKEN, ADMIN_EMAIL, ADMIN_PASSWORD);

  const deleteProjectRes = await r('DELETE', `/api/dashboard/projects/${projectId}`);
  assertStatus(results, 'DELETE /projects/{id} cleanup', 'DELETE', `/api/dashboard/projects/${projectId}`, [200, 204], deleteProjectRes.status, deleteProjectRes.body);

  const deleteBuilderRes = await r('DELETE', `/api/dashboard/builders/${builderId}`);
  assertStatus(results, 'DELETE /builders/{id} cleanup', 'DELETE', `/api/dashboard/builders/${builderId}`, [200, 204], deleteBuilderRes.status, deleteBuilderRes.body);

  // ─── Summary ─────────────────────────────────────────────────────────────────
  const failed = printSummary(results, 'dashboard-state-machine-test.js');
  process.exit(failed > 0 ? 1 : 0);
}

main().catch(err => {
  console.error('\nUnhandled error:', err);
  process.exit(1);
});
