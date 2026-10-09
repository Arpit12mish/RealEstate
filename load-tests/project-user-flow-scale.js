import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || '').replace(/\/$/, '');
const PROJECT_IDS = (__ENV.PROJECT_IDS || __ENV.PROJECT_ID || '51')
  .split(',')
  .map((value) => value.trim())
  .filter(Boolean);
const SCENARIO = __ENV.SCENARIO || 'realistic';
const TARGET_USERS = Number(__ENV.TARGET_USERS || '5');
const HOLD_DURATION = __ENV.HOLD_DURATION || '2m';
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || '10s';
const SUMMARY_PATH = __ENV.SUMMARY_PATH || 'project-user-flow-summary.json';
const AUTH_TOKENS = (__ENV.AUTH_TOKENS || '')
  .split(',')
  .map((value) => value.trim())
  .filter(Boolean);
const MAX_FAILURE_LOGS_PER_VU = Number(__ENV.MAX_FAILURE_LOGS_PER_VU || '2');

if (!BASE_URL) {
  throw new Error('BASE_URL environment variable is required');
}

if (![5, 10, 15, 20, 25, 50].includes(TARGET_USERS)) {
  throw new Error('TARGET_USERS must be one of 5, 10, 15, 20, 25, or 50');
}

if (!['realistic', 'canonical-home', 'legacy-home-feed'].includes(SCENARIO)) {
  throw new Error('SCENARIO must be realistic, canonical-home, or legacy-home-feed');
}

if (PROJECT_IDS.length === 0) {
  throw new Error('At least one PROJECT_ID or PROJECT_IDS value is required');
}

// Anonymous VUs share one source-IP bucket. Each iteration consumes five
// PUBLIC_PROJECT_READ tokens, so an anonymous 10+ VU run primarily measures
// the limiter rather than backend capacity. Require an explicit choice.
if (
  TARGET_USERS > 5 &&
  AUTH_TOKENS.length < TARGET_USERS &&
  __ENV.ALLOW_SHARED_IDENTITY !== 'true'
) {
  throw new Error(
    '10+ VUs require one AUTH_TOKENS entry per VU, or ' +
      'ALLOW_SHARED_IDENTITY=true when intentionally testing shared-IP rate limiting'
  );
}

const routeDefinitions = {
  home: { label: 'Public home', p95: 1500, p99: 3000 },
  feed: { label: 'Home feed', p95: 1500, p99: 3000 },
  featured: { label: 'Featured projects', p95: 1500, p99: 3000 },
  detail: { label: 'Project detail', p95: 1500, p99: 3000 },
  media: { label: 'Project media', p95: 1500, p99: 3000 },
  meter: { label: 'Project meter', p95: 2000, p99: 4000 },
  floorPlans: { label: 'Project floor plans', p95: 1500, p99: 3000 },
};

const activeRouteKeys = SCENARIO === 'legacy-home-feed'
  ? ['feed']
  : SCENARIO === 'canonical-home'
    ? ['home']
    : ['home', 'featured', 'detail', 'media', 'meter', 'floorPlans'];

const routeMetrics = Object.fromEntries(
  Object.entries(routeDefinitions).map(([key]) => [
    key,
    {
      failures: new Rate(`${key}_failures`),
      duration: new Trend(`${key}_duration`, true),
      responseSize: new Trend(`${key}_response_size_bytes`),
    },
  ])
);

const iterationFailures = new Rate('iteration_failures');
const recoveryFailures = new Rate('recovery_failures');
let failureLogsForVu = 0;

function buildStages(target) {
  const levels = [5, 10, 15, 20, 25, 50].filter((level) => level <= target);
  return [
    ...levels.flatMap((level) => [
      { duration: '1m', target: level },
      { duration: HOLD_DURATION, target: level },
    ]),
    { duration: '1m', target: 0 },
  ];
}

const thresholds = {
  http_req_failed: [{ threshold: 'rate<0.01', abortOnFail: true, delayAbortEval: '30s' }],
  iteration_failures: [{ threshold: 'rate<0.01', abortOnFail: true, delayAbortEval: '30s' }],
  http_req_duration: ['p(90)<1000', 'p(95)<1500', 'p(99)<3000'],
  recovery_failures: ['rate==0'],
};

for (const key of activeRouteKeys) {
  const limits = routeDefinitions[key];
  thresholds[`${key}_failures`] = [
    { threshold: 'rate<0.01', abortOnFail: true, delayAbortEval: '30s' },
  ];
  thresholds[`${key}_duration`] = [
    `p(95)<${limits.p95}`,
    `p(99)<${limits.p99}`,
  ];
}

export const options = {
  scenarios: {
    [SCENARIO.replaceAll('-', '_')]: {
      executor: 'ramping-vus',
      exec: SCENARIO === 'legacy-home-feed'
        ? 'legacyHomeFeed'
        : SCENARIO === 'canonical-home'
          ? 'canonicalHome'
          : 'realisticMobileFlow',
      startVUs: 0,
      stages: buildStages(TARGET_USERS),
      gracefulRampDown: '30s',
    },
  },
  thresholds,
};

function requestParams(routeName) {
  const headers = {
    Accept: 'application/json',
    'User-Agent': 'SFS-k6-load-test/2.0',
  };
  if (AUTH_TOKENS.length > 0) {
    const tokenIndex = Math.max(0, (__VU || 1) - 1) % AUTH_TOKENS.length;
    headers.Authorization = `Bearer ${AUTH_TOKENS[tokenIndex]}`;
  }
  return {
    headers,
    timeout: REQUEST_TIMEOUT,
    tags: { route: routeName, name: routeName },
  };
}

