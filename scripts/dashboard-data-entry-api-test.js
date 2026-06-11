#!/usr/bin/env node
/**
 * SFS Dashboard — DATA_ENTRY Role API Test Runner
 *
 * Verifies every endpoint DATA_ENTRY is permitted to call (expects 2xx),
 * and every ADMIN/REVIEWER-only endpoint returns 403 when called with a
 * DATA_ENTRY token.
 *
 * Prerequisites: Node 18+ (uses built-in fetch)
 *
 * Usage:
 *   BASE_URL=http://localhost:8080 \
 *   DATA_ENTRY_EMAIL=data@squarefootstory.com \
 *   DATA_ENTRY_PASSWORD=DataEntry@12345 \
 *   node scripts/dashboard-data-entry-api-test.js
 *
 * Exit code: 0 = all tests passed, 1 = one or more tests failed.
 */

'use strict';

// ─── Environment ──────────────────────────────────────────────────────────────

const BASE_URL = (process.env.BASE_URL || '').replace(/\/$/, '');
const EMAIL    = process.env.DATA_ENTRY_EMAIL;
const PASSWORD = process.env.DATA_ENTRY_PASSWORD;

if (!BASE_URL || !EMAIL || !PASSWORD) {
  console.error(
    '\nERROR: The following environment variables are required:\n' +
    '  BASE_URL            e.g. http://localhost:8080\n' +
    '  DATA_ENTRY_EMAIL    dashboard DATA_ENTRY account email\n' +
    '  DATA_ENTRY_PASSWORD dashboard DATA_ENTRY account password\n'
  );
  process.exit(1);
}

// ─── State ────────────────────────────────────────────────────────────────────

let token = null;
const results = [];   // { label, method, path, expected, actual, passed, body }
const TS = Date.now(); // unique suffix for all test data — safe to re-run

// ─── Core helpers ─────────────────────────────────────────────────────────────

