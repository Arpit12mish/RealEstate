import http from 'k6/http';
import { check, sleep } from 'k6';
import { requireSafeTarget } from './lib/config.js';

const target = requireSafeTarget();

export const options = {
  scenarios: {
    smoke: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 3,
      maxDuration: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate==0'],
    http_req_duration: ['p(95)<1500'],
  },
};

export default function () {
  const responses = http.batch([
    ['GET', `${target.baseUrl}/actuator/health/readiness`, null, { tags: { endpoint: 'readiness' } }],
    ['GET', `${target.baseUrl}/api/public/cities/trending`, null, { tags: { endpoint: 'cities' } }],
  ]);
  check(responses[0], { 'readiness is UP': (r) => r.status === 200 && r.body.includes('UP') });
  check(responses[1], { 'public API succeeds': (r) => r.status >= 200 && r.status < 400 });
  sleep(1);
}
