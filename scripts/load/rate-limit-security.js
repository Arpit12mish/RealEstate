import http from 'k6/http';
import { check, sleep } from 'k6';
import { requireSafeTarget } from './lib/config.js';

const target = requireSafeTarget();

export const options = {
  scenarios: {
    invalid_bearer_guard: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 110,
      maxDuration: '3m',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1500'],
  },
};

export default function () {
  const response = http.post(
    `${target.baseUrl}/api/auth/refresh`,
    JSON.stringify({ refreshToken: 'local-invalid-value' }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer invalid-local-${__ITER}`,
        'X-Forwarded-For': '198.51.100.77',
      },
      tags: { endpoint: 'invalid-bearer-preauth' },
    },
  );

  check(response, {
    'invalid auth is never accepted': (r) => r.status >= 400,
    '429 retry-after is positive': (r) => {
      if (r.status !== 429) return true;
      const value = Number(r.headers['Retry-After']);
      return Number.isFinite(value) && value >= 1;
    },
  });
  sleep(0.05);
}
