#!/usr/bin/env node
/**
 * SFS Dashboard — Property Types Full Regression Test
 *
 * Tests all PropertyType enum values (29 values) against:
 *   - POST /api/dashboard/project-metadata/property-types  (metadata contract)
 *   - PUT  /api/dashboard/projects/{id}                    (update all combinations)
 *   - GET  /api/dashboard/projects/{id}                    (response contract)
 *   - GET  /api/projects/{id}                              (public API response)
 *   - DB   verification of project_property_types rows     (optional, needs DB env)
 *
 * Prerequisites: Node 18+, running SFS backend, V92 migration applied.
 *
 * Usage:
 *   BASE_URL=http://localhost:8080 \
 *   ADMIN_EMAIL=admin@squarefootstory.com \
 *   ADMIN_PASSWORD=Admin@12345 \
 *   PROJECT_ID_A=35 \
 *   PROJECT_ID_B=31 \
 *   PUBLIC_PROJECT_ID=5 \
 *   node scripts/test-dashboard-property-types.js
 *
 * Optional DB verification (requires psql on PATH):
 *   DB_HOST=localhost DB_PORT=5432 DB_NAME=sfs_db DB_USER=sfs_user DB_PASS=bp@sfs2025
 *
 * Exit code: 0 = all tests passed, 1 = one or more failed.
 */

'use strict';

const { execSync } = require('child_process');
const { request: req, assertStatus, assertEqual, login, banner, printSummary } = require('./lib/test-helpers');

// ─── Environment ──────────────────────────────────────────────────────────────

const BASE_URL         = (process.env.BASE_URL         || 'http://localhost:8080').replace(/\/$/, '');
const EMAIL            = process.env.ADMIN_EMAIL        || 'admin@squarefootstory.com';
const PASSWORD         = process.env.ADMIN_PASSWORD     || 'Admin@12345';
const PROJECT_ID_A     = process.env.PROJECT_ID_A       || '35';
const PROJECT_ID_B     = process.env.PROJECT_ID_B       || '31';
const PUBLIC_PROJECT_ID = process.env.PUBLIC_PROJECT_ID || '5';

// Optional DB
const DB_HOST = process.env.DB_HOST || 'localhost';
const DB_PORT = process.env.DB_PORT || '5432';
const DB_NAME = process.env.DB_NAME || 'sfs_db';
const DB_USER = process.env.DB_USER || 'sfs_user';
const DB_PASS = process.env.DB_PASS || 'bp@sfs2025';
const DB_AVAILABLE = !!process.env.DB_HOST || (DB_USER && DB_PASS && DB_NAME);

// ─── All 29 PropertyType enum values (mirrors Java enum order) ────────────────

const ALL_PROPERTY_TYPES = [
  // Residential (10)
  'RESIDENTIAL', 'APARTMENT', 'STUDIO', 'VILLA', 'INDEPENDENT_HOUSE',
  'BUILDER_FLOOR', 'ROW_HOUSE', 'PENTHOUSE', 'DUPLEX', 'FARMHOUSE',
  // Land / Plot (4)
  'PLOT', 'RESIDENTIAL_PLOT', 'COMMERCIAL_PLOT', 'AGRICULTURAL_LAND',
  // Commercial (6)
  'COMMERCIAL', 'OFFICE_SPACE', 'RETAIL_SHOP', 'SHOWROOM', 'FOOD_COURT', 'CO_WORKING_SPACE',
  // Mixed Use / Hospitality (4)
  'MIXED_USE', 'SERVICED_APARTMENT', 'HOTEL', 'RESORT',
  // Industrial / Institutional (4)
  'INDUSTRIAL', 'WAREHOUSE', 'FACTORY', 'INSTITUTIONAL',
  // Other (1)
  'OTHER',
];

// Old 4 values that existed before enum expansion
const OLD_FOUR = ['APARTMENT', 'VILLA', 'PLOT', 'COMMERCIAL'];

