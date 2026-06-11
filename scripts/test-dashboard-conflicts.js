#!/usr/bin/env node
/**
 * SFS Dashboard — 409 Conflict Regression Test
 *
 * Verifies that project basic-detail updates with expanded PropertyType enum
 * values no longer return 409 CONFLICT after the V92 Flyway migration drops
 * the stale project_property_types_property_type_check constraint.
 *
 * Tests two projects to confirm the fix is consistent.
 *
 * Prerequisites: Node 18+, running SFS backend, V92 migration applied.
 *
 * Usage:
 *   BASE_URL=http://localhost:8080 \
 *   ADMIN_EMAIL=admin@squarefootstory.com \
 *   ADMIN_PASSWORD=Admin@12345 \
 *   PROJECT_ID_1=35 \
 *   PROJECT_ID_2=31 \
 *   node scripts/test-dashboard-conflicts.js
 *
 * Exit code: 0 = all tests passed, 1 = one or more failed.
 */

'use strict';

const { request: req, assertStatus, assertEqual, login, banner, printSummary } = require('./lib/test-helpers');

// ─── Environment ──────────────────────────────────────────────────────────────

const BASE_URL  = (process.env.BASE_URL  || 'http://localhost:8080').replace(/\/$/, '');
const EMAIL     = process.env.ADMIN_EMAIL    || 'admin@squarefootstory.com';
const PASSWORD  = process.env.ADMIN_PASSWORD || 'Admin@12345';
const PROJECT_1 = process.env.PROJECT_ID_1   || '35';
const PROJECT_2 = process.env.PROJECT_ID_2   || '31';

if (!EMAIL || !PASSWORD) {
  console.error('\nERROR: Required env vars: ADMIN_EMAIL, ADMIN_PASSWORD\n');
  process.exit(1);
}

const results = [];

// ─── Expanded enum values that previously caused 409 ──────────────────────────
const EXPANDED_PROPERTY_TYPES = [
  'RESIDENTIAL', 'STUDIO', 'INDEPENDENT_HOUSE', 'BUILDER_FLOOR', 'ROW_HOUSE',
  'PENTHOUSE', 'DUPLEX', 'FARMHOUSE', 'RESIDENTIAL_PLOT', 'COMMERCIAL_PLOT',
  'AGRICULTURAL_LAND', 'OFFICE_SPACE', 'RETAIL_SHOP', 'SHOWROOM', 'FOOD_COURT',
  'CO_WORKING_SPACE', 'MIXED_USE', 'SERVICED_APARTMENT', 'HOTEL', 'RESORT',
  'INDUSTRIAL', 'WAREHOUSE', 'FACTORY', 'INSTITUTIONAL', 'OTHER',
];

// ─── Helpers ──────────────────────────────────────────────────────────────────

function assert(label, method, path, expected, actual, body) {
  return assertStatus(results, label, method, path, expected, actual, body);
}

function eq(label, expected, actual) {
  return assertEqual(results, label, expected, actual);
}

// ─── Main ─────────────────────────────────────────────────────────────────────

