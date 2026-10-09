import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8084').replace(/\/$/, '');
const PROJECT_IDS = (__ENV.PROJECT_IDS || '').split(',').map((value) => value.trim()).filter(Boolean);
const BUILDER_ID = __ENV.BUILDER_ID || '';
const AUTH_TOKENS = (__ENV.AUTH_TOKENS || '').split(',').map((value) => value.trim()).filter(Boolean);
const RATE = Number(__ENV.RATE || '25');
const DURATION = __ENV.DURATION || '2m';
const PRE_ALLOCATED_VUS = Number(__ENV.PRE_ALLOCATED_VUS || '60');
const MAX_VUS = Number(__ENV.MAX_VUS || '200');
const SUMMARY_PATH = __ENV.SUMMARY_PATH || 'mixed-backend-hikari-stress-summary.json';

if (PROJECT_IDS.length < 2) throw new Error('PROJECT_IDS must contain at least two real project IDs');
if (!BUILDER_ID) throw new Error('BUILDER_ID is required');
if (AUTH_TOKENS.length !== 10) throw new Error('Exactly 10 AUTH_TOKENS are required');

const requestDuration = new Trend('mixed_success_duration', true);
const rateLimited = new Rate('mixed_rate_limited');
const serverErrors = new Rate('mixed_server_errors');
const contractFailures = new Rate('mixed_contract_failures');
const responseSize = new Trend('mixed_response_size_bytes');
const successCount = new Counter('mixed_success_count');

export const options = {
  scenarios: {
    realistic_mix: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: PRE_ALLOCATED_VUS,
      maxVUs: MAX_VUS,
      gracefulStop: '30s',
    },
  },
  thresholds: {
    mixed_rate_limited: ['rate==0'],
    mixed_server_errors: ['rate<0.01'],
    mixed_contract_failures: ['rate==0'],
    mixed_success_duration: ['p(95)<750', 'p(99)<1500'],
    dropped_iterations: ['count==0'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

function params(name, method = 'GET') {
  const token = AUTH_TOKENS[Math.max(0, (__VU || 1) - 1) % AUTH_TOKENS.length];
  return {
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      'User-Agent': 'SFS-k6-mixed-hikari-stress/1.0',
    },
    timeout: '10s',
    tags: { route: name, name, method },
  };
}

function chooseRequest() {
  const projectId = PROJECT_IDS[(__ITER + (__VU || 1)) % PROJECT_IDS.length];
  const bucket = (__ITER * 17 + (__VU || 1) * 13) % 100;
  if (bucket < 40) return ['GET', `/api/projects/${projectId}/meter`, null, 'meter'];
  if (bucket < 60) return ['GET', '/api/public/home', null, 'home'];
  if (bucket < 75) return ['GET', `/api/projects/${projectId}`, null, 'project-detail'];
  if (bucket < 85) return ['GET', '/api/public/search?q=project&page=0&size=10', null, 'search'];
  if (bucket < 90) return ['POST', '/api/projects/compare', JSON.stringify({ projectIds: PROJECT_IDS.slice(0, 2) }), 'compare'];
  if (bucket < 95) return ['GET', `/api/project-favorites/${projectId}/exists`, null, 'favorite-read'];
  if (bucket < 98) return ['GET', `/api/projects/${projectId}/reviews`, null, 'review-read'];
  return ['GET', `/api/builders/${BUILDER_ID}/credibility`, null, 'builder-credibility'];
}

export default function () {
  const [method, path, body, route] = chooseRequest();
  const response = method === 'POST'
    ? http.post(`${BASE_URL}${path}`, body, params(route, method))
    : http.get(`${BASE_URL}${path}`, params(route, method));

  const is429 = response.status === 429;
  const is5xx = response.status >= 500;
  rateLimited.add(is429);
  serverErrors.add(is5xx);
  responseSize.add(response.body ? response.body.length : 0);

  let jsonBody = null;
  if (response.status === 200) {
    try { jsonBody = response.json(); } catch (_) { jsonBody = null; }
    requestDuration.add(response.timings.duration, { route });
    successCount.add(1, { route });
  }
  const contractOk = response.status === 200 && jsonBody != null;
  contractFailures.add(response.status === 200 && !contractOk, { route });
  check(response, {
    'mixed status is 200': (res) => res.status === 200,
    'mixed response is JSON': (res) => (res.headers['Content-Type'] || '').toLowerCase().includes('application/json'),
    'mixed body parses': () => jsonBody != null,
  }, { route });
}

export function handleSummary(data) {
  return {
    stdout: `Mixed-flow summary written to ${SUMMARY_PATH}\n`,
    [SUMMARY_PATH]: JSON.stringify({
      baseUrl: BASE_URL,
      projectIds: PROJECT_IDS,
      builderId: BUILDER_ID,
      authenticatedTokenCount: AUTH_TOKENS.length,
      rate: RATE,
      duration: DURATION,
      k6: data,
    }, null, 2),
  };
}
