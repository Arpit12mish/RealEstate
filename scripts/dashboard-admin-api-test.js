#!/usr/bin/env node
/**
 * SFS Dashboard — ADMIN Role API Test Runner
 *
 * Verifies all ADMIN-accessible endpoints return 2xx, including
 * endpoints shared with other roles and ADMIN-exclusive ones.
 * Also walks the full project review workflow from the ADMIN side.
 *
 * Prerequisites: Node 18+ (uses built-in fetch)
 *
 * Usage:
 *   BASE_URL=http://localhost:8080 \
 *   ADMIN_EMAIL=admin@squarefootstory.com \
 *   ADMIN_PASSWORD=Admin@12345 \
 *   node scripts/dashboard-admin-api-test.js
 *
 * Exit code: 0 = all tests passed, 1 = one or more tests failed.
 */

'use strict';

const { is2xx, request: req, assertStatus, assertEqual, login, banner, printSummary } = require('./lib/test-helpers');

// ─── Environment ──────────────────────────────────────────────────────────────

const BASE_URL = (process.env.BASE_URL || '').replace(/\/$/, '');
const EMAIL    = process.env.ADMIN_EMAIL;
const PASSWORD = process.env.ADMIN_PASSWORD;

if (!BASE_URL || !EMAIL || !PASSWORD) {
  console.error(
    '\nERROR: Required environment variables:\n' +
    '  BASE_URL      e.g. http://localhost:8080\n' +
    '  ADMIN_EMAIL\n' +
    '  ADMIN_PASSWORD\n'
  );
  process.exit(1);
}

const results = [];
const TS = Date.now();

let token = null;
async function r(method, path, body) {
  return req(BASE_URL, token, method, path, body);
}

// ─── Main ─────────────────────────────────────────────────────────────────────

