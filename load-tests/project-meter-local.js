import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || '').replace(/\/$/, '');

const PROJECT_IDS = (__ENV.PROJECT_IDS || __ENV.PROJECT_ID || '27')
  .split(',')
  .map((value) => value.trim())
  .filter(Boolean);

const AUTH_TOKENS = (__ENV.AUTH_TOKENS || '')
  .split(',')
  .map((value) => value.trim())
  .filter(Boolean);

const TARGET_USERS = Number(__ENV.TARGET_USERS || '5');
const HOLD_DURATION = __ENV.HOLD_DURATION || '2m';
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || '10s';

const ALLOW_SHARED_IDENTITY =
  __ENV.ALLOW_SHARED_IDENTITY === 'true';

const EXPECT_RATE_LIMITING =
  __ENV.EXPECT_RATE_LIMITING === 'true';

const MAX_FAILURE_LOGS_PER_VU = Number(
  __ENV.MAX_FAILURE_LOGS_PER_VU || '2'
);

const MIN_THINK_TIME_SECONDS = Number(
  __ENV.MIN_THINK_TIME_SECONDS || '2'
);

const MAX_THINK_TIME_SECONDS = Number(
  __ENV.MAX_THINK_TIME_SECONDS || '5'
);

const SUMMARY_PATH =
  __ENV.SUMMARY_PATH ||
  'project-meter-local-summary.json';

if (!BASE_URL) {
  throw new Error('BASE_URL is required');
}

if (![1, 5, 10, 15, 25].includes(TARGET_USERS)) {
  throw new Error(
    'TARGET_USERS must be one of 1, 5, 10, 15, or 25'
  );
}

if (PROJECT_IDS.length === 0) {
  throw new Error(
    'At least one PROJECT_ID or PROJECT_IDS value is required'
  );
}

if (
  !Number.isFinite(MIN_THINK_TIME_SECONDS) ||
  !Number.isFinite(MAX_THINK_TIME_SECONDS) ||
  MIN_THINK_TIME_SECONDS < 0 ||
  MAX_THINK_TIME_SECONDS < MIN_THINK_TIME_SECONDS
) {
  throw new Error(
    'Think-time values are invalid. ' +
      'MAX_THINK_TIME_SECONDS must be greater than or equal to ' +
      'MIN_THINK_TIME_SECONDS.'
  );
}

/*
 * Anonymous VUs share a source IP and therefore usually share the
 * PUBLIC_PROJECT_READ limiter bucket.
 *
 * For a backend-capacity test above 5 VUs, provide one token per VU:
 *
 * AUTH_TOKENS='token1,token2,...'
 *
 * To intentionally test shared-IP/shared-identity rate limiting, use:
 *
 * ALLOW_SHARED_IDENTITY=true
 * EXPECT_RATE_LIMITING=true
 */
if (
  TARGET_USERS > 5 &&
  AUTH_TOKENS.length < TARGET_USERS &&
  !ALLOW_SHARED_IDENTITY
) {
  throw new Error(
    `${TARGET_USERS} VUs require at least ${TARGET_USERS} AUTH_TOKENS ` +
      'entries, or ALLOW_SHARED_IDENTITY=true for an intentional ' +
      'shared-identity rate-limit test.'
  );
}

const meterFailures = new Rate('meter_failures');

const meterDuration = new Trend(
  'meter_duration',
  true
);

const meterSuccessDuration = new Trend(
  'meter_success_duration',
  true
);

const meterResponseSize = new Trend(
  'meter_response_size_bytes'
);

const meterSuccessResponseSize = new Trend(
  'meter_success_response_size_bytes'
);

const meterRateLimited = new Rate(
  'meter_rate_limited'
);

const meterRateLimitedCount = new Counter(
  'meter_rate_limited_count'
);

const meterServerErrors = new Rate(
  'meter_server_errors'
);

const meterContractFailures = new Rate(
  'meter_contract_failures'
);

let failureLogsForVu = 0;