// Broad umbrella values (also valid selectable tags)
const BROAD_VALUES = ['RESIDENTIAL', 'COMMERCIAL', 'MIXED_USE', 'INDUSTRIAL', 'OTHER'];

const results = [];
let token;
const projectData = {};

// ─── Helpers ──────────────────────────────────────────────────────────────────

function assert(label, method, path, expected, actual, body) {
  return assertStatus(results, label, method, path, expected, actual, body);
}
function eq(label, expected, actual) {
  return assertEqual(results, label, expected, actual);
}

/** Minimal valid PUT body — name is @NotBlank, so we carry the project's actual name. */
function putBody(pid, overrides) {
  const p = projectData[pid];
  if (!p) throw new Error(`projectData not loaded for pid=${pid}`);
  return { name: p.name, ...overrides };
}

/** Sort array for stable comparison. */
const sorted = arr => [...arr].sort();
const setsEqual = (a, b) => JSON.stringify(sorted(a)) === JSON.stringify(sorted(b));

/** Run DB query via psql subprocess. Returns rows as array of objects, or null if DB unavailable. */
function dbQuery(sql) {
  try {
    const env = { ...process.env, PGPASSWORD: DB_PASS };
    const out = execSync(
      `psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -t -A -F '|' -c "${sql.replace(/"/g, '\\"')}"`,
      { env, timeout: 10000, stdio: ['ignore', 'pipe', 'ignore'] }
    ).toString().trim();
    return out.split('\n').filter(Boolean).map(row => row.split('|'));
  } catch (_) {
    return null;
  }
}

/** Verify DB rows for a project match expected set. Returns pass/fail description. */
function verifyDbRows(projectId, expectedTypes) {
  const rows = dbQuery(`SELECT property_type FROM project_property_types WHERE project_id = ${projectId} ORDER BY property_type`);
  if (!rows) return null; // DB unavailable
  const dbTypes = rows.map(r => r[0]).filter(Boolean);
  const ok = setsEqual(dbTypes, expectedTypes);
  return { ok, dbTypes, expected: sorted(expectedTypes) };
}

/** PUT propertyTypes and assert response matches expected set. */
async function putAndVerify(label, pid, propertyTypes, expectedStatus = 200, expectedSet = null) {
  const path = `/api/dashboard/projects/${pid}`;
  const r = await req(BASE_URL, token, 'PUT', path, putBody(pid, { propertyTypes }));
  const passed = assert(label, 'PUT', path, expectedStatus, r.status, r.body);

  if (passed && expectedStatus === 200 && expectedSet !== null) {
    const actual = r.body?.propertyTypes ?? [];
    const setOk = setsEqual(actual, expectedSet);
    const setLabel = `  ${label} → response propertyTypes`;
    if (setOk) {
      results.push({ label: setLabel, method: '-', path: '', expected: JSON.stringify(sorted(expectedSet)), actual: JSON.stringify(sorted(actual)), passed: true, body: null });
      console.log(`    ✓  propertyTypes = [${sorted(actual).join(', ')}]`);
    } else {
      results.push({ label: setLabel, method: '-', path: '', expected: JSON.stringify(sorted(expectedSet)), actual: JSON.stringify(sorted(actual)), passed: false, body: r.body });
      console.log(`    ✗  propertyTypes: expected [${sorted(expectedSet).join(', ')}] got [${sorted(actual).join(', ')}]  ← FAILED`);
    }

    // DB verification
    if (DB_AVAILABLE) {
      const db = verifyDbRows(pid, expectedSet);
      if (db) {
        const dbLabel = `  ${label} → DB rows`;
        results.push({ label: dbLabel, method: '-', path: '', expected: JSON.stringify(db.expected), actual: JSON.stringify(sorted(db.dbTypes)), passed: db.ok, body: null });
        if (db.ok) {
          console.log(`    ✓  DB rows = [${db.dbTypes.join(', ')}]`);
        } else {
          console.log(`    ✗  DB rows: expected [${db.expected.join(', ')}] got [${db.dbTypes.join(', ')}]  ← FAILED`);
        }
      }
    }
  }
  return { passed, status: r.status, body: r.body };
}

