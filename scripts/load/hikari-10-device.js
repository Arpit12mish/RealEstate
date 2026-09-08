import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const projectIds = (__ENV.PROJECT_IDS || '44,46,53,56,76')
  .split(',')
  .map((value) => value.trim())
  .filter(Boolean);

export const options = {
  scenarios: {
    ten_devices: {
      executor: 'constant-vus',
      vus: 10,
      duration: __ENV.DURATION || '2m',
      gracefulStop: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate==0'],
    http_req_duration: ['p(95)<1500'],
  },
};

export default function () {
  const projectId = projectIds[(__VU + __ITER) % projectIds.length];
  const response = http.get(`${baseUrl}/api/projects/${projectId}`, {
    headers: { 'X-Request-ID': `hikari-${__VU}-${__ITER}` },
    tags: { endpoint: 'project-detail' },
  });

  check(response, {
    'project detail succeeds': (result) => result.status >= 200 && result.status < 400,
    'no connection timeout': (result) => !result.body.includes('Connection is not available'),
  });
  sleep(0.2);
}