function buildStages(target) {
  if (target === 1) {
    return [
      { duration: '10s', target: 1 },
      { duration: HOLD_DURATION, target: 1 },
      { duration: '10s', target: 0 },
    ];
  }

  const levels = [5, 10, 15, 25]
    .filter((level) => level <= target);

  return [
    ...levels.flatMap((level) => [
      { duration: '30s', target: level },
      { duration: HOLD_DURATION, target: level },
    ]),
    { duration: '30s', target: 0 },
  ];
}

const thresholds = {
  meter_success_duration: [
    'p(90)<1000',
    'p(95)<2000',
    'p(99)<4000',
  ],

  meter_server_errors: [
    {
      threshold: 'rate<0.01',
      abortOnFail: true,
      delayAbortEval: '20s',
    },
  ],

  meter_contract_failures: [
    {
      threshold: 'rate<0.01',
      abortOnFail: true,
      delayAbortEval: '20s',
    },
  ],
};

/*
 * During a normal capacity test, any 429 is a test failure.
 *
 * During an intentional limiter test, 429 responses are measured but do
 * not abort the run.
 */
if (!EXPECT_RATE_LIMITING) {
  thresholds.http_req_failed = [
    {
      threshold: 'rate<0.01',
      abortOnFail: true,
      delayAbortEval: '20s',
    },
  ];

  thresholds.meter_failures = [
    {
      threshold: 'rate<0.01',
      abortOnFail: true,
      delayAbortEval: '20s',
    },
  ];

  thresholds.meter_rate_limited = [
    {
      threshold: 'rate==0',
      abortOnFail: true,
      delayAbortEval: '20s',
    },
  ];
}

export const options = {
  scenarios: {
    project_meter_local: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: buildStages(TARGET_USERS),
      gracefulRampDown: '30s',
      gracefulStop: '30s',
    },
  },

  thresholds,

  summaryTrendStats: [
    'avg',
    'min',
    'med',
    'max',
    'p(90)',
    'p(95)',
    'p(99)',
  ],
};

function tokenForCurrentVu() {
  if (AUTH_TOKENS.length === 0) {
    return null;
  }

  const tokenIndex =
    Math.max(0, (__VU || 1) - 1) %
    AUTH_TOKENS.length;

  return AUTH_TOKENS[tokenIndex];
}

function buildRequestParams() {
  const headers = {
    Accept: 'application/json',
    'User-Agent': 'SFS-k6-project-meter-local/2.0',
  };

  const token = tokenForCurrentVu();

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return {
    headers,
    timeout: REQUEST_TIMEOUT,
    tags: {
      route: 'project-meter',
      name: 'GET /api/projects/:id/meter',
      authentication:
        token !== null ? 'authenticated' : 'anonymous',
    },
  };
}

function selectProjectId() {
  const index =
    ((__VU || 1) + (__ITER || 0) - 1) %
    PROJECT_IDS.length;

  return PROJECT_IDS[index];
}

function parseJsonSafely(response) {
  if (response.status !== 200 || !response.body) {
    return null;
  }

  try {
    return response.json();
  } catch (_) {
    return null;
  }
}

function validateMeterContract(response, body) {
  return check(response, {
    'Meter: status is 200': (res) =>
      res.status === 200,

    'Meter: response is JSON': (res) =>
      (res.headers['Content-Type'] || '')
        .toLowerCase()
        .includes('application/json'),

    'Meter: response body exists': (res) =>
      Boolean(res.body && res.body.length > 0),

    'Meter: project exists': () =>
      body !== null &&
      body.project !== null &&
      body.project !== undefined,

    'Meter: project id exists': () =>
      body?.project?.id !== null &&
      body?.project?.id !== undefined,

    'Meter: project name exists': () =>
      typeof body?.project?.name === 'string' &&
      body.project.name.trim().length > 0,

    'Meter: media exists': () =>
      body?.media !== null &&
      body?.media !== undefined,

    'Meter: media items is array': () =>
      Array.isArray(body?.media?.items),

    'Meter: floorPlanGroups is array': () =>
      Array.isArray(body?.floorPlanGroups),

    'Meter: connectivity exists': () =>
      body?.connectivity !== null &&
      body?.connectivity !== undefined,

    'Meter: summary exists': () =>
      body?.summary !== null &&
      body?.summary !== undefined,

    'Meter: construction exists': () =>
      body?.construction !== null &&
      body?.construction !== undefined,
  });
}