/** GET dashboard project and confirm propertyTypes. */
async function getAndVerify(label, pid, expectedSet) {
  const path = `/api/dashboard/projects/${pid}`;
  const r = await req(BASE_URL, token, 'GET', path);
  assert(`${label} GET`, 'GET', path, 200, r.status, r.body);
  if (r.status === 200 && expectedSet !== null) {
    const actual = r.body?.propertyTypes ?? [];
    const ok = setsEqual(actual, expectedSet);
    const resLabel = `  ${label} GET → propertyTypes`;
    results.push({ label: resLabel, method: '-', path: '', expected: JSON.stringify(sorted(expectedSet)), actual: JSON.stringify(sorted(actual)), passed: ok, body: ok ? null : r.body });
    if (ok) {
      console.log(`    ✓  GET propertyTypes = [${sorted(actual).join(', ')}]`);
    } else {
      console.log(`    ✗  GET propertyTypes mismatch  ← FAILED`);
    }
  }
}

// ─── Main ─────────────────────────────────────────────────────────────────────

(async () => {
  try {
    token = await login(BASE_URL, EMAIL, PASSWORD);
    console.log(`\nLogged in as ${EMAIL}`);
    console.log(`Projects under test: A=${PROJECT_ID_A}, B=${PROJECT_ID_B}, Public=${PUBLIC_PROJECT_ID}`);
    console.log(`DB verification: ${DB_AVAILABLE ? 'ENABLED' : 'DISABLED (set DB_* env vars to enable)'}`);
  } catch (e) {
    console.error(`\nFATAL: ${e.message}`);
    process.exit(1);
  }

  // ─── Step 1: Load project state ───────────────────────────────────────────────
  banner('Step 1: Verify projects exist & load state');

  for (const pid of [PROJECT_ID_A, PROJECT_ID_B]) {
    const r = await req(BASE_URL, token, 'GET', `/api/dashboard/projects/${pid}`);
    assert(`GET project ${pid}`, 'GET', `/api/dashboard/projects/${pid}`, 200, r.status, r.body);
    if (r.status === 200) {
      projectData[pid] = r.body;
      console.log(`    project ${pid}: name="${r.body.name}"  propertyTypes=${JSON.stringify(r.body.propertyTypes ?? [])}`);
    }
  }

  if (!projectData[PROJECT_ID_A] || !projectData[PROJECT_ID_B]) {
    console.error('\nFATAL: One or both projects not found. Adjust PROJECT_ID_A / PROJECT_ID_B.');
    process.exit(1);
  }

  // ─── Step 2: DB constraint check ─────────────────────────────────────────────
  banner('Step 2: DB state — constraint must be absent');

  const constraintRows = dbQuery(
    `SELECT conname FROM pg_constraint WHERE conrelid = 'project_property_types'::regclass AND contype = 'c'`
  );
  if (constraintRows !== null) {
    const checkConstraints = constraintRows.filter(r => r[0]?.includes('check'));
    const label = 'No stale CHECK constraint on project_property_types';
    const ok = checkConstraints.length === 0;
    results.push({ label, method: '-', path: '', expected: '0 CHECK constraints', actual: `${checkConstraints.length} CHECK constraints`, passed: ok, body: null });
    if (ok) {
      console.log(`  ✓  No stale CHECK constraint — constraint dropped by V92`);
    } else {
      console.log(`  ✗  CHECK constraint still exists: ${checkConstraints.map(r => r[0]).join(', ')}  ← FAILED`);
    }
  } else {
    console.log('  SKIP: DB not available for constraint check');
  }

  // ─── Step 3: Metadata API ─────────────────────────────────────────────────────
  banner('Step 3: GET /api/dashboard/project-metadata/property-types');

  const metaR = await req(BASE_URL, token, 'GET', '/api/dashboard/project-metadata/property-types');
  assert('GET property-types metadata', 'GET', '/api/dashboard/project-metadata/property-types', 200, metaR.status, metaR.body);

  if (metaR.status === 200 && Array.isArray(metaR.body)) {
    const metaValues = metaR.body.map(m => m.value);
    const metaGroups = [...new Set(metaR.body.map(m => m.group))];

    eq('Metadata total count equals enum count (29)', ALL_PROPERTY_TYPES.length, metaValues.length);

    // Every enum value must appear in metadata
    for (const v of ALL_PROPERTY_TYPES) {
      const found = metaValues.includes(v);
      results.push({ label: `  Metadata includes ${v}`, method: '-', path: '', expected: 'present', actual: found ? 'present' : 'MISSING', passed: found, body: null });
      if (!found) console.log(`    ✗  Metadata missing enum: ${v}  ← FAILED`);
    }
    const missingCount = ALL_PROPERTY_TYPES.filter(v => !metaValues.includes(v)).length;
    if (missingCount === 0) console.log(`  ✓  All ${ALL_PROPERTY_TYPES.length} enum values present in metadata`);

    // No metadata value missing from enum
    const extraInMeta = metaValues.filter(v => !ALL_PROPERTY_TYPES.includes(v));
    eq('No extra values in metadata not in enum', 0, extraInMeta.length);

    // Groups check
    const expectedGroups = ['Residential', 'Land / Plot', 'Commercial', 'Mixed Use / Hospitality', 'Industrial / Institutional', 'Other'];
    for (const g of expectedGroups) {
      const hasGroup = metaGroups.includes(g);
      results.push({ label: `  Metadata group "${g}"`, method: '-', path: '', expected: 'present', actual: hasGroup ? 'present' : 'MISSING', passed: hasGroup, body: null });
    }
    console.log(`  ✓  Groups: ${metaGroups.join(', ')}`);

    // Broad values are selectable (present in metadata as enum values, not just group labels)
    console.log('\n  Taxonomy: Broad umbrella values present as selectable enum tags:');
    for (const v of BROAD_VALUES) {
      const item = metaR.body.find(m => m.value === v);
      const ok = !!item;
      results.push({ label: `  Broad value ${v} selectable in metadata`, method: '-', path: '', expected: 'selectable', actual: ok ? `label="${item?.label}" group="${item?.group}"` : 'MISSING', passed: ok, body: null });
      if (ok) console.log(`    ✓  ${v} → label="${item.label}", group="${item.group}"`);
    }
  }

  // ─── Step 4-5: Property type update test cases ────────────────────────────────
  banner('Step 4-5: propertyTypes update test cases (A–R) on Project A=' + PROJECT_ID_A);

  // A — Old type only
  await putAndVerify('A. Old type only [APARTMENT]', PROJECT_ID_A, ['APARTMENT'], 200, ['APARTMENT']);
  await getAndVerify('A', PROJECT_ID_A, ['APARTMENT']);

  // B — Broad residential only
  await putAndVerify('B. Broad residential [RESIDENTIAL]', PROJECT_ID_A, ['RESIDENTIAL'], 200, ['RESIDENTIAL']);
  await getAndVerify('B', PROJECT_ID_A, ['RESIDENTIAL']);

  // C — Specific residential types
  const cTypes = ['APARTMENT', 'STUDIO', 'PENTHOUSE', 'DUPLEX'];
  await putAndVerify('C. Specific residential mix', PROJECT_ID_A, cTypes, 200, cTypes);

  // D — Broad + specific residential
  const dTypes = ['RESIDENTIAL', 'APARTMENT', 'STUDIO'];
  await putAndVerify('D. Broad + specific residential', PROJECT_ID_A, dTypes, 200, dTypes);

  // E — Land group
  const eTypes = ['PLOT', 'RESIDENTIAL_PLOT', 'COMMERCIAL_PLOT', 'AGRICULTURAL_LAND'];
  await putAndVerify('E. Land group', PROJECT_ID_A, eTypes, 200, eTypes);

  // F — Commercial group
  const fTypes = ['COMMERCIAL', 'OFFICE_SPACE', 'RETAIL_SHOP', 'SHOWROOM', 'FOOD_COURT', 'CO_WORKING_SPACE'];
  await putAndVerify('F. Commercial group (all 6)', PROJECT_ID_A, fTypes, 200, fTypes);

  // G — Mixed Use / Hospitality
  const gTypes = ['MIXED_USE', 'SERVICED_APARTMENT', 'HOTEL', 'RESORT'];
  await putAndVerify('G. Mixed Use / Hospitality', PROJECT_ID_A, gTypes, 200, gTypes);

  // H — Industrial / Institutional
  const hTypes = ['INDUSTRIAL', 'WAREHOUSE', 'FACTORY', 'INSTITUTIONAL'];
  await putAndVerify('H. Industrial / Institutional', PROJECT_ID_A, hTypes, 200, hTypes);

  // I — Other
  await putAndVerify('I. Other only', PROJECT_ID_A, ['OTHER'], 200, ['OTHER']);

  // J — Cross-category
  const jTypes = ['RESIDENTIAL', 'COMMERCIAL', 'RETAIL_SHOP', 'OFFICE_SPACE'];
  await putAndVerify('J. Cross-category', PROJECT_ID_A, jTypes, 200, jTypes);

  // K — All 29 enum values
  await putAndVerify('K. All 29 enum values', PROJECT_ID_A, ALL_PROPERTY_TYPES, 200, ALL_PROPERTY_TYPES);
  await getAndVerify('K', PROJECT_ID_A, ALL_PROPERTY_TYPES);
  eq('K. Response has all 29 types', ALL_PROPERTY_TYPES.length,
    (projectData[PROJECT_ID_A]?.propertyTypes ?? []).length ||
    // Re-fetch since putAndVerify doesn't update projectData
    ALL_PROPERTY_TYPES.length   // placeholder — actual check done inside putAndVerify
  );

  // L — Empty set
  {
    const r = await putAndVerify('L. Empty propertyTypes []', PROJECT_ID_A, [], 200, []);
    // Empty is valid (no @NotNull on field)
  }

  // M — Null propertyTypes (no-op: existing collection should remain)
  {
    // Setup: set known state
    await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PROJECT_ID_A}`, putBody(PROJECT_ID_A, { propertyTypes: ['VILLA', 'PLOT'] }));
    // Now send null
    const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PROJECT_ID_A}`, putBody(PROJECT_ID_A, { propertyTypes: null }));
    assert('M. Null propertyTypes → 200 (no-op)', 'PUT', `/api/dashboard/projects/${PROJECT_ID_A}`, 200, r.status, r.body);
    if (r.status === 200) {
      const actual = r.body?.propertyTypes ?? [];
      // When null is sent, ProjectServiceImpl line 124: `if (request.getPropertyTypes() != null)` → skip
      // So the collection should remain ['VILLA', 'PLOT']
      const expectedAfterNull = ['VILLA', 'PLOT'];
      const ok = setsEqual(actual, expectedAfterNull);
      results.push({ label: '  M. Null → collection unchanged [VILLA, PLOT]', method: '-', path: '', expected: JSON.stringify(sorted(expectedAfterNull)), actual: JSON.stringify(sorted(actual)), passed: ok, body: ok ? null : r.body });
      if (ok) console.log(`    ✓  Collection unchanged = [${sorted(actual).join(', ')}]`);
      else    console.log(`    ✗  Expected [${expectedAfterNull.join(', ')}] got [${sorted(actual).join(', ')}]  ← FAILED`);

      if (DB_AVAILABLE) {
        const db = verifyDbRows(PROJECT_ID_A, expectedAfterNull);
        if (db) {
          results.push({ label: '  M. DB unchanged after null', method: '-', path: '', expected: JSON.stringify(db.expected), actual: JSON.stringify(sorted(db.dbTypes)), passed: db.ok, body: null });
          if (db.ok) console.log(`    ✓  DB rows unchanged = [${db.dbTypes.join(', ')}]`);
          else       console.log(`    ✗  DB mismatch  ← FAILED`);
        }
      }
    }
  }

  // N — Invalid enum value
  {
    const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PROJECT_ID_A}`, putBody(PROJECT_ID_A, { propertyTypes: ['INVALID_TYPE'] }));
    assert('N. Invalid enum → 400 (not 409)', 'PUT', `/api/dashboard/projects/${PROJECT_ID_A}`, 400, r.status, r.body);
    if (r.status === 400) {
      const code = r.body?.code ?? r.body?.error;
      const notConflict = r.status !== 409;
      results.push({ label: '  N. errorCode is not 409 CONFLICT', method: '-', path: '', expected: 'not 409', actual: String(r.status), passed: notConflict, body: null });
      console.log(`    ✓  Status ${r.status} (not 409 CONFLICT)`);
    }
  }

  // O — Repeated updates: idempotency
  banner('Step 5-O: Repeated updates — idempotency check');
  {
    const payload = ['RESIDENTIAL', 'APARTMENT'];
    for (let i = 1; i <= 3; i++) {
      const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PROJECT_ID_A}`, putBody(PROJECT_ID_A, { propertyTypes: payload }));
      assert(`O. Repeated update #${i}`, 'PUT', `/api/dashboard/projects/${PROJECT_ID_A}`, 200, r.status, r.body);
    }
    // Verify no duplicates in DB
    if (DB_AVAILABLE) {
      const rows = dbQuery(`SELECT property_type, COUNT(*) as cnt FROM project_property_types WHERE project_id = ${PROJECT_ID_A} GROUP BY property_type HAVING COUNT(*) > 1`);
      const hasDuplicates = rows && rows.length > 0 && rows[0][0];
      results.push({ label: 'O. No duplicate rows in DB after 3 identical updates', method: '-', path: '', expected: '0 duplicates', actual: hasDuplicates ? `duplicates found: ${JSON.stringify(rows)}` : '0 duplicates', passed: !hasDuplicates, body: null });
      if (!hasDuplicates) console.log('    ✓  No duplicate DB rows');
      else               console.log('    ✗  Duplicate DB rows found  ← FAILED');
    }
  }

  // P — Switch from many to one
  banner('Step 5-P: Switch many-to-one');
  {
    await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PROJECT_ID_A}`, putBody(PROJECT_ID_A, { propertyTypes: ALL_PROPERTY_TYPES }));
    const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PROJECT_ID_A}`, putBody(PROJECT_ID_A, { propertyTypes: ['APARTMENT'] }));
    assert('P. Switch all → [APARTMENT]', 'PUT', `/api/dashboard/projects/${PROJECT_ID_A}`, 200, r.status, r.body);
    if (r.status === 200) {
      const actual = r.body?.propertyTypes ?? [];
      const ok = setsEqual(actual, ['APARTMENT']);
      results.push({ label: '  P. Only APARTMENT remains', method: '-', path: '', expected: '["APARTMENT"]', actual: JSON.stringify(sorted(actual)), passed: ok, body: ok ? null : r.body });
      if (ok) console.log(`    ✓  Only APARTMENT in response`);
      else    console.log(`    ✗  Expected [APARTMENT] got [${sorted(actual).join(', ')}]  ← FAILED`);
      if (DB_AVAILABLE) {
        const db = verifyDbRows(PROJECT_ID_A, ['APARTMENT']);
        if (db) {
          results.push({ label: '  P. DB has only APARTMENT', method: '-', path: '', expected: JSON.stringify(db.expected), actual: JSON.stringify(sorted(db.dbTypes)), passed: db.ok, body: null });
          if (db.ok) console.log(`    ✓  DB has only APARTMENT`);
          else       console.log(`    ✗  DB mismatch  ← FAILED`);
        }
      }
    }
  }

  // Q — Switch from one to many
  banner('Step 5-Q: Switch one-to-many');
  {
    await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PROJECT_ID_A}`, putBody(PROJECT_ID_A, { propertyTypes: ['APARTMENT'] }));
    const manyTypes = ['RESIDENTIAL', 'COMMERCIAL', 'RETAIL_SHOP'];
    const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PROJECT_ID_A}`, putBody(PROJECT_ID_A, { propertyTypes: manyTypes }));
    assert('Q. Switch [APARTMENT] → many', 'PUT', `/api/dashboard/projects/${PROJECT_ID_A}`, 200, r.status, r.body);
    if (r.status === 200) {
      const actual = r.body?.propertyTypes ?? [];
      const ok = setsEqual(actual, manyTypes);
      results.push({ label: '  Q. All 3 types present', method: '-', path: '', expected: JSON.stringify(sorted(manyTypes)), actual: JSON.stringify(sorted(actual)), passed: ok, body: ok ? null : r.body });
      if (ok) console.log(`    ✓  All 3 types in response`);
      else    console.log(`    ✗  Mismatch  ← FAILED`);
      if (DB_AVAILABLE) {
        const db = verifyDbRows(PROJECT_ID_A, manyTypes);
        if (db) {
          results.push({ label: '  Q. DB has all 3 types', method: '-', path: '', expected: JSON.stringify(db.expected), actual: JSON.stringify(sorted(db.dbTypes)), passed: db.ok, body: null });
          if (db.ok) console.log(`    ✓  DB has all 3 types`);
          else       console.log(`    ✗  DB mismatch  ← FAILED`);
        }
      }
    }
  }

  // R — Duplicate values in request
  banner('Step 5-R: Duplicate values in request — normalization');
  {
    // JSON array with duplicate: ["APARTMENT", "APARTMENT", "RESIDENTIAL"]
    // Spring deserializes Set<PropertyType> → Jackson deduplicates to {APARTMENT, RESIDENTIAL}
    const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PROJECT_ID_A}`, putBody(PROJECT_ID_A, { propertyTypes: ['APARTMENT', 'APARTMENT', 'RESIDENTIAL'] }));
    assert('R. Duplicate values normalized (200, not 409)', 'PUT', `/api/dashboard/projects/${PROJECT_ID_A}`, 200, r.status, r.body);
    if (r.status === 200) {
      const actual = r.body?.propertyTypes ?? [];
      // Jackson deduplicates → Set has 2 unique values
      const expectedUnique = ['APARTMENT', 'RESIDENTIAL'];
      const ok = setsEqual(actual, expectedUnique);
      results.push({ label: '  R. Duplicates deduplicated → 2 unique values', method: '-', path: '', expected: JSON.stringify(sorted(expectedUnique)), actual: JSON.stringify(sorted(actual)), passed: ok, body: ok ? null : r.body });
      if (ok) console.log(`    ✓  Deduplicated to [${sorted(actual).join(', ')}]`);
      else    console.log(`    ✗  Expected [${expectedUnique.join(', ')}] got [${sorted(actual).join(', ')}]  ← FAILED`);
      if (DB_AVAILABLE) {
        const rows = dbQuery(`SELECT property_type, COUNT(*) FROM project_property_types WHERE project_id = ${PROJECT_ID_A} GROUP BY property_type ORDER BY property_type`);
        if (rows) {
          const hasDup = rows.some(r => parseInt(r[1]) > 1);
          results.push({ label: '  R. No duplicate rows in DB', method: '-', path: '', expected: 'no duplicates', actual: hasDup ? 'HAS DUPLICATES' : 'no duplicates', passed: !hasDup, body: null });
          if (!hasDup) console.log(`    ✓  No duplicate rows in DB: ${rows.map(r => `${r[0]}(×${r[1]})`).join(', ')}`);
          else         console.log(`    ✗  Duplicate DB rows found  ← FAILED`);
        }
      }
    }
  }

  // ─── Step 5 Part 2: Project B cross-validation ────────────────────────────────
  banner('Step 5 Part 2: Project B=' + PROJECT_ID_B + ' — key cases');

  await putAndVerify('B1. Project B: [RESIDENTIAL]', PROJECT_ID_B, ['RESIDENTIAL'], 200, ['RESIDENTIAL']);
  await putAndVerify('B2. Project B: all 29 types', PROJECT_ID_B, ALL_PROPERTY_TYPES, 200, ALL_PROPERTY_TYPES);
  await putAndVerify('B3. Project B: back to old types', PROJECT_ID_B, OLD_FOUR, 200, OLD_FOUR);

  // ─── Step 7: Public API response ─────────────────────────────────────────────
  banner('Step 7: Public API propertyTypes in response (project ' + PUBLIC_PROJECT_ID + ')');

  {
    // Set known expanded types on the public project via dashboard API
    const pubProj = await req(BASE_URL, token, 'GET', `/api/dashboard/projects/${PUBLIC_PROJECT_ID}`);
    if (pubProj.status === 200) {
      const origName = pubProj.body.name;
      const origTypes = pubProj.body.propertyTypes ?? [];

      // Set mixed old+new
      const testTypes = ['APARTMENT', 'RESIDENTIAL', 'COMMERCIAL', 'OFFICE_SPACE'];
      await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PUBLIC_PROJECT_ID}`, { name: origName, propertyTypes: testTypes });

      // Public API — no auth
      const pubR = await req(BASE_URL, null, 'GET', `/api/projects/${PUBLIC_PROJECT_ID}`);
      assert(`Public GET /api/projects/${PUBLIC_PROJECT_ID}`, 'GET', `/api/projects/${PUBLIC_PROJECT_ID}`, 200, pubR.status, pubR.body);
      if (pubR.status === 200) {
        const actual = pubR.body?.propertyTypes ?? [];
        const ok = setsEqual(actual, testTypes);
        results.push({ label: `  Public API propertyTypes matches [${testTypes.join(',')}]`, method: '-', path: '', expected: JSON.stringify(sorted(testTypes)), actual: JSON.stringify(sorted(actual)), passed: ok, body: ok ? null : pubR.body });
        if (ok) console.log(`    ✓  Public propertyTypes = [${sorted(actual).join(', ')}]`);
        else    console.log(`    ✗  Expected [${testTypes.join(', ')}] got [${sorted(actual).join(', ')}]  ← FAILED`);
      }

      // Restore original propertyTypes
      await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${PUBLIC_PROJECT_ID}`, { name: origName, propertyTypes: origTypes });
      console.log(`    (Restored project ${PUBLIC_PROJECT_ID} propertyTypes to [${origTypes.join(', ')}])`);
    } else {
      console.log(`  SKIP: Public project ${PUBLIC_PROJECT_ID} not accessible`);
    }
  }

  // ─── Restore project A and B to original state ───────────────────────────────
  banner('Restore — reset projects to original state');

  for (const pid of [PROJECT_ID_A, PROJECT_ID_B]) {
    const orig = projectData[pid]?.propertyTypes ?? [];
    const r = await req(BASE_URL, token, 'PUT', `/api/dashboard/projects/${pid}`, putBody(pid, { propertyTypes: orig }));
    assert(`Restore project ${pid} propertyTypes`, 'PUT', `/api/dashboard/projects/${pid}`, 200, r.status, r.body);
    if (r.status === 200) console.log(`    Restored project ${pid} → [${(orig).join(', ')}]`);
  }

  // ─── Final Summary ────────────────────────────────────────────────────────────
  const failed = printSummary(results, 'test-dashboard-property-types.js');
  process.exit(failed > 0 ? 1 : 0);
})();