(async () => {
  let token;
  try {
    token = await login(BASE_URL, EMAIL, PASSWORD);
    console.log(`\nLogged in as ${EMAIL}`);
  } catch (e) {
    console.error(`\nFATAL: ${e.message}`);
    process.exit(1);
  }

  // ── 1. Fetch both projects — we need their names for PUT requests ─────────────
  banner('1. Verify projects exist and capture current state');

  const projectData = {};
  for (const pid of [PROJECT_1, PROJECT_2]) {
    const r = await req(BASE_URL, token, 'GET', `/api/dashboard/projects/${pid}`);
    assert(`GET project ${pid}`, 'GET', `/api/dashboard/projects/${pid}`, 200, r.status, r.body);
    if (r.status === 200) {
      projectData[pid] = r.body;
      console.log(`    name="${r.body.name}"  propertyTypes=${JSON.stringify(r.body.propertyTypes ?? [])}`);
    }
  }

  // Helper: build a minimal valid PUT payload (name is @NotBlank)
  function putBody(pid, overrides) {
    const p = projectData[pid];
    return { name: p?.name ?? 'Test Project', ...overrides };
  }

  // ── 2. Old allowed values should still work ────────────────────────────────────
  banner('2. Old PropertyType values still accepted (regression guard)');

  const oldTypes = ['APARTMENT', 'VILLA', 'PLOT', 'COMMERCIAL'];
  for (const pid of [PROJECT_1, PROJECT_2]) {
    if (!projectData[pid]) { console.log(`  SKIP project ${pid} (not found)`); continue; }
    const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${pid}`, putBody(pid, { propertyTypes: oldTypes }));
    assert(`PUT project ${pid} with [${oldTypes.join(',')}]`, 'PUT', `/api/dashboard/projects/${pid}`, 200, r.status, r.body);
    if (r.status === 200 && r.body?.propertyTypes) {
      eq(`project ${pid} propertyTypes count`, oldTypes.length, r.body.propertyTypes.length);
    }
  }

  // ── 3. RESIDENTIAL — the originally failing value ─────────────────────────────
  banner('3. RESIDENTIAL — previously failing with 409 (core regression check)');

  for (const pid of [PROJECT_1, PROJECT_2]) {
    if (!projectData[pid]) { console.log(`  SKIP project ${pid} (not found)`); continue; }
    const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${pid}`, putBody(pid, { propertyTypes: ['RESIDENTIAL'] }));
    assert(`PUT project ${pid} propertyTypes=[RESIDENTIAL]`, 'PUT', `/api/dashboard/projects/${pid}`, 200, r.status, r.body);
    if (r.status !== 200) {
      console.log(`    Response: ${JSON.stringify(r.body)}`);
    }
  }

  // ── 4. Mixed old + expanded values ────────────────────────────────────────────
  banner('4. Mixed old + new PropertyType values');

  const mixed = ['APARTMENT', 'RESIDENTIAL', 'VILLA', 'STUDIO'];
  for (const pid of [PROJECT_1, PROJECT_2]) {
    if (!projectData[pid]) { console.log(`  SKIP project ${pid} (not found)`); continue; }
    const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${pid}`, putBody(pid, { propertyTypes: mixed }));
    assert(`PUT project ${pid} mixed types`, 'PUT', `/api/dashboard/projects/${pid}`, 200, r.status, r.body);
  }

  // ── 5. All 25 expanded enum values (batch insert) ────────────────────────────
  banner('5. All 25 expanded PropertyType values — project ' + PROJECT_1);

  if (projectData[PROJECT_1]) {
    const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PROJECT_1}`, putBody(PROJECT_1, { propertyTypes: EXPANDED_PROPERTY_TYPES }));
    assert(`PUT project ${PROJECT_1} all expanded types`, 'PUT', `/api/dashboard/projects/${PROJECT_1}`, 200, r.status, r.body);
    if (r.status === 200 && r.body?.propertyTypes) {
      eq(`project ${PROJECT_1} propertyTypes count`, EXPANDED_PROPERTY_TYPES.length, r.body.propertyTypes.length);
    }
  }

  // ── 6. Empty propertyTypes — collection cleared ───────────────────────────────
  banner('6. Empty propertyTypes — collection cleared');

  for (const pid of [PROJECT_1, PROJECT_2]) {
    if (!projectData[pid]) { console.log(`  SKIP project ${pid} (not found)`); continue; }
    const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${pid}`, putBody(pid, { propertyTypes: [] }));
    assert(`PUT project ${pid} propertyTypes=[]`, 'PUT', `/api/dashboard/projects/${pid}`, 200, r.status, r.body);
    if (r.status === 200 && r.body?.propertyTypes) {
      eq(`project ${pid} propertyTypes empty`, 0, r.body.propertyTypes.length);
    }
  }

  // ── 7. Null propertyTypes — field omitted, entity unchanged ──────────────────
  banner('7. Null propertyTypes — no mutation to existing collection');

  if (projectData[PROJECT_1]) {
    // Set known state
    await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PROJECT_1}`, putBody(PROJECT_1, { propertyTypes: ['APARTMENT', 'VILLA'] }));

    // Update with propertyTypes: null — should leave collection unchanged
    const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PROJECT_1}`, putBody(PROJECT_1, { propertyTypes: null }));
    assert(`PUT project ${PROJECT_1} null propertyTypes`, 'PUT', `/api/dashboard/projects/${PROJECT_1}`, 200, r.status, r.body);
    if (r.status === 200 && r.body?.propertyTypes) {
      eq(`project ${PROJECT_1} propertyTypes unchanged after null`, 2, r.body.propertyTypes.length);
    }
  }

  // ── 8. Invalid enum value — should 400, not 500/409 ──────────────────────────
  banner('8. Invalid enum value → expect 400');

  if (projectData[PROJECT_1]) {
    const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PROJECT_1}`, putBody(PROJECT_1, { propertyTypes: ['NOT_A_REAL_TYPE'] }));
    assert(`PUT project ${PROJECT_1} invalid enum → 400`, 'PUT', `/api/dashboard/projects/${PROJECT_1}`, 400, r.status, r.body);
  }

  // ── 9. Slug conflict between two known projects → 409 ────────────────────────
  banner('9. Slug conflict → expect 409');

  if (projectData[PROJECT_1] && projectData[PROJECT_2]) {
    const slug1 = projectData[PROJECT_1].slug;
    if (slug1) {
      const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PROJECT_2}`, putBody(PROJECT_2, { slug: slug1 }));
      assert(
        `PUT project ${PROJECT_2} with project ${PROJECT_1}'s slug → 409`,
        'PUT', `/api/dashboard/projects/${PROJECT_2}`,
        409, r.status, r.body
      );
      if (r.status === 409) {
        eq('conflict errorCode', 'DASHBOARD_DATA_CONFLICT', r.body?.errorCode);
      }
    } else {
      console.log('  SKIP: project 1 has no slug');
    }
  }

  // ── 10. Construction stage CRUD ───────────────────────────────────────────────
  banner('10. Construction stages — CRUD + duplicate code conflict');

  const METER_BASE = `/api/dashboard/projects/${PROJECT_1}/meter`;
  let stageId = null;

  if (projectData[PROJECT_1]) {
    // Clean up any FOUNDATION stage left by a prior run
    const existingStages = await req(BASE_URL, token, 'GET', `${METER_BASE}/construction-stages`);
    if (Array.isArray(existingStages.body)) {
      for (const s of existingStages.body) {
        if (s.stageCode === 'FOUNDATION') {
          await req(BASE_URL, token, 'DELETE', `${METER_BASE}/construction-stages/${s.id}`);
        }
      }
    }

    // Create
    const createBody = {
      stageCode:        'FOUNDATION',
      stageLabel:       'Foundation Work',
      displayOrder:     1,
      weightPercent:    15,
      progressPercent:  80,
      status:           'IN_PROGRESS',
      plannedStartDate: '2025-01-01',
      plannedEndDate:   '2025-06-01',
      evidenceCount:    3,
      verified:         false,
    };
    const rc = await req(BASE_URL, token, 'POST', `${METER_BASE}/construction-stages`, createBody);
    assert(`POST construction stage`, 'POST', `${METER_BASE}/construction-stages`, 200, rc.status, rc.body);
    if (rc.status === 201) {
      stageId = rc.body?.id;
      eq('stage stageCode', 'FOUNDATION', rc.body?.stageCode);
      eq('stage progressPercent', '80', String(rc.body?.progressPercent));
    }

    if (stageId) {
      // Update
      const ru = await req(BASE_URL, token, 'PUT', `${METER_BASE}/construction-stages/${stageId}`,
        { progressPercent: 90, status: 'COMPLETED', verified: true });
      assert(`PUT construction stage ${stageId}`, 'PUT', `${METER_BASE}/construction-stages/${stageId}`, 200, ru.status, ru.body);
      if (ru.status === 200) {
        eq('updated progressPercent', '90', String(ru.body?.progressPercent));
      }

      // Duplicate stage code → 409
      const dupBody = {
        stageCode: 'FOUNDATION', stageLabel: 'Duplicate Foundation',
        displayOrder: 99, weightPercent: 5, progressPercent: 0,
        status: 'NOT_STARTED', evidenceCount: 0, verified: false,
      };
      const rd = await req(BASE_URL, token, 'POST', `${METER_BASE}/construction-stages`, dupBody);
      assert(`POST duplicate FOUNDATION → 409`, 'POST', `${METER_BASE}/construction-stages`, 409, rd.status, rd.body);
      if (rd.status === 409) {
        eq('conflict errorCode', 'DASHBOARD_DATA_CONFLICT', rd.body?.errorCode);
      }

      // Clean up
      const rdel = await req(BASE_URL, token, 'DELETE', `${METER_BASE}/construction-stages/${stageId}`);
      assert(`DELETE construction stage ${stageId}`, 'DELETE', `${METER_BASE}/construction-stages/${stageId}`, [200, 204], rdel.status, rdel.body);
    }
  }

  // ── Restore original property types on both projects ─────────────────────────
  banner('Restore — reset propertyTypes to original state');

  for (const pid of [PROJECT_1, PROJECT_2]) {
    if (!projectData[pid]) continue;
    const orig = projectData[pid].propertyTypes ?? [];
    const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${pid}`, putBody(pid, { propertyTypes: orig }));
    assert(`Restore project ${pid} propertyTypes`, 'PUT', `/api/dashboard/projects/${pid}`, 200, r.status, r.body);
  }

  // ── Final Summary ─────────────────────────────────────────────────────────────
  const failed = printSummary(results, 'test-dashboard-conflicts.js');
  process.exit(failed > 0 ? 1 : 0);
})();