function logFailure({
  projectId,
  response,
  reason,
}) {
  if (
    failureLogsForVu >=
    MAX_FAILURE_LOGS_PER_VU
  ) {
    return;
  }

  failureLogsForVu += 1;

  console.error(
    `Meter request failed: ` +
      `reason=${reason}, ` +
      `vu=${__VU}, ` +
      `iteration=${__ITER}, ` +
      `projectId=${projectId}, ` +
      `status=${response.status}, ` +
      `duration=${response.timings.duration}ms, ` +
      `body=${(response.body || '').slice(0, 400)}`
  );
}

function thinkTime() {
  const duration =
    Math.random() *
      (
        MAX_THINK_TIME_SECONDS -
        MIN_THINK_TIME_SECONDS
      ) +
    MIN_THINK_TIME_SECONDS;

  sleep(duration);
}

function staggerFirstIteration() {
  if (__ITER === 0) {
    sleep(Math.random() * 3);
  }
}

export default function () {
  staggerFirstIteration();

  const projectId = selectProjectId();

  const response = http.get(
    `${BASE_URL}/api/projects/${projectId}/meter`,
    buildRequestParams()
  );

  meterDuration.add(
    response.timings.duration
  );

  meterResponseSize.add(
    response.body ? response.body.length : 0
  );

  const rateLimited =
    response.status === 429;

  const serverError =
    response.status >= 500;

  meterRateLimited.add(rateLimited);
  meterServerErrors.add(serverError);

  if (rateLimited) {
    meterRateLimitedCount.add(1);
  }

  if (response.status === 200) {
    meterSuccessDuration.add(
      response.timings.duration
    );

    meterSuccessResponseSize.add(
      response.body
        ? response.body.length
        : 0
    );
  }

  const body = parseJsonSafely(response);

  let contractPassed = false;

  if (response.status === 200) {
    contractPassed =
      validateMeterContract(response, body);
  }

  meterContractFailures.add(
    response.status === 200 &&
      !contractPassed
  );

  const requestPassed =
    response.status === 200 &&
    contractPassed;

  /*
   * In an intentional limiter test, a valid 429 response is recorded but
   * is not treated as a meter failure.
   */
  const expectedRateLimitResponse =
    EXPECT_RATE_LIMITING &&
    response.status === 429;

  meterFailures.add(
    !requestPassed &&
      !expectedRateLimitResponse
  );

  if (
    !requestPassed &&
    !expectedRateLimitResponse
  ) {
    logFailure({
      projectId,
      response,
      reason:
        response.status === 429
          ? 'unexpected-rate-limit'
          : response.status >= 500
            ? 'server-error'
            : response.status !== 200
              ? 'unexpected-status'
              : 'contract-validation',
    });
  }

  thinkTime();
}

export function handleSummary(data) {
  const failedThresholds =
    Object.entries(data.metrics)
      .filter(([, metric]) => {
        if (!metric.thresholds) {
          return false;
        }

        return Object.values(
          metric.thresholds
        ).some((threshold) => !threshold.ok);
      })
      .map(([name]) => name);

  const summary = {
    baseUrl: BASE_URL,
    projectIds: PROJECT_IDS,
    targetUsers: TARGET_USERS,
    holdDuration: HOLD_DURATION,
    authenticatedTokenCount:
      AUTH_TOKENS.length,
    allowSharedIdentity:
      ALLOW_SHARED_IDENTITY,
    expectRateLimiting:
      EXPECT_RATE_LIMITING,
    failedThresholds,
    k6: data,
  };

  return {
    stdout:
      `Summary written to ${SUMMARY_PATH}. ` +
      `Failed thresholds: ${
        failedThresholds.length > 0
          ? failedThresholds.join(', ')
          : 'none'
      }\n`,

    [SUMMARY_PATH]:
      JSON.stringify(summary, null, 2),
  };
}