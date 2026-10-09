import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || '').replace(/\/$/, '');
const PROJECT_ID = (__ENV.PROJECT_ID || '51').trim();
const BURST_SIZE = Number(__ENV.BURST_SIZE || '50');
const BURST_ROUTE = __ENV.BURST_ROUTE || 'detail';
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || '10s';
const SUMMARY_PATH =
  __ENV.SUMMARY_PATH || `burst-${BURST_ROUTE}-${BURST_SIZE}-summary.json`;

const AUTH_TOKENS = (__ENV.AUTH_TOKENS || '')
  .split(',')
  .map((value) => value.trim())
  .filter(Boolean);

if (!BASE_URL) {
  throw new Error('BASE_URL environment variable is required');
}

if (!PROJECT_ID) {
  throw new Error('PROJECT_ID environment variable is required');
}

if (![10, 25, 50, 100].includes(BURST_SIZE)) {
  throw new Error('BURST_SIZE must be one of 10, 25, 50, or 100');
}

if (!['detail', 'meter', 'home'].includes(BURST_ROUTE)) {
  throw new Error('BURST_ROUTE must be detail, meter, or home');
}

/*
 * IMPORTANT:
 *
 * shared-iterations with exactly BURST_SIZE VUs + BURST_SIZE iterations
 * means every VU performs exactly one request.
 *
 * maxDuration is only a safety bound.
 *
 * This is intentionally different from the normal ramping-VU test:
 * no think time
 * no multiple endpoint flow
 * no repeated requests per VU
 */
export const options = {
  scenarios: {
    burst: {
      executor: 'shared-iterations',
      vus: BURST_SIZE,
      iterations: BURST_SIZE,
      maxDuration: '30s',
    },
  },

  thresholds: {
    http_req_failed: ['rate==0'],
    burst_failures: ['count==0'],
    http_req_duration: [
      'p(95)<2000',
      'p(99)<5000',
    ],
  },
};

const burstFailures = new Counter('burst_failures');
const burstDuration = new Trend('burst_duration', true);
const status2xx = new Counter('burst_status_2xx');
const status4xx = new Counter('burst_status_4xx');
const status5xx = new Counter('burst_status_5xx');

function routeConfig() {
  switch (BURST_ROUTE) {
    case 'detail':
      return {
        name: 'GET /api/projects/:id',
        path: `/api/projects/${PROJECT_ID}`,
      };

    case 'meter':
      return {
        name: 'GET /api/projects/:id/meter',
        path: `/api/projects/${PROJECT_ID}/meter`,
      };

    case 'home':
      return {
        name: 'GET /api/public/home',
        path: '/api/public/home',
      };

    default:
      throw new Error(`Unsupported BURST_ROUTE=${BURST_ROUTE}`);
  }
}

function requestParams(routeName) {
  const headers = {
    Accept: 'application/json',
    'User-Agent': 'SFS-k6-burst-test/1.0',
  };

  if (AUTH_TOKENS.length > 0) {
    const tokenIndex = Math.max(0, (__VU || 1) - 1) % AUTH_TOKENS.length;
    headers.Authorization = `Bearer ${AUTH_TOKENS[tokenIndex]}`;
  }

  return {
    headers,
    timeout: REQUEST_TIMEOUT,
    tags: {
      route: BURST_ROUTE,
      name: routeName,
      test_type: 'burst',
    },
  };
}

export default function () {
  const route = routeConfig();

  /*
   * No sleep here.
   *
   * k6 starts all configured VUs for this shared-iterations scenario,
   * giving us a concentrated burst rather than a realistic user journey.
   */
  const response = http.get(
    `${BASE_URL}${route.path}`,
    requestParams(route.name)
  );

  burstDuration.add(response.timings.duration);

  if (response.status >= 200 && response.status < 300) {
    status2xx.add(1);
  } else if (response.status >= 400 && response.status < 500) {
    status4xx.add(1);
  } else if (response.status >= 500) {
    status5xx.add(1);
  }

  const passed = check(response, {
    [`${route.name}: status is 200`]: (res) => res.status === 200,

    [`${route.name}: JSON response`]: (res) =>
      (res.headers['Content-Type'] || '')
        .toLowerCase()
        .includes('application/json'),

    [`${route.name}: response body exists`]: (res) =>
      Boolean(res.body && res.body.length > 0),
  });

  if (!passed) {
    burstFailures.add(1);

    console.error(
      `${route.name} failed: ` +
      `status=${response.status}, ` +
      `duration=${response.timings.duration}ms, ` +
      `body=${(response.body || '').slice(0, 300)}`
    );
  }
}

export function setup() {
  /*
   * Warm one request before the actual burst.
   *
   * This prevents class loading/JIT/first DB connection establishment from
   * distorting the burst measurement.
   *
   * This request is not part of the 50-request scenario.
   */
  const route = routeConfig();

  const response = http.get(
    `${BASE_URL}${route.path}`,
    requestParams(`warmup ${route.name}`)
  );

  if (response.status !== 200) {
    throw new Error(
      `Warmup failed for ${route.name}: ` +
      `status=${response.status}, ` +
      `body=${(response.body || '').slice(0, 300)}`
    );
  }

  return {
    warmedRoute: route.name,
  };
}

export function teardown() {
  const readinessUrl =
    __ENV.RECOVERY_URL ||
    `${BASE_URL}/actuator/health/readiness`;

  const response = http.get(readinessUrl, {
    headers: {
      Accept: 'application/json',
      'User-Agent': 'SFS-k6-burst-test/1.0',
    },
    timeout: REQUEST_TIMEOUT,
    tags: {
      name: 'post-burst-readiness',
      test_type: 'recovery',
    },
  });

  check(response, {
    'Backend remains ready after burst': (res) => res.status === 200,
  });
}

export function handleSummary(data) {
  return {
    stdout:
      `Burst summary written to ${SUMMARY_PATH}\n`,
    [SUMMARY_PATH]: JSON.stringify(data, null, 2),
  };
}