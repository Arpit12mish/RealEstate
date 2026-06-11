'use strict';
/**
 * Shared test utilities for SFS dashboard API test scripts.
 * Requires Node 18+ (built-in fetch).
 */

function is2xx(s) { return s >= 200 && s < 300; }

/** Raw HTTP helper. token may be null for unauthenticated calls. */
async function request(baseUrl, token, method, path, body) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  let json = null;
  try { json = await res.json(); } catch (_) { /* binary / empty */ }
  return { status: res.status, body: json };
}

/**
 * Record an HTTP status assertion.
 * expected may be a single number or an array of acceptable codes.
 */
function assertStatus(results, label, method, path, expected, actual, body) {
  const codes  = Array.isArray(expected) ? expected : [expected];
  const passed = codes.includes(actual);
  results.push({ label, method, path, expected: codes.join('/'), actual, passed, body });
  if (passed) {
    console.log(`  ✓  [${actual}] ${method} ${path}`);
  } else {
    console.log(`  ✗  [exp ${codes.join('/')} got ${actual}] ${method} ${path}  ← FAILED`);
    if (body) console.log(`       ${JSON.stringify(body)}`);
  }
  return passed;
}

/** Record a value equality assertion. */
function assertEqual(results, label, expected, actual) {
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
  return passed;
}

/** Login and return accessToken, or throw on failure. */
async function login(baseUrl, email, password) {
  const res = await fetch(`${baseUrl}/api/dashboard/auth/login`, {
    method : 'POST',
    headers: { 'Content-Type': 'application/json' },
    body   : JSON.stringify({ email, password }),
  });
  let json = null;
  try { json = await res.json(); } catch (_) {}
  if (!is2xx(res.status) || !json?.accessToken) {
    throw new Error(`Login failed for ${email}: ${res.status} ${JSON.stringify(json)}`);
  }
  return json.accessToken;
}

function banner(title) {
  console.log(`\n${'─'.repeat(64)}`);
  console.log(`  ${title}`);
  console.log('─'.repeat(64));
}

function printSummary(results, scriptName) {
  const total  = results.length;
  const passed = results.filter(r => r.passed).length;
  const failed = results.filter(r => !r.passed).length;

  console.log('\n' + '═'.repeat(64));
  console.log(`  SUMMARY — ${scriptName}`);
  console.log('═'.repeat(64));

  const labelW = Math.min(Math.max(20, ...results.map(r => r.label.length)), 56);
  console.log(`\n  ${'RESULT'.padEnd(4)}  ${'EXP'.padStart(5)}  ${'ACT'.padStart(3)}  TEST`);
  console.log(`  ${'─'.repeat(4)}  ${'─'.repeat(5)}  ${'─'.repeat(3)}  ${'─'.repeat(labelW)}`);
  results.forEach(r => {
    const icon = r.passed ? 'PASS' : 'FAIL';
    console.log(
      `  ${icon.padEnd(4)}  ${String(r.expected).padStart(5)}  ${String(r.actual).padStart(3)}  ${r.label.slice(0, labelW)}`
    );
  });

  console.log('\n' + '─'.repeat(64));
  console.log(`  Total  : ${total}`);
  console.log(`  Passed : ${passed}`);
  console.log(`  Failed : ${failed}`);
  console.log('─'.repeat(64));

  if (failed > 0) {
    console.log('\n  ── FAILURES ───────────────────────────────────────────────');
    results.filter(r => !r.passed).forEach(r => {
      console.log(`\n  ✗  ${r.label}`);
      if (r.method !== '-') console.log(`     ${r.method} ${r.path}`);
      console.log(`     Expected : ${r.expected}`);
      console.log(`     Actual   : ${r.actual}`);
      if (r.body) {
        const bodyStr = JSON.stringify(r.body, null, 2).replace(/\n/g, '\n               ');
        console.log(`     Body     : ${bodyStr}`);
      }
    });
    console.log('');
  }

  console.log('═'.repeat(64));
  console.log(failed === 0 ? '  ✓  ALL TESTS PASSED' : `  ✗  ${failed} TEST(S) FAILED`);
  console.log('═'.repeat(64) + '\n');
  return failed;
}

module.exports = { is2xx, request, assertStatus, assertEqual, login, banner, printSummary };