async function main() {
  console.log('\n' + '═'.repeat(64));
  console.log('  SFS Dashboard — ADMIN API Test Runner');
  console.log(`  Target  : ${BASE_URL}`);
  console.log(`  Run ID  : ${TS}`);
  console.log('═'.repeat(64));

  // ── Step 1: Login ────────────────────────────────────────────────────────────
  banner('Step 1 — Login as ADMIN');
  token = await login(BASE_URL, EMAIL, PASSWORD);
  console.log('  ✓  ADMIN login OK');

  // ── Step 2: Verify identity ──────────────────────────────────────────────────
  banner('Step 2 — Verify identity (/me)');
  const me = await r('GET', '/api/dashboard/auth/me');
  assertStatus(results, 'GET /api/dashboard/auth/me', 'GET', '/api/dashboard/auth/me', 200, me.status, me.body);
  if (me.body?.role !== 'ADMIN') {
    console.error(`  ✗  Expected role ADMIN, got "${me.body?.role}". Aborting.`);
    process.exit(1);
  }
  console.log('  ✓  Confirmed: role = ADMIN');

  // ── Step 3: Reference data ───────────────────────────────────────────────────
  banner('Step 3 — Reference data (cities)');
  const citiesRes = await r('GET', '/api/dashboard/cities');
  const citiesArray = Array.isArray(citiesRes.body) ? citiesRes.body
    : Array.isArray(citiesRes.body?.content) ? citiesRes.body.content : [];
  let cityId = citiesArray[0]?.id ?? null;
  console.log(cityId ? `  ✓  cityId = ${cityId}` : '  !  No cities found — will create without cityId');

  // ── Step 4: City CRUD (ADMIN-only) ───────────────────────────────────────────
  banner('Step 4 — City CRUD (ADMIN-only)');

  const cityCreateRes = await r('POST', '/api/dashboard/cities', { name: `Test City ${TS}`, active: true });
  assertStatus(results, 'POST /api/dashboard/cities', 'POST', '/api/dashboard/cities', [200, 201], cityCreateRes.status, cityCreateRes.body);
  const testCityId = cityCreateRes.body?.id ?? null;
  if (testCityId) cityId = cityId ?? testCityId;

  if (testCityId) {
    const cityUpdateRes = await r('PUT', `/api/dashboard/cities/${testCityId}`, { name: `Test City ${TS} (updated)`, active: true });
    assertStatus(results, `PUT /api/dashboard/cities/${testCityId}`, 'PUT', `/api/dashboard/cities/${testCityId}`, 200, cityUpdateRes.status, cityUpdateRes.body);
    assertEqual(results, 'city name updated', `Test City ${TS} (updated)`, cityUpdateRes.body?.name);

    await r('GET', `/api/dashboard/cities/${testCityId}`).then(res =>
      assertStatus(results, `GET /api/dashboard/cities/${testCityId}`, 'GET', `/api/dashboard/cities/${testCityId}`, 200, res.status, res.body)
    );
  }

  await r('GET', '/api/dashboard/cities').then(res =>
    assertStatus(results, 'GET /api/dashboard/cities', 'GET', '/api/dashboard/cities', 200, res.status, res.body)
  );

  // ── Step 5: Category CRUD (ADMIN-only) ───────────────────────────────────────
  banner('Step 5 — Category CRUD (ADMIN-only)');

  const catCreateRes = await r('POST', '/api/dashboard/categories', {
    name: `Test Category ${TS}`, slug: `test-category-${TS}`, active: true,
  });
  assertStatus(results, 'POST /api/dashboard/categories', 'POST', '/api/dashboard/categories', [200, 201], catCreateRes.status, catCreateRes.body);
  const catId = catCreateRes.body?.id ?? null;

  if (catId) {
    const catUpdateRes = await r('PUT', `/api/dashboard/categories/${catId}`, {
      name: `Test Category ${TS} (updated)`, slug: `test-category-${TS}`, active: true,
    });
    assertStatus(results, `PUT /api/dashboard/categories/${catId}`, 'PUT', `/api/dashboard/categories/${catId}`, 200, catUpdateRes.status, catUpdateRes.body);

    await r('GET', `/api/dashboard/categories/${catId}`).then(res =>
      assertStatus(results, `GET /api/dashboard/categories/${catId}`, 'GET', `/api/dashboard/categories/${catId}`, 200, res.status, res.body)
    );
  }

  await r('GET', '/api/dashboard/categories').then(res =>
    assertStatus(results, 'GET /api/dashboard/categories', 'GET', '/api/dashboard/categories', 200, res.status, res.body)
  );

  // ── Step 6: Builder CRUD ─────────────────────────────────────────────────────
  banner('Step 6 — Builder CRUD');

  const builderPayload = {
    name    : `Admin Builder ${TS}`,
    phone   : '9111111110',
    priority: 0,
    active  : true,
    ...(cityId ? { cityId } : {}),
  };
  const builderRes = await r('POST', '/api/dashboard/builders', builderPayload);
  assertStatus(results, 'POST /api/dashboard/builders', 'POST', '/api/dashboard/builders', 200, builderRes.status, builderRes.body);
  if (!is2xx(builderRes.status)) { console.error('Cannot create builder. Aborting.'); process.exit(1); }
  const builderId = builderRes.body.id;
  assertEqual(results, 'builder.name matches payload', builderPayload.name, builderRes.body?.name);

  // ADMIN can do full PUT update (DATA_ENTRY cannot)
  const builderUpdateRes = await r('PUT', `/api/dashboard/builders/${builderId}`, {
    ...builderPayload, name: `Admin Builder ${TS} (updated)`,
  });
  assertStatus(results, `PUT /api/dashboard/builders/${builderId}`, 'PUT', `/api/dashboard/builders/${builderId}`, 200, builderUpdateRes.status, builderUpdateRes.body);

  await r('GET', `/api/dashboard/builders/${builderId}`).then(res =>
    assertStatus(results, `GET /api/dashboard/builders/${builderId}`, 'GET', `/api/dashboard/builders/${builderId}`, 200, res.status, res.body)
  );

  await r('GET', '/api/dashboard/builders?page=0&size=5').then(res =>
    assertStatus(results, 'GET /api/dashboard/builders', 'GET', '/api/dashboard/builders', 200, res.status, res.body)
  );

  // ADMIN can update logo
  const logoRes = await r('PATCH', `/api/dashboard/builders/${builderId}/logo`, {
    logoUrl: 'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/dashboard/builders/admin-test/logo.webp',
  });
  assertStatus(results, `PATCH /api/dashboard/builders/${builderId}/logo`, 'PATCH', `/api/dashboard/builders/${builderId}/logo`, 200, logoRes.status, logoRes.body);

  // ADMIN can set published
  const builderPublishRes = await r('PATCH', `/api/dashboard/builders/${builderId}/published?value=true`);
  assertStatus(results, `PATCH /builders/${builderId}/published?value=true`, 'PATCH', `/api/dashboard/builders/${builderId}/published`, 200, builderPublishRes.status, builderPublishRes.body);

  // ── Step 7: Project CRUD ─────────────────────────────────────────────────────
  banner('Step 7 — Project CRUD');

  const projectPayload = {
    name         : `Admin Project ${TS}`,
    slug         : `admin-project-${TS}`,
    status       : 'UNDER_CONSTRUCTION',
    propertyTypes: ['APARTMENT'],
    priority     : 0,
    active       : true,
    ...(cityId ? { cityId } : {}),
  };
  const projectRes = await r('POST', `/api/dashboard/builders/${builderId}/projects`, projectPayload);
  assertStatus(results, 'POST /builders/{id}/projects', 'POST', `/api/dashboard/builders/${builderId}/projects`, 200, projectRes.status, projectRes.body);
  if (!is2xx(projectRes.status)) { console.error('Cannot create project. Aborting.'); process.exit(1); }
  const projectId = projectRes.body.id;
  assertEqual(results, 'project.published is false on creation', 'false', String(projectRes.body?.published));

  const projectUpdateRes = await r('PUT', `/api/dashboard/projects/${projectId}`, {
    ...projectPayload, description: `Updated by admin test run ${TS}`,
  });
  assertStatus(results, `PUT /api/dashboard/projects/${projectId}`, 'PUT', `/api/dashboard/projects/${projectId}`, 200, projectUpdateRes.status, projectUpdateRes.body);

  await r('GET', `/api/dashboard/projects/${projectId}`).then(res =>
    assertStatus(results, `GET /api/dashboard/projects/${projectId}`, 'GET', `/api/dashboard/projects/${projectId}`, 200, res.status, res.body)
  );

  await r('GET', '/api/dashboard/projects?page=0&size=5').then(res =>
    assertStatus(results, 'GET /api/dashboard/projects', 'GET', '/api/dashboard/projects', 200, res.status, res.body)
  );

  const workspaceRes = await r('GET', `/api/dashboard/projects/${projectId}/workspace`);
  assertStatus(results, 'GET /projects/{id}/workspace', 'GET', `/api/dashboard/projects/${projectId}/workspace`, 200, workspaceRes.status, workspaceRes.body);
  if (workspaceRes.body) {
    assertEqual(results, 'workspace.project exists', 'object', typeof workspaceRes.body.project);
    assertEqual(results, 'workspace.review exists', 'object', typeof workspaceRes.body.review);
    assertEqual(results, 'workspace.media is array', 'true', String(Array.isArray(workspaceRes.body.media)));
  }

  // ADMIN-only: set active
  const setActiveRes = await r('PATCH', `/api/dashboard/projects/${projectId}/active?value=true`);
  assertStatus(results, `PATCH /projects/${projectId}/active?value=true`, 'PATCH', `/api/dashboard/projects/${projectId}/active`, 200, setActiveRes.status, setActiveRes.body);

  // ── Step 8: Review workflow (submit → approve) ───────────────────────────────
  banner('Step 8 — Review workflow (ADMIN submit → approve)');

  const submitRes = await r('POST', `/api/dashboard/projects/${projectId}/submit-review`, {
    remarks: `Admin test submission ${TS}`,
  });
  assertStatus(results, 'POST /submit-review', 'POST', `/api/dashboard/projects/${projectId}/submit-review`, 200, submitRes.status, submitRes.body);
  if (is2xx(submitRes.status)) {
    assertEqual(results, 'reviewStatus after submit=PENDING_REVIEW', 'PENDING_REVIEW', submitRes.body?.reviewStatus);
  }

  // ADMIN can approve
  const approveRes = await r('POST', `/api/dashboard/projects/${projectId}/approve`, {
    remarks: 'ADMIN self-approve for testing',
  });
  assertStatus(results, 'POST /approve', 'POST', `/api/dashboard/projects/${projectId}/approve`, 200, approveRes.status, approveRes.body);
  if (is2xx(approveRes.status)) {
    assertEqual(results, 'reviewStatus after approve=APPROVED', 'APPROVED', approveRes.body?.reviewStatus);
  }

  // ADMIN can publish after approval
  const publishRes = await r('PATCH', `/api/dashboard/projects/${projectId}/published?value=true`);
  assertStatus(results, `PATCH /projects/${projectId}/published?value=true`, 'PATCH', `/api/dashboard/projects/${projectId}/published`, 200, publishRes.status, publishRes.body);

  // Negative: reopen and reject are not valid transitions from APPROVED
  const reopenApprovedRes = await r('POST', `/api/dashboard/projects/${projectId}/reopen`, {
    remarks: 'reopen from APPROVED — must fail',
  });
  assertStatus(results, 'POST /reopen from APPROVED — must be 400', 'POST', `/api/dashboard/projects/${projectId}/reopen`, 400, reopenApprovedRes.status, reopenApprovedRes.body);

  const rejectApprovedRes = await r('POST', `/api/dashboard/projects/${projectId}/reject`, {
    remarks: 'reject from APPROVED — must fail',
  });
  assertStatus(results, 'POST /reject from APPROVED — must be 400', 'POST', `/api/dashboard/projects/${projectId}/reject`, 400, rejectApprovedRes.status, rejectApprovedRes.body);

  // ── Step 9: Field Help CRUD (ADMIN-only) ─────────────────────────────────────
  banner('Step 9 — Field Help CRUD (ADMIN-only)');

  const helpUpsertRes = await r('PUT', '/api/dashboard/field-help', {
    module    : 'PROJECT_BASIC',
    fieldKey  : `admin_test_field_${TS}`,
    fieldLabel: 'Admin Test Field',
    shortHelp : 'Created by admin API test runner',
    active    : true,
  });
  assertStatus(results, 'PUT /api/dashboard/field-help', 'PUT', '/api/dashboard/field-help', 200, helpUpsertRes.status, helpUpsertRes.body);

  await r('GET', '/api/dashboard/field-help?module=PROJECT_BASIC').then(res =>
    assertStatus(results, 'GET /api/dashboard/field-help?module=PROJECT_BASIC', 'GET', '/api/dashboard/field-help', 200, res.status, res.body)
  );

  if (helpUpsertRes.body?.id) {
    const helpActiveRes = await r('PATCH', `/api/dashboard/field-help/${helpUpsertRes.body.id}/active?value=false`);
    assertStatus(results, `PATCH /field-help/${helpUpsertRes.body.id}/active`, 'PATCH', `/api/dashboard/field-help/${helpUpsertRes.body.id}/active`, [200, 204], helpActiveRes.status, helpActiveRes.body);
  }

  // ── Step 10: Audit log (ADMIN-only) ─────────────────────────────────────────
  banner('Step 10 — Audit log (ADMIN-only)');

  const auditAllRes = await r('GET', '/api/dashboard/audit?page=0&size=5');
  assertStatus(results, 'GET /api/dashboard/audit', 'GET', '/api/dashboard/audit', 200, auditAllRes.status, auditAllRes.body);
  if (!is2xx(auditAllRes.status)) {
    console.log('  ╔══ AUDIT DEBUG ──────────────────────────────────────────');
    console.log(`  ║  status : ${auditAllRes.status}`);
    console.log(`  ║  body   : ${JSON.stringify(auditAllRes.body, null, 2).replace(/\n/g, '\n  ║           ')}`);
    console.log('  ╚────────────────────────────────────────────────────────');
  }

  await r('GET', `/api/dashboard/audit/projects/${projectId}?page=0&size=5`).then(res =>
    assertStatus(results, 'GET /api/dashboard/audit/projects/{id}', 'GET', `/api/dashboard/audit/projects/${projectId}`, 200, res.status, res.body)
  );

  // ── Step 11: Overview ────────────────────────────────────────────────────────
  banner('Step 11 — Overview');

  await r('GET', '/api/dashboard/overview').then(res =>
    assertStatus(results, 'GET /api/dashboard/overview', 'GET', '/api/dashboard/overview', 200, res.status, res.body)
  );

  // ── Step 12: Project B — reject / reopen lifecycle ───────────────────────────
  // reject is valid only from PENDING_REVIEW; reopen is valid only from REJECTED.
  // Project A is APPROVED, so we create a separate project B for this path.
  banner('Step 12 — Project B: submit → reject → REJECTED → reopen → DRAFT');

  const projBPayload = { ...projectPayload, name: `Admin Project B ${TS}`, slug: `admin-project-b-${TS}` };
  const projBRes = await r('POST', `/api/dashboard/builders/${builderId}/projects`, projBPayload);
  assertStatus(results, 'POST /builders/{id}/projects (project B)', 'POST', `/api/dashboard/builders/${builderId}/projects`, 200, projBRes.status, projBRes.body);
  const projectBId = projBRes.body?.id ?? null;

  if (projectBId) {
    const submitBRes = await r('POST', `/api/dashboard/projects/${projectBId}/submit-review`, {
      remarks: `Admin test B submission ${TS}`,
    });
    assertStatus(results, 'POST /submit-review (project B → PENDING_REVIEW)', 'POST', `/api/dashboard/projects/${projectBId}/submit-review`, 200, submitBRes.status, submitBRes.body);

    if (is2xx(submitBRes.status)) {
      const rejectBRes = await r('POST', `/api/dashboard/projects/${projectBId}/reject`, {
        remarks: 'ADMIN reject from PENDING_REVIEW',
      });
      assertStatus(results, 'POST /reject (project B → REJECTED)', 'POST', `/api/dashboard/projects/${projectBId}/reject`, 200, rejectBRes.status, rejectBRes.body);
      if (is2xx(rejectBRes.status)) {
        assertEqual(results, 'project B reviewStatus=REJECTED', 'REJECTED', rejectBRes.body?.reviewStatus);
      }

      const reopenBRes = await r('POST', `/api/dashboard/projects/${projectBId}/reopen`, {
        remarks: 'ADMIN reopen from REJECTED',
      });
      assertStatus(results, 'POST /reopen (project B REJECTED → DRAFT)', 'POST', `/api/dashboard/projects/${projectBId}/reopen`, 200, reopenBRes.status, reopenBRes.body);
      if (is2xx(reopenBRes.status)) {
        assertEqual(results, 'project B reviewStatus=DRAFT', 'DRAFT', reopenBRes.body?.reviewStatus);
      }
    }
  }

  // ── Cleanup ──────────────────────────────────────────────────────────────────
  banner('Cleanup — soft-delete test data');

  if (projectBId) {
    const deleteProjBRes = await r('DELETE', `/api/dashboard/projects/${projectBId}`);
    assertStatus(results, `DELETE /projects/${projectBId} (project B)`, 'DELETE', `/api/dashboard/projects/${projectBId}`, [200, 204], deleteProjBRes.status, deleteProjBRes.body);
  }

  // ADMIN soft-deletes project A (APPROVED)
  const deleteProjectRes = await r('DELETE', `/api/dashboard/projects/${projectId}`);
  assertStatus(results, `DELETE /api/dashboard/projects/${projectId}`, 'DELETE', `/api/dashboard/projects/${projectId}`, [200, 204], deleteProjectRes.status, deleteProjectRes.body);

  // ADMIN soft-deletes builder
  const deleteBuilderRes = await r('DELETE', `/api/dashboard/builders/${builderId}`);
  assertStatus(results, `DELETE /api/dashboard/builders/${builderId}`, 'DELETE', `/api/dashboard/builders/${builderId}`, [200, 204], deleteBuilderRes.status, deleteBuilderRes.body);

  // Optionally delete test category
  if (catId) {
    const deleteCatRes = await r('DELETE', `/api/dashboard/categories/${catId}`);
    assertStatus(results, `DELETE /api/dashboard/categories/${catId}`, 'DELETE', `/api/dashboard/categories/${catId}`, [200, 204], deleteCatRes.status, deleteCatRes.body);
  }

  // Optionally delete test city
  if (testCityId) {
    const deleteCityRes = await r('DELETE', `/api/dashboard/cities/${testCityId}`);
    assertStatus(results, `DELETE /api/dashboard/cities/${testCityId}`, 'DELETE', `/api/dashboard/cities/${testCityId}`, [200, 204], deleteCityRes.status, deleteCityRes.body);
  }

  // ─── Summary ─────────────────────────────────────────────────────────────────
  const failed = printSummary(results, 'dashboard-admin-api-test.js');
  process.exit(failed > 0 ? 1 : 0);
}

main().catch(err => {
  console.error('\nUnhandled error:', err);
  process.exit(1);
});