function verifyResponse(response, routeKey) {
  const definition = routeDefinitions[routeKey];
  const metrics = routeMetrics[routeKey];
  metrics.duration.add(response.timings.duration);
  metrics.responseSize.add(response.body ? response.body.length : 0);

  const passed = check(response, {
    [`${definition.label}: status is 200`]: (res) => res.status === 200,
    [`${definition.label}: response is JSON`]: (res) =>
      (res.headers['Content-Type'] || '').includes('application/json'),
    [`${definition.label}: response body exists`]: (res) => Boolean(res.body && res.body.length > 0),
  });

  metrics.failures.add(!passed);
  if (!passed && failureLogsForVu < MAX_FAILURE_LOGS_PER_VU) {
    failureLogsForVu += 1;
    console.error(
      `${definition.label} failed: status=${response.status}, ` +
        `duration=${response.timings.duration}ms, body=${(response.body || '').slice(0, 300)}`
    );
  }
  return passed;
}

function get(routeKey, path, routeName) {
  const response = http.get(`${BASE_URL}${path}`, requestParams(routeName));
  return { response, passed: verifyResponse(response, routeKey) };
}

function thinkTime(minSeconds, maxSeconds) {
  sleep(Math.random() * (maxSeconds - minSeconds) + minSeconds);
}

function startIteration() {
  if (__ITER === 0) {
    sleep(Math.random() * 3);
  }
}

export function canonicalHome() {
  startIteration();
  const result = get('home', '/api/public/home', 'GET /api/public/home');
  iterationFailures.add(!result.passed);
  thinkTime(2, 5);
}

export function legacyHomeFeed() {
  startIteration();
  const result = get(
    'feed',
    '/api/public/feed?screen=HOME',
    'GET /api/public/feed?screen=HOME'
  );
  iterationFailures.add(!result.passed);
  thinkTime(2, 5);
}

export function realisticMobileFlow() {
  startIteration();

  // JS `%` can return a negative result (e.g. VU=1, ITER=0 -> -1 % n = -1),
  // which would index PROJECT_IDS with a negative offset and silently
  // resolve to undefined - hence the extra `+ n) % n`. This never surfaced
  // with a single PROJECT_IDS entry (any index mod 1 is 0) but breaks as
  // soon as more than one project ID is supplied for a realistic mix.
  const projectIndex = ((__VU + __ITER - 2) % PROJECT_IDS.length + PROJECT_IDS.length) % PROJECT_IDS.length;
  const projectId = PROJECT_IDS[projectIndex];
  let iterationPassed = true;

  group('Open public home', () => {
    iterationPassed = get('home', '/api/public/home', 'GET /api/public/home').passed && iterationPassed;
  });
  thinkTime(1, 3);

  group('Browse featured projects', () => {
    iterationPassed = get(
      'featured',
      '/api/projects/feature',
      'GET /api/projects/feature'
    ).passed && iterationPassed;
  });
  thinkTime(2, 5);

  group('Open project detail', () => {
    const result = get('detail', `/api/projects/${projectId}`, 'GET /api/projects/:id');
    iterationPassed = result.passed && iterationPassed;
    if (result.passed) {
      let body;
      try {
        body = result.response.json();
      } catch (_) {
        body = null;
      }
      const shapePassed = check(body, {
        'Project detail contains id': (data) => data && data.id !== undefined && data.id !== null,
        'Project detail contains propertyTypes': (data) => data && Array.isArray(data.propertyTypes),
      });
      routeMetrics.detail.failures.add(!shapePassed);
      iterationPassed = shapePassed && iterationPassed;
    }
  });
  thinkTime(1, 3);

  group('Load project media', () => {
    iterationPassed = get(
      'media',
      `/api/projects/${projectId}/media`,
      'GET /api/projects/:id/media'
    ).passed && iterationPassed;
  });
  thinkTime(1, 3);

  group('Load project meter', () => {
    iterationPassed = get(
      'meter',
      `/api/projects/${projectId}/meter`,
      'GET /api/projects/:id/meter'
    ).passed && iterationPassed;
  });
  thinkTime(1, 3);

  group('Load project floor plans', () => {
    iterationPassed = get(
      'floorPlans',
      `/api/projects/${projectId}/floor-plans`,
      'GET /api/projects/:id/floor-plans'
    ).passed && iterationPassed;
  });

  iterationFailures.add(!iterationPassed);
  thinkTime(2, 5);
}

export default realisticMobileFlow;

export function teardown() {
  const recoveryUrl = __ENV.RECOVERY_URL || `${BASE_URL}/actuator/health/readiness`;
  const response = http.get(recoveryUrl, requestParams('recovery-readiness'));
  const recovered = check(response, {
    'Server recovered after load': (res) => res.status === 200,
  });
  recoveryFailures.add(!recovered);
}

export function handleSummary(data) {
  const failedThresholds = Object.entries(data.metrics)
    .filter(([, metric]) => metric.thresholds && Object.values(metric.thresholds).some((value) => !value.ok))
    .map(([name]) => name);
  return {
    stdout:
      `Summary written to ${SUMMARY_PATH}. ` +
      `Failed thresholds: ${failedThresholds.length ? failedThresholds.join(', ') : 'none'}\n`,
    [SUMMARY_PATH]: JSON.stringify(data, null, 2),
  };
}
