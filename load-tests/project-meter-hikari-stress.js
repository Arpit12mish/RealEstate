import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import exec from 'k6/execution';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8084').replace(/\/$/, '');
const PROJECT_IDS = (__ENV.PROJECT_IDS || __ENV.PROJECT_ID || '27')
  .split(',').map((value) => value.trim()).filter(Boolean);
const AUTH_TOKENS = (__ENV.AUTH_TOKENS || '')
  .split(',').map((value) => value.trim()).filter(Boolean);
const START_RATE = Number(__ENV.START_RATE || '10');
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || '100');
const MAX_VUS = Number(__ENV.MAX_VUS || '300');
const STAGE_DURATION = __ENV.STAGE_DURATION || '60s';
const SUMMARY_PATH = __ENV.SUMMARY_PATH || 'project-meter-hikari-stress-summary.json';
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || '10s';
const RATES = (__ENV.RATES || '10,25,50,75,100')
  .split(',').map(Number).filter((rate) => Number.isFinite(rate) && rate > 0);

if (PROJECT_IDS.length === 0) throw new Error('PROJECT_ID or PROJECT_IDS is required');
if (AUTH_TOKENS.length !== 10) throw new Error('Exactly 10 AUTH_TOKENS are required');
if (RATES.length === 0) throw new Error('RATES must contain positive numbers');

const successDuration = new Trend('meter_success_duration', true);
const responseSize = new Trend('meter_response_size_bytes');
const rateLimited = new Rate('meter_rate_limited');
const serverErrors = new Rate('meter_server_errors');
const contractFailures = new Rate('meter_contract_failures');
const successfulRequests = new Counter('meter_successful_requests');
const totalRequests = new Counter('meter_requests');
const rateLimitedCount = new Counter('meter_rate_limited_count');
const serverErrorCount = new Counter('meter_server_error_count');

const thresholds = {
  meter_rate_limited: ['rate==0'],
  meter_server_errors: ['rate<0.01'],
  meter_contract_failures: ['rate==0'],
  meter_success_duration: ['p(95)<500', 'p(99)<1000'],
  dropped_iterations: ['count==0'],
};
for (const rate of RATES) {
  const selector = `stage_rps:${rate}`;
  thresholds[`meter_requests{${selector}}`] = ['count>=0'];
  thresholds[`meter_successful_requests{${selector}}`] = ['count>=0'];
  thresholds[`meter_rate_limited{${selector}}`] = ['rate==0'];
  thresholds[`meter_server_errors{${selector}}`] = ['rate<0.01'];
  thresholds[`meter_contract_failures{${selector}}`] = ['rate==0'];
  thresholds[`meter_success_duration{${selector}}`] = ['p(95)<500', 'p(99)<1000'];
}

export const options = {
  scenarios: {
    meter_capacity: {
      executor: 'ramping-arrival-rate',
      startRate: START_RATE,
      timeUnit: '1s',
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
      stages: RATES.map((target) => ({ target, duration: STAGE_DURATION })),
      gracefulStop: '30s',
    },
  },
  thresholds,
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

function tokenForVu() {
  return AUTH_TOKENS[Math.max(0, (__VU || 1) - 1) % AUTH_TOKENS.length];
}

function validContract(body) {
  return body && body.project && body.project.id != null && body.media
    && Array.isArray(body.media.items) && Array.isArray(body.floorPlanGroups)
    && body.connectivity != null;
}

export default function () {
  const stageIndex = Math.min(RATES.length - 1, Math.floor(exec.scenario.progress * RATES.length));
  const stageTags = { stage_rps: String(RATES[stageIndex]) };
  const projectId = PROJECT_IDS[(__ITER + (__VU || 1) - 1) % PROJECT_IDS.length];
  const response = http.get(`${BASE_URL}/api/projects/${projectId}/meter`, {
    headers: {
      Accept: 'application/json',
      Authorization: `Bearer ${tokenForVu()}`,
      'User-Agent': 'SFS-k6-hikari-stress/1.0',
    },
    timeout: REQUEST_TIMEOUT,
    tags: { route: 'project-meter', name: 'GET /api/projects/:id/meter', ...stageTags },
  });

  const is429 = response.status === 429;
  const is5xx = response.status >= 500;
  totalRequests.add(1, stageTags);
  rateLimited.add(is429, stageTags);
  serverErrors.add(is5xx, stageTags);
  responseSize.add(response.body ? response.body.length : 0, stageTags);
  if (is429) rateLimitedCount.add(1);
  if (is5xx) serverErrorCount.add(1);

  let body = null;
  if (response.status === 200) {
    try { body = response.json(); } catch (_) { body = null; }
  }
  const contractOk = response.status === 200 && validContract(body);
  contractFailures.add(response.status === 200 && !contractOk, stageTags);
  if (response.status === 200) {
    successDuration.add(response.timings.duration, stageTags);
    successfulRequests.add(1, stageTags);
  }

  check(response, {
    'status is 200': (res) => res.status === 200,
    'content type is JSON': (res) => (res.headers['Content-Type'] || '').toLowerCase().includes('application/json'),
    'project exists': () => body?.project?.id != null,
    'media exists': () => body?.media != null,
    'floorPlanGroups is array': () => Array.isArray(body?.floorPlanGroups),
    'connectivity exists': () => body?.connectivity != null,
  }, stageTags);
}

export function handleSummary(data) {
  const summary = {
    baseUrl: BASE_URL,
    projectIds: PROJECT_IDS,
    authenticatedTokenCount: AUTH_TOKENS.length,
    rates: RATES,
    stageDuration: STAGE_DURATION,
    k6: data,
  };
  return {
    stdout: `Stress summary written to ${SUMMARY_PATH}\n`,
    [SUMMARY_PATH]: JSON.stringify(summary, null, 2),
  };
}