async function request(method, path, body) {
  const url     = `${BASE_URL}${path}`;
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(url, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  let json = null;
  try { json = await res.json(); } catch (_) { /* binary / empty body */ }
  return { status: res.status, body: json };
}

/**
 * Core assertion — records the result and prints a one-liner.
 * @param {string}        label    human-readable test name
 * @param {string}        method
 * @param {string}        path
 * @param {number|number[]} expected  single code or array of acceptable codes
 * @param {number}        actual
 * @param {*}             body     response body for failure output
 */
function assertStatus(label, method, path, expected, actual, body) {
  const codes  = Array.isArray(expected) ? expected : [expected];
  const passed = codes.includes(actual);
  results.push({ label, method, path, expected: codes.join(' or '), actual, passed, body });

  if (passed) {
    console.log(`  ✓  [${actual}] ${method} ${path}`);
  } else {
    console.log(`  ✗  [expected ${codes.join('/')} got ${actual}] ${method} ${path}  ← FAILED`);
    if (body) console.log(`       ${JSON.stringify(body)}`);
  }
  return passed;
}

/** Calls an endpoint and asserts a 2xx response (DATA_ENTRY is allowed). */
async function testAllowed(label, method, path, body) {
  const res = await request(method, path, body);
  assertStatus(label, method, path, is2xx(res.status) ? res.status : 200, res.status, res.body);
  return res;
}

function is2xx(s) { return s >= 200 && s < 300; }

/**
 * Like testAllowed but also accepts 400 (for endpoints whose success depends on
 * external state, e.g. submit-review after already being submitted).
 * Still fails on 403 / 5xx.
 */
async function testPermitted(label, method, path, body) {
  const res = await request(method, path, body);
  const ok  = res.status !== 403 && res.status < 500;
  assertStatus(label, method, path, ok ? res.status : 403, res.status, res.body);
  return res;
}

/** Calls an endpoint and asserts exactly 403 (DATA_ENTRY must be forbidden). */
async function testForbidden(label, method, path, body) {
  const res = await request(method, path, body);
  assertStatus(label, method, path, 403, res.status, res.body);
  return res;
}

/** Asserts a 400 response — used for validation/bad-input tests. */
async function testValidation(label, method, path, body) {
  const res = await request(method, path, body);
  assertStatus(label, method, path, 400, res.status, res.body);
  return res;
}

/** Layer 4: numeric equality assertion — avoids string '7.5' vs '7.500000' mismatches. */
function assertNumberEqual(label, expected, actual) {
  const exp = Number(expected);
  const act = Number(actual);
  const passed = !Number.isNaN(exp) && !Number.isNaN(act) && exp === act;
  results.push({
    label   : `    assert: ${label}`,
    method  : '-',
    path    : '',
    expected: String(exp),
    actual  : String(act),
    passed,
    body    : null,
  });
  if (passed) {
    console.log(`    ✓  ${label} = ${act}`);
  } else {
    console.log(`    ✗  ${label}: expected ${exp} got ${act}  ← FAILED`);
  }
}

/** Layer 4: value equality assertion recorded in the shared results array. */
function assertEqual(label, expected, actual) {
  const passed = String(actual) === String(expected);
  results.push({
    label   : `    assert: ${label}`,
    method  : '-',
    path    : '',
    expected: String(expected),
    actual  : String(actual),
    passed,
    body    : null,
  });
  if (passed) {
    console.log(`    ✓  ${label} = "${actual}"`);
  } else {
    console.log(`    ✗  ${label}: expected "${expected}" got "${actual}"  ← FAILED`);
  }
}

// ─── Login ────────────────────────────────────────────────────────────────────

async function loginAsDataEntry() {
  banner('Step 1 — Login as DATA_ENTRY');
  const res = await request('POST', '/api/dashboard/auth/login', { email: EMAIL, password: PASSWORD });
  if (!is2xx(res.status) || !res.body?.accessToken) {
    console.error(`  ✗  Login failed (${res.status}): ${JSON.stringify(res.body)}`);
    process.exit(1);
  }
  token = res.body.accessToken;
  console.log(`  ✓  Login OK — token acquired`);
}

// ─── Banner / Section headers ─────────────────────────────────────────────────

function banner(title) {
  console.log(`\n${'─'.repeat(62)}`);
  console.log(`  ${title}`);
  console.log('─'.repeat(62));
}

// ─── Main ─────────────────────────────────────────────────────────────────────

async function main() {
  console.log('\n' + '═'.repeat(62));
  console.log('  SFS Dashboard — DATA_ENTRY API Test Runner');
  console.log(`  Target  : ${BASE_URL}`);
  console.log(`  Run ID  : ${TS}`);
  console.log('═'.repeat(62));

  // ── Step 1: Login ────────────────────────────────────────────────────────────
  await loginAsDataEntry();

  // ── Step 2: Verify identity ──────────────────────────────────────────────────
  banner('Step 2 — Verify identity (/me)');
  const me = await request('GET', '/api/dashboard/auth/me');
  assertStatus('GET /api/dashboard/auth/me', 'GET', '/api/dashboard/auth/me', 200, me.status, me.body);
  if (me.body?.role !== 'DATA_ENTRY') {
    console.error(`  ✗  Expected role DATA_ENTRY, got "${me.body?.role}". Aborting.`);
    process.exit(1);
  }
  console.log(`  ✓  Confirmed: role = DATA_ENTRY`);

  // ── Step 3: Fetch dependency data ────────────────────────────────────────────
  banner('Step 3 — Fetch dependency data (city)');
  let cityId = null;
  // GET /api/dashboard/cities returns a plain List (array), not a Spring Page object.
  const citiesRes = await request('GET', '/api/dashboard/cities');
  const citiesArray = Array.isArray(citiesRes.body) ? citiesRes.body
    : Array.isArray(citiesRes.body?.content) ? citiesRes.body.content
    : [];
  if (is2xx(citiesRes.status) && citiesArray.length > 0) {
    cityId = citiesArray[0].id;
    console.log(`  ✓  Using cityId = ${cityId} ("${citiesArray[0].name}")`);
  } else {
    console.log(`  !  No cities found — builder/project will be created without cityId`);
  }

  // ── Step 4: Create test builder ──────────────────────────────────────────────
  banner('Step 4 — Create test builder');
  const builderPayload = {
    name    : `Test Builder ${TS}`,
    phone   : '9999999990',
    priority: 0,
    active  : true,
    ...(cityId ? { cityId } : {}),
  };
  const builderRes = await request('POST', '/api/dashboard/builders', builderPayload);
  assertStatus(
    'POST /api/dashboard/builders (create)',
    'POST', '/api/dashboard/builders', 200, builderRes.status, builderRes.body
  );
  if (!is2xx(builderRes.status)) {
    console.error('  ✗  Cannot continue without a builderId. Aborting.');
    process.exit(1);
  }
  const builderId = builderRes.body.id;
  console.log(`  ✓  builderId = ${builderId}`);
  // Layer 4 — verify response body matches sent payload
  assertEqual('builder.name matches payload', builderPayload.name, builderRes.body?.name);

  // ── Step 5: Create test project ──────────────────────────────────────────────
  banner('Step 5 — Create test project');
  const projectPayload = {
    name          : `Test Project ${TS}`,
    slug          : `test-project-${TS}`,
    status        : 'UNDER_CONSTRUCTION',
    propertyTypes : ['APARTMENT'],
    priority      : 0,
    active        : true,
    ...(cityId ? { cityId } : {}),
  };
  const projectRes = await request('POST', `/api/dashboard/builders/${builderId}/projects`, projectPayload);
  assertStatus(
    `POST /api/dashboard/builders/${builderId}/projects (create)`,
    'POST', `/api/dashboard/builders/${builderId}/projects`, 200, projectRes.status, projectRes.body
  );
  if (!is2xx(projectRes.status)) {
    console.error('  ✗  Cannot continue without a projectId. Aborting.');
    process.exit(1);
  }
  const projectId = projectRes.body.id;
  console.log(`  ✓  projectId = ${projectId}`);
  // Layer 4 — new project must start with published=false
  assertEqual('project.published is false on creation', 'false', String(projectRes.body?.published));
  // Layer 4 — check reviewStatus=DRAFT via review-status endpoint
  const initialReviewRes = await request('GET', `/api/dashboard/projects/${projectId}/review-status`);
  assertEqual('project.reviewStatus is DRAFT on creation', 'DRAFT', initialReviewRes.body?.reviewStatus);

  // ════════════════════════════════════════════════════════════════════════════
  //  SECTION A — ALLOWED APIs  (DATA_ENTRY must receive 2xx)
  // ════════════════════════════════════════════════════════════════════════════
  banner('SECTION A — ALLOWED APIs');

  // Overview
  await testAllowed('GET /api/dashboard/overview', 'GET', '/api/dashboard/overview');

  // Builders
  await testAllowed('GET /api/dashboard/builders (list)', 'GET', '/api/dashboard/builders?page=0&size=5');
  await testAllowed('GET /api/dashboard/builders/{id}', 'GET', `/api/dashboard/builders/${builderId}`);

  // Builder logo update (new PATCH endpoint added for DATA_ENTRY)
  await testAllowed(
    'PATCH /api/dashboard/builders/{id}/logo',
    'PATCH', `/api/dashboard/builders/${builderId}/logo`,
    { logoUrl: 'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/dashboard/builders/test/logo.webp' }
  );

  // Projects
  await testAllowed('GET /api/dashboard/projects (list)', 'GET', '/api/dashboard/projects?page=0&size=5');
  await testAllowed('GET /api/dashboard/projects/{id}', 'GET', `/api/dashboard/projects/${projectId}`);

  // Layer 4 — workspace must include project, review, and collection arrays
  const workspaceRes = await request('GET', `/api/dashboard/projects/${projectId}/workspace`);
  assertStatus('GET /api/dashboard/projects/{id}/workspace', 'GET', `/api/dashboard/projects/${projectId}/workspace`, 200, workspaceRes.status, workspaceRes.body);
  if (workspaceRes.body) {
    assertEqual('workspace.project exists', 'object', typeof workspaceRes.body.project);
    assertEqual('workspace.review exists', 'object', typeof workspaceRes.body.review);
    assertEqual('workspace.media is array', 'true', String(Array.isArray(workspaceRes.body.media)));
    assertEqual('workspace.highlights is array', 'true', String(Array.isArray(workspaceRes.body.highlights)));
    assertEqual('workspace.floorPlans is array', 'true', String(Array.isArray(workspaceRes.body.floorPlans)));
  }

  await testAllowed(
    'PUT /api/dashboard/projects/{id}',
    'PUT', `/api/dashboard/projects/${projectId}`,
    { ...projectPayload, description: `Updated by test run ${TS}` }
  );

  // Reference data reads (DATA_ENTRY needs these to fill forms)
  // Both endpoints return plain List — no pagination params.
  await testAllowed('GET /api/dashboard/categories (list)', 'GET', '/api/dashboard/categories');
  await testAllowed('GET /api/dashboard/cities (list)', 'GET', '/api/dashboard/cities');

  // ── Project Media ────────────────────────────────────────────────────────────
  console.log('\n  [project media]');
  const mediaRes = await request('POST', `/api/dashboard/projects/${projectId}/media`, {
    mediaType : 'IMAGE',
    url       : 'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/test/image.jpg',
    sortOrder : 0,
    active    : true,
  });
  assertStatus('POST /api/dashboard/projects/{id}/media', 'POST', `/api/dashboard/projects/${projectId}/media`, 200, mediaRes.status, mediaRes.body);
  const mediaId = mediaRes.body?.id ?? null;

  await testAllowed('GET /api/dashboard/projects/{id}/media', 'GET', `/api/dashboard/projects/${projectId}/media`);

  if (mediaId) {
    const updatedUrl = 'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/test/image-v2.jpg';
    await testAllowed(
      'PUT /api/dashboard/projects/{id}/media/{mediaId}',
      'PUT', `/api/dashboard/projects/${projectId}/media/${mediaId}`,
      { mediaType: 'IMAGE', url: updatedUrl, sortOrder: 0, active: true }
    );
    // Layer 4 — verify updated URL persisted by re-reading the list
    const mediaListRes = await request('GET', `/api/dashboard/projects/${projectId}/media`);
    const updatedMedia = Array.isArray(mediaListRes.body)
      ? mediaListRes.body.find(m => m.id === mediaId)
      : null;
    assertEqual('Updated media URL persisted', updatedUrl, updatedMedia?.url);
  }

  // ── Project Highlights ───────────────────────────────────────────────────────
  console.log('\n  [project highlights]');
  const highlightRes = await request('POST', `/api/dashboard/projects/${projectId}/highlights`, {
    title    : `Test Highlight ${TS}`,
    subtitle : 'Auto-generated by test runner',
    sortOrder: 0,
    active   : true,
  });
  assertStatus('POST /api/dashboard/projects/{id}/highlights', 'POST', `/api/dashboard/projects/${projectId}/highlights`, 200, highlightRes.status, highlightRes.body);
  const highlightId = highlightRes.body?.id ?? null;

  await testAllowed('GET /api/dashboard/projects/{id}/highlights', 'GET', `/api/dashboard/projects/${projectId}/highlights`);

  if (highlightId) {
    const updatedTitle = `Updated Highlight ${TS}`;
    const highlightPutRes = await request(
      'PUT', `/api/dashboard/projects/${projectId}/highlights/${highlightId}`,
      { title: updatedTitle, sortOrder: 0, active: true }
    );
    assertStatus('PUT .../highlights/{id}', 'PUT', `/api/dashboard/projects/${projectId}/highlights/${highlightId}`, 200, highlightPutRes.status, highlightPutRes.body);
    // Layer 4 — verify title change reflected in response
    assertEqual('Updated highlight title in response', updatedTitle, highlightPutRes.body?.title);
  }

  // ── Floor Plans ──────────────────────────────────────────────────────────────
  console.log('\n  [floor plans]');
  const floorPlanRes = await request('POST', `/api/dashboard/projects/${projectId}/floor-plans`, {
    title    : `2BHK Type A ${TS}`,
    imageUrl : 'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/test/floor-plan.jpg',
    sortOrder: 0,
    active   : true,
  });
  assertStatus('POST /api/dashboard/projects/{id}/floor-plans', 'POST', `/api/dashboard/projects/${projectId}/floor-plans`, 200, floorPlanRes.status, floorPlanRes.body);
  const floorPlanId = floorPlanRes.body?.id ?? null;

  await testAllowed('GET /api/dashboard/projects/{id}/floor-plans', 'GET', `/api/dashboard/projects/${projectId}/floor-plans`);

  if (floorPlanId) {
    await testAllowed(
      'PUT /api/dashboard/projects/{id}/floor-plans/{floorPlanId}',
      'PUT', `/api/dashboard/projects/${projectId}/floor-plans/${floorPlanId}`,
      {
        title    : `3BHK Type A ${TS}`,
        imageUrl : 'https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/test/floor-plan-v2.jpg',
        sortOrder: 0,
        active   : true,
      }
    );
  }

  // ── Connectivity ─────────────────────────────────────────────────────────────
  console.log('\n  [connectivity]');
  await testAllowed(
    'PUT /api/dashboard/projects/{id}/connectivity',
    'PUT', `/api/dashboard/projects/${projectId}/connectivity`,
    { title: `Connectivity ${TS}`, subtitle: 'Well connected test project', active: true }
  );
  await testAllowed('GET /api/dashboard/projects/{id}/connectivity', 'GET', `/api/dashboard/projects/${projectId}/connectivity`);

  const placeRes = await request('POST', `/api/dashboard/projects/${projectId}/connectivity/places`, {
    placeName    : `Test Metro Station ${TS}`,
    placeType    : 'METRO',
    distanceLabel: '1.2 km',
    sortOrder    : 0,
    active       : true,
  });
  assertStatus('POST /api/dashboard/projects/{id}/connectivity/places', 'POST', `/api/dashboard/projects/${projectId}/connectivity/places`, 200, placeRes.status, placeRes.body);
  const placeId = placeRes.body?.id ?? null;

  await testAllowed('GET /api/dashboard/projects/{id}/connectivity/places', 'GET', `/api/dashboard/projects/${projectId}/connectivity/places`);

  if (placeId) {
    await testAllowed(
      'PUT /api/dashboard/projects/{id}/connectivity/places/{placeId}',
      'PUT', `/api/dashboard/projects/${projectId}/connectivity/places/${placeId}`,
      { placeName: `Test Metro Updated ${TS}`, placeType: 'METRO', distanceLabel: '1.0 km', sortOrder: 0, active: true }
    );
  }

  // ── Construction Stages ──────────────────────────────────────────────────────
  console.log('\n  [construction stages]');
  const stageRes = await request('POST', `/api/dashboard/projects/${projectId}/meter/construction-stages`, {
    stageCode      : 'FOUNDATION',
    stageLabel     : `Foundation ${TS}`,
    displayOrder   : 0,
    weightPercent  : 10,
    progressPercent: 40,
    status         : 'IN_PROGRESS',
  });
  assertStatus('POST /api/dashboard/projects/{id}/meter/construction-stages', 'POST', `/api/dashboard/projects/${projectId}/meter/construction-stages`, 200, stageRes.status, stageRes.body);
  const stageId = stageRes.body?.id ?? null;

  await testAllowed('GET /api/dashboard/projects/{id}/meter/construction-stages', 'GET', `/api/dashboard/projects/${projectId}/meter/construction-stages`);

  if (stageId) {
    await testAllowed(
      'PUT /api/dashboard/projects/{id}/meter/construction-stages/{stageId}',
      'PUT', `/api/dashboard/projects/${projectId}/meter/construction-stages/${stageId}`,
      { stageCode: 'FOUNDATION', stageLabel: `Foundation Updated ${TS}`, displayOrder: 0, weightPercent: 10, progressPercent: 75, status: 'IN_PROGRESS' }
    );
  }

  // ── Compliance Items ─────────────────────────────────────────────────────────
  console.log('\n  [compliance items]');
  const complianceRes = await request('POST', `/api/dashboard/projects/${projectId}/meter/compliance-items`, {
    itemGroup   : 'RERA',
    itemKey     : `RERA_REG_${TS}`,
    itemLabel   : 'RERA Registration',
    status      : 'OBTAINED',
    displayOrder: 0,
    verified    : false,
  });
  assertStatus('POST /api/dashboard/projects/{id}/meter/compliance-items', 'POST', `/api/dashboard/projects/${projectId}/meter/compliance-items`, 200, complianceRes.status, complianceRes.body);
  const complianceId = complianceRes.body?.id ?? null;

  await testAllowed('GET /api/dashboard/projects/{id}/meter/compliance-items', 'GET', `/api/dashboard/projects/${projectId}/meter/compliance-items`);

  if (complianceId) {
    await testAllowed(
      'PUT /api/dashboard/projects/{id}/meter/compliance-items/{itemId}',
      'PUT', `/api/dashboard/projects/${projectId}/meter/compliance-items/${complianceId}`,
      { itemGroup: 'RERA', itemKey: `RERA_REG_${TS}`, itemLabel: 'RERA Registration (updated)', status: 'VERIFIED', displayOrder: 0, verified: true }
    );
  }

  // ── Amenities ────────────────────────────────────────────────────────────────
  console.log('\n  [amenities]');
  const amenityRes = await request('POST', `/api/dashboard/projects/${projectId}/meter/amenities`, {
    amenityCode   : `POOL_${TS}`,
    amenityLabel  : 'Swimming Pool',
    status        : 'IN_PROGRESS',
    progressPercent: 60,
    weightPercent : 5,
    displayOrder  : 0,
    verified      : false,
  });
  assertStatus('POST /api/dashboard/projects/{id}/meter/amenities', 'POST', `/api/dashboard/projects/${projectId}/meter/amenities`, 200, amenityRes.status, amenityRes.body);
  const amenityId = amenityRes.body?.id ?? null;

  await testAllowed('GET /api/dashboard/projects/{id}/meter/amenities', 'GET', `/api/dashboard/projects/${projectId}/meter/amenities`);

  if (amenityId) {
    await testAllowed(
      'PUT /api/dashboard/projects/{id}/meter/amenities/{amenityId}',
      'PUT', `/api/dashboard/projects/${projectId}/meter/amenities/${amenityId}`,
      { amenityCode: `POOL_${TS}`, amenityLabel: 'Swimming Pool (updated)', status: 'COMPLETED', progressPercent: 100, weightPercent: 5, displayOrder: 0, verified: true }
    );
  }

  // ── Price History ────────────────────────────────────────────────────────────
  console.log('\n  [price history]');
  const priceHistoryRes = await request('POST', `/api/dashboard/projects/${projectId}/meter/price-history`, {
    yearLabel      : '2024',
    projectPrice   : 5000000,
    averageAreaPrice: 8000,
    displayOrder   : 0,
    verified       : false,
  });
  assertStatus('POST /api/dashboard/projects/{id}/meter/price-history', 'POST', `/api/dashboard/projects/${projectId}/meter/price-history`, 200, priceHistoryRes.status, priceHistoryRes.body);
  const priceHistoryId = priceHistoryRes.body?.id ?? null;

  await testAllowed('GET /api/dashboard/projects/{id}/meter/price-history', 'GET', `/api/dashboard/projects/${projectId}/meter/price-history`);

  if (priceHistoryId) {
    await testAllowed(
      'PUT /api/dashboard/projects/{id}/meter/price-history/{priceHistoryId}',
      'PUT', `/api/dashboard/projects/${projectId}/meter/price-history/${priceHistoryId}`,
      { yearLabel: '2024', projectPrice: 5500000, averageAreaPrice: 8500, displayOrder: 0, verified: false }
    );
  }

  // ── Payment Milestones ───────────────────────────────────────────────────────
  console.log('\n  [payment milestones]');
  const milestoneRes = await request('POST', `/api/dashboard/projects/${projectId}/meter/payment-milestones`, {
    milestoneCode : `BOOKING_${TS}`,
    milestoneLabel: 'Booking Amount',
    percentageValue: 10,
    displayOrder  : 0,
    active        : true,
  });
  assertStatus('POST /api/dashboard/projects/{id}/meter/payment-milestones', 'POST', `/api/dashboard/projects/${projectId}/meter/payment-milestones`, 200, milestoneRes.status, milestoneRes.body);
  const milestoneId = milestoneRes.body?.id ?? null;

  await testAllowed('GET /api/dashboard/projects/{id}/meter/payment-milestones', 'GET', `/api/dashboard/projects/${projectId}/meter/payment-milestones`);

  if (milestoneId) {
    await testAllowed(
      'PUT /api/dashboard/projects/{id}/meter/payment-milestones/{milestoneId}',
      'PUT', `/api/dashboard/projects/${projectId}/meter/payment-milestones/${milestoneId}`,
      { milestoneCode: `BOOKING_${TS}`, milestoneLabel: 'Booking Amount (updated)', percentageValue: 15, displayOrder: 0, active: true }
    );
  }

  // ── Cost Breakdown ───────────────────────────────────────────────────────────
  console.log('\n  [cost breakdown]');
  await testAllowed(
    'PUT /api/dashboard/projects/{id}/meter/cost-breakdown',
    'PUT', `/api/dashboard/projects/${projectId}/meter/cost-breakdown`,
    { landCost: 10000000, constructionCost: 50000000, totalCost: 60000000, verified: false }
  );
  await testAllowed('GET /api/dashboard/projects/{id}/meter/cost-breakdown', 'GET', `/api/dashboard/projects/${projectId}/meter/cost-breakdown`);

  // ── Land Utilization ─────────────────────────────────────────────────────────
  console.log('\n  [land utilization]');
  await testAllowed(
    'PUT /api/dashboard/projects/{id}/meter/land-utilization',
    'PUT', `/api/dashboard/projects/${projectId}/meter/land-utilization`,
    { totalLandAreaSqm: 10000.0, residentialAreaSqm: 7000.0, openAreaSqm: 2000.0, parkingAreaSqm: 1000.0 }
  );
  await testAllowed('GET /api/dashboard/projects/{id}/meter/land-utilization', 'GET', `/api/dashboard/projects/${projectId}/meter/land-utilization`);

  // ── Location Score ───────────────────────────────────────────────────────────
  console.log('\n  [location score]');
  const locationScorePayload = { metroScore: 7.5, educationScore: 8.0, healthcareScore: 6.5, finalScore: 7.3, verified: false };
  await testAllowed(
    'PUT /api/dashboard/projects/{id}/meter/location-score',
    'PUT', `/api/dashboard/projects/${projectId}/meter/location-score`,
    locationScorePayload
  );
  // Layer 4 — verify values actually persisted
  const locationScoreGetRes = await request('GET', `/api/dashboard/projects/${projectId}/meter/location-score`);
  assertStatus('GET /meter/location-score (verify persistence)', 'GET', `/api/dashboard/projects/${projectId}/meter/location-score`, 200, locationScoreGetRes.status, locationScoreGetRes.body);
  if (is2xx(locationScoreGetRes.status)) {
    assertNumberEqual('location metroScore persisted',     7.5, locationScoreGetRes.body?.metroScore);
    assertNumberEqual('location educationScore persisted', 8.0, locationScoreGetRes.body?.educationScore);
    assertNumberEqual('location finalScore persisted',     7.3, locationScoreGetRes.body?.finalScore);
  }

  // ── Review ───────────────────────────────────────────────────────────────────
  console.log('\n  [review]');
  await testAllowed('GET /api/dashboard/projects/{id}/review-status', 'GET', `/api/dashboard/projects/${projectId}/review-status`);

  // submit-review may legitimately return 400 if the project fails internal
  // readiness checks (e.g. missing required fields). We only care that DATA_ENTRY
  // is not blocked with 403 and the server does not crash (no 5xx).
  // Layer 4 — if submit succeeds, assert reviewStatus transitions to PENDING_REVIEW.
  const submitReviewRes = await request(
    'POST', `/api/dashboard/projects/${projectId}/submit-review`,
    { remarks: `Test submission by run ${TS}` }
  );
  const submitOk = submitReviewRes.status !== 403 && submitReviewRes.status < 500;
  assertStatus(
    'POST /api/dashboard/projects/{id}/submit-review (permitted — not 403)',
    'POST', `/api/dashboard/projects/${projectId}/submit-review`,
    submitOk ? submitReviewRes.status : 403,
    submitReviewRes.status, submitReviewRes.body
  );
  if (is2xx(submitReviewRes.status)) {
    assertEqual('submit-review → reviewStatus=PENDING_REVIEW', 'PENDING_REVIEW', submitReviewRes.body?.reviewStatus);
    assertEqual('submit-review → published=false', 'false', String(submitReviewRes.body?.published));
  }

  // ── Review field issues & history (read-only for DATA_ENTRY) ─────────────────
  console.log('\n  [reviews — read]');
  // entityType and entityId are @RequestParam (required) on both endpoints.
  await testAllowed('GET /api/dashboard/reviews/field-issues', 'GET', `/api/dashboard/reviews/field-issues?entityType=PROJECT&entityId=${projectId}&activeOnly=true`);
  await testAllowed('GET /api/dashboard/reviews/history', 'GET', `/api/dashboard/reviews/history?entityType=PROJECT&entityId=${projectId}`);
  // NOTE: PATCH /field-issues/{id}/fixed is also allowed for DATA_ENTRY but
  // requires a field-issue created by ADMIN or REVIEWER. It cannot be tested
  // here without a separate ADMIN token. Skipping intentionally.

  // ── Field Help ───────────────────────────────────────────────────────────────
  console.log('\n  [field help]');
  // module is a required @RequestParam DashboardHelpModule enum value.
  await testAllowed('GET /api/dashboard/field-help', 'GET', '/api/dashboard/field-help?module=PROJECT_METER_CONSTRUCTION_STAGE');

  // ── Scraping Candidates ──────────────────────────────────────────────────────
  console.log('\n  [scraping candidates]');
  await testAllowed('GET /api/dashboard/scraping/candidates', 'GET', '/api/dashboard/scraping/candidates?page=0&size=5');

  const candidatesListRes = await request('GET', '/api/dashboard/scraping/candidates?page=0&size=1');
  if (is2xx(candidatesListRes.status) && candidatesListRes.body?.content?.length > 0) {
    const candidateId = candidatesListRes.body.content[0].id;
    console.log(`  ℹ  Found candidateId = ${candidateId}`);
    await testAllowed('GET /api/dashboard/scraping/candidates/{id}', 'GET', `/api/dashboard/scraping/candidates/${candidateId}`);
    // Screenshot may return image/binary — only the status code matters here.
    await testPermitted('GET /api/dashboard/scraping/candidates/{id}/screenshot (permitted)', 'GET', `/api/dashboard/scraping/candidates/${candidateId}/screenshot`);
  } else {
    console.log('  ℹ  No scraping candidates in DB — candidate sub-resource tests skipped');
  }

  // ════════════════════════════════════════════════════════════════════════════
  //  SECTION B — FORBIDDEN APIs  (DATA_ENTRY must receive 403)
  // ════════════════════════════════════════════════════════════════════════════
  banner('SECTION B — FORBIDDEN APIs  (must all return 403)');

  // Builder — admin-only mutations
  await testForbidden(
    'PUT /api/dashboard/builders/{id} must be 403',
    'PUT', `/api/dashboard/builders/${builderId}`,
    { name: 'Should Not Update', priority: 0, active: true }
  );
  await testForbidden('PATCH /api/dashboard/builders/{id}/published must be 403', 'PATCH', `/api/dashboard/builders/${builderId}/published?value=true`);
  await testForbidden('DELETE /api/dashboard/builders/{id} must be 403', 'DELETE', `/api/dashboard/builders/${builderId}`);

  // Project — admin-only mutations
  await testForbidden('PATCH /api/dashboard/projects/{id}/published must be 403', 'PATCH', `/api/dashboard/projects/${projectId}/published?value=true`);
  await testForbidden('PATCH /api/dashboard/projects/{id}/active must be 403', 'PATCH', `/api/dashboard/projects/${projectId}/active?value=false`);
  await testForbidden('DELETE /api/dashboard/projects/{id} must be 403', 'DELETE', `/api/dashboard/projects/${projectId}`);

  // Review decisions (ADMIN / REVIEWER only)
  await testForbidden('POST /api/dashboard/projects/{id}/approve must be 403', 'POST', `/api/dashboard/projects/${projectId}/approve`, { remarks: 'forbidden test' });
  await testForbidden('POST /api/dashboard/projects/{id}/reject must be 403', 'POST', `/api/dashboard/projects/${projectId}/reject`, { remarks: 'forbidden test' });
  await testForbidden('POST /api/dashboard/projects/{id}/reopen must be 403', 'POST', `/api/dashboard/projects/${projectId}/reopen`, { remarks: 'forbidden test' });

  // Media delete (admin-only)
  if (mediaId) {
    await testForbidden('DELETE /api/dashboard/projects/{id}/media/{mediaId} must be 403', 'DELETE', `/api/dashboard/projects/${projectId}/media/${mediaId}`);
  }

  // Highlight delete (admin-only)
  if (highlightId) {
    await testForbidden('DELETE /api/dashboard/projects/{id}/highlights/{highlightId} must be 403', 'DELETE', `/api/dashboard/projects/${projectId}/highlights/${highlightId}`);
  }

  // Floor plan delete + toggle-active (admin-only)
  if (floorPlanId) {
    await testForbidden('PATCH /api/dashboard/projects/{id}/floor-plans/{floorPlanId}/active must be 403', 'PATCH', `/api/dashboard/projects/${projectId}/floor-plans/${floorPlanId}/active?active=false`);
    await testForbidden('DELETE /api/dashboard/projects/{id}/floor-plans/{floorPlanId} must be 403', 'DELETE', `/api/dashboard/projects/${projectId}/floor-plans/${floorPlanId}`);
  }

  // Connectivity place delete (admin-only)
  if (placeId) {
    await testForbidden('DELETE /api/dashboard/projects/{id}/connectivity/places/{placeId} must be 403', 'DELETE', `/api/dashboard/projects/${projectId}/connectivity/places/${placeId}`);
  }

  // Meter item deletes (admin-only)
  if (stageId) {
    await testForbidden('DELETE /api/dashboard/projects/{id}/meter/construction-stages/{stageId} must be 403', 'DELETE', `/api/dashboard/projects/${projectId}/meter/construction-stages/${stageId}`);
  }
  if (complianceId) {
    await testForbidden('DELETE /api/dashboard/projects/{id}/meter/compliance-items/{itemId} must be 403', 'DELETE', `/api/dashboard/projects/${projectId}/meter/compliance-items/${complianceId}`);
  }
  if (amenityId) {
    await testForbidden('DELETE /api/dashboard/projects/{id}/meter/amenities/{amenityId} must be 403', 'DELETE', `/api/dashboard/projects/${projectId}/meter/amenities/${amenityId}`);
  }
  if (priceHistoryId) {
    await testForbidden('DELETE /api/dashboard/projects/{id}/meter/price-history/{priceHistoryId} must be 403', 'DELETE', `/api/dashboard/projects/${projectId}/meter/price-history/${priceHistoryId}`);
  }
  if (milestoneId) {
    await testForbidden('DELETE /api/dashboard/projects/{id}/meter/payment-milestones/{milestoneId} must be 403', 'DELETE', `/api/dashboard/projects/${projectId}/meter/payment-milestones/${milestoneId}`);
  }

  // Meter snapshot recalculate (ADMIN / REVIEWER only)
  await testForbidden('POST /api/dashboard/projects/{id}/meter/snapshot/recalculate must be 403', 'POST', `/api/dashboard/projects/${projectId}/meter/snapshot/recalculate`);

  // Review write (ADMIN / REVIEWER only)
  await testForbidden(
    'POST /api/dashboard/reviews/field-issues must be 403',
    'POST', '/api/dashboard/reviews/field-issues',
    { entityType: 'PROJECT', entityId: projectId, fieldKey: 'name', status: 'WRONG' }
  );
  await testForbidden('DELETE /api/dashboard/reviews/field-issues/999999 must be 403', 'DELETE', '/api/dashboard/reviews/field-issues/999999');

  // Audit log (ADMIN only)
  await testForbidden('GET /api/dashboard/audit must be 403', 'GET', '/api/dashboard/audit?page=0&size=5');

  // Category / city write (ADMIN only)
  await testForbidden('POST /api/dashboard/categories must be 403', 'POST', '/api/dashboard/categories', { name: 'Forbidden Category', slug: 'forbidden-category-test', active: true });
  await testForbidden('POST /api/dashboard/cities must be 403', 'POST', '/api/dashboard/cities', { name: `Forbidden City ${TS}`, active: true });

  // Field-help write (ADMIN only)
  await testForbidden('PUT /api/dashboard/field-help must be 403', 'PUT', '/api/dashboard/field-help',
    { module: 'PROJECT_BASIC', fieldKey: 'name', fieldLabel: 'Project Name', shortHelp: 'Forbidden test' });

  // Scraping write (ADMIN only)
  await testForbidden('POST /api/dashboard/scraping/rera/search-by-number must be 403', 'POST', '/api/dashboard/scraping/rera/search-by-number',
    { sourceCode: 'HARYANA_RERA', reraNumber: 'FORBIDDEN-TEST-123' });
  await testForbidden('POST /api/dashboard/scraping/candidates must be 403', 'POST', '/api/dashboard/scraping/candidates',
    { sourceCode: 'HARYANA_RERA', reraNumber: 'FORBIDDEN-TEST-123' });

  // ════════════════════════════════════════════════════════════════════════════
  //  SECTION C — LAYER 1: DTO VALIDATION  (invalid payloads must return 400)
  //  Each call targets an allowed endpoint; the 400 is from @Valid, not auth.
  //  A fresh project is used so ownership / state-machine checks don't interfere.
  // ════════════════════════════════════════════════════════════════════════════
  banner('SECTION C — VALIDATION (must all return 400)');

  // Create a minimal valid builder to use as parent for project validation tests.
  const valBuilderRes = await request('POST', '/api/dashboard/builders', {
    name: `Val Builder ${TS}`, priority: 0, active: true,
  });
  const valBuilderId = valBuilderRes.body?.id ?? builderId; // fall back to main builder if create fails

  // Create a fresh project in DRAFT for meter-item validation tests.
  const valProjectRes = await request('POST', `/api/dashboard/builders/${valBuilderId}/projects`, {
    name: `Val Project ${TS}`, slug: `val-project-${TS}`,
    status: 'UNDER_CONSTRUCTION', propertyTypes: ['APARTMENT'],
    priority: 0, active: true,
  });
  const valProjectId = valProjectRes.body?.id ?? projectId;

  // ── Builder-level validation ─────────────────────────────────────────────────
  await testValidation(
    'POST /builders: blank name → 400',
    'POST', '/api/dashboard/builders',
    { name: '', priority: 0, active: true }
  );
  await testValidation(
    'POST /builders: invalid email format → 400',
    'POST', '/api/dashboard/builders',
    { name: `Builder Email Fail ${TS}`, email: 'not-an-email', priority: 0, active: true }
  );
  await testValidation(
    'POST /builders: latitude > 90 → 400',
    'POST', '/api/dashboard/builders',
    { name: `Builder Lat Fail ${TS}`, latitude: 91.0, priority: 0, active: true }
  );
  await testValidation(
    'POST /builders: longitude > 180 → 400',
    'POST', '/api/dashboard/builders',
    { name: `Builder Lon Fail ${TS}`, longitude: 181.0, priority: 0, active: true }
  );

  // ── Project-level validation ─────────────────────────────────────────────────
  await testValidation(
    'POST /projects: invalid status value → 400',
    'POST', `/api/dashboard/builders/${valBuilderId}/projects`,
    { name: `Proj Status Fail ${TS}`, slug: `ps-fail-${TS}`, status: 'INVALID_STATUS', propertyTypes: ['APARTMENT'] }
  );
  await testValidation(
    'POST /projects: invalid propertyTypes value → 400',
    'POST', `/api/dashboard/builders/${valBuilderId}/projects`,
    { name: `Proj Prop Fail ${TS}`, slug: `pp-fail-${TS}`, status: 'UNDER_CONSTRUCTION', propertyTypes: ['NOT_A_TYPE'] }
  );
  await testValidation(
    'POST /projects: negative priceMin → 400',
    'POST', `/api/dashboard/builders/${valBuilderId}/projects`,
    { name: `Proj Price Fail ${TS}`, slug: `pprice-fail-${TS}`, status: 'UNDER_CONSTRUCTION', propertyTypes: ['APARTMENT'], priceMin: -1 }
  );
  await testValidation(
    'POST /projects: invalid possessionDate format → 400',
    'POST', `/api/dashboard/builders/${valBuilderId}/projects`,
    { name: `Proj Date Fail ${TS}`, slug: `pdate-fail-${TS}`, status: 'UNDER_CONSTRUCTION', propertyTypes: ['APARTMENT'], possessionDate: 'not-a-date' }
  );

  // ── Meter-item validation (uses fresh DRAFT project) ─────────────────────────
  await testValidation(
    'POST /meter/construction-stages: missing required stageCode → 400',
    'POST', `/api/dashboard/projects/${valProjectId}/meter/construction-stages`,
    { stageLabel: 'No Code Stage', displayOrder: 0, weightPercent: 10 }
    // stageCode is @NotNull
  );
  await testValidation(
    'POST /meter/construction-stages: progressPercent > 100 → 400',
    'POST', `/api/dashboard/projects/${valProjectId}/meter/construction-stages`,
    { stageCode: 'FOUNDATION', stageLabel: 'Over Progress', displayOrder: 0, weightPercent: 10, progressPercent: 101 }
  );

  // ════════════════════════════════════════════════════════════════════════════
  //  SUMMARY
  // ════════════════════════════════════════════════════════════════════════════
  const total  = results.length;
  const passed = results.filter(r => r.passed).length;
  const failed = results.filter(r => !r.passed).length;

  console.log('\n' + '═'.repeat(62));
  console.log('  TEST SUMMARY');
  console.log('═'.repeat(62));

  // Full results table
  const labelW = Math.min(Math.max(...results.map(r => r.label.length)), 60);
  console.log(
    `\n  ${'RESULT'.padEnd(6)}  ${'EXP'.padStart(3)}  ${'ACT'.padStart(3)}  ${'ENDPOINT / TEST'.padEnd(labelW)}`
  );
  console.log(`  ${'─'.repeat(6)}  ${'─'.repeat(3)}  ${'─'.repeat(3)}  ${'─'.repeat(labelW)}`);
  results.forEach(r => {
    const icon = r.passed ? 'PASS' : 'FAIL';
    console.log(
      `  ${icon.padEnd(6)}  ${String(r.expected).padStart(3)}  ${String(r.actual).padStart(3)}  ${r.label.slice(0, labelW)}`
    );
  });

  console.log('\n' + '─'.repeat(62));
  console.log(`  Total   : ${total}`);
  console.log(`  Passed  : ${passed}`);
  console.log(`  Failed  : ${failed}`);
  console.log('─'.repeat(62));

  if (failed > 0) {
    console.log('\n  ── FAILURE DETAILS ──────────────────────────────────────');
    results
      .filter(r => !r.passed)
      .forEach(r => {
        console.log(`\n  ✗  ${r.label}`);
        console.log(`     ${r.method} ${r.path}`);
        console.log(`     Expected : ${r.expected}`);
        console.log(`     Actual   : ${r.actual}`);
        if (r.body) console.log(`     Body     : ${JSON.stringify(r.body, null, 2).replace(/\n/g, '\n               ')}`);
      });
    console.log('');
  }

  console.log('═'.repeat(62));
  console.log(failed === 0 ? '  ✓  ALL TESTS PASSED' : `  ✗  ${failed} TEST(S) FAILED`);
  console.log('═'.repeat(62) + '\n');

  process.exit(failed > 0 ? 1 : 0);
}

main().catch(err => {
  console.error('\nUnhandled error:', err);
  process.exit(1);
});
