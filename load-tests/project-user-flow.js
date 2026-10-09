import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL;
const PROJECT_ID = __ENV.PROJECT_ID || '51';
const FEED_SCREEN = __ENV.FEED_SCREEN || 'HOME';

if (!BASE_URL) {
  throw new Error('BASE_URL environment variable is required');
}

const businessErrors = new Rate('business_errors');

const homeDuration = new Trend('home_duration', true);
const feedDuration = new Trend('feed_duration', true);
const featuredProjectsDuration = new Trend(
  'featured_projects_duration',
  true
);
const projectDetailDuration = new Trend(
  'project_detail_duration',
  true
);
const projectMediaDuration = new Trend(
  'project_media_duration',
  true
);
const projectMeterDuration = new Trend(
  'project_meter_duration',
  true
);
const projectFloorPlansDuration = new Trend(
  'project_floor_plans_duration',
  true
);

export const options = {
  scenarios: {
    realistic_mobile_users: {
      executor: 'ramping-vus',
      startVUs: 0,

      stages: [
        { duration: '30s', target: 2 },
        { duration: '1m', target: 5 },
        { duration: '2m', target: 10 },
        { duration: '1m', target: 5 },
        { duration: '30s', target: 0 },
      ],

      gracefulRampDown: '30s',
    },
  },

  thresholds: {
    http_req_failed: ['rate<0.01'],

    http_req_duration: [
      'p(90)<1000',
      'p(95)<1500',
      'p(99)<3000',
    ],

    business_errors: ['rate<0.01'],

    home_duration: ['p(95)<1500'],
    feed_duration: ['p(95)<1500'],
    featured_projects_duration: ['p(95)<1500'],
    project_detail_duration: ['p(95)<1500'],
    project_media_duration: ['p(95)<1500'],
    project_meter_duration: ['p(95)<2000'],
    project_floor_plans_duration: ['p(95)<1500'],
  },
};

const defaultParams = {
  headers: {
    Accept: 'application/json',
    'User-Agent': 'SFS-k6-load-test/1.0',
  },

  timeout: '20s',
};

function verifyResponse(response, requestName) {
  const passed = check(response, {
    [`${requestName}: status is 200`]: (res) => res.status === 200,

    [`${requestName}: response is JSON`]: (res) => {
      const contentType = res.headers['Content-Type'] || '';
      return contentType.includes('application/json');
    },

    [`${requestName}: response body exists`]: (res) =>
      Boolean(res.body && res.body.length > 0),
  });

  businessErrors.add(!passed);

  if (!passed) {
    console.error(
      `${requestName} failed: ` +
        `status=${response.status}, ` +
        `duration=${response.timings.duration}ms, ` +
        `body=${response.body?.slice(0, 500)}`
    );
  }

  return passed;
}

function randomThinkTime(minSeconds, maxSeconds) {
  sleep(
    Math.random() * (maxSeconds - minSeconds) +
      minSeconds
  );
}

export default function () {
  group('Open home screen', () => {
    const response = http.get(
      `${BASE_URL}/api/public/home`,
      {
        ...defaultParams,
        tags: {
          name: 'GET /api/public/home',
          activity: 'home',
        },
      }
    );

    homeDuration.add(response.timings.duration);
    verifyResponse(response, 'Public home');
  });

  randomThinkTime(1, 3);

  group('Load home feed', () => {
    const response = http.get(
      `${BASE_URL}/api/public/feed?screen=${encodeURIComponent(FEED_SCREEN)}`,
      {
        ...defaultParams,
        tags: {
          name: 'GET /api/public/feed',
          activity: 'home-feed',
        },
      }
    );

    feedDuration.add(response.timings.duration);
    verifyResponse(response, 'Home feed');
  });

  randomThinkTime(1, 3);

  group('Browse featured projects', () => {
    const response = http.get(
      `${BASE_URL}/api/projects/feature`,
      {
        ...defaultParams,
        tags: {
          name: 'GET /api/projects/feature',
          activity: 'featured-projects',
        },
      }
    );

    featuredProjectsDuration.add(
      response.timings.duration
    );

    verifyResponse(response, 'Featured projects');
  });

  randomThinkTime(2, 5);

  group('Open project detail', () => {
    const response = http.get(
      `${BASE_URL}/api/projects/${PROJECT_ID}`,
      {
        ...defaultParams,
        tags: {
          name: 'GET /api/projects/:id',
          activity: 'project-detail',
        },
      }
    );

    projectDetailDuration.add(
      response.timings.duration
    );

    const passed = verifyResponse(
      response,
      'Project detail'
    );

    if (passed) {
      const body = response.json();

      const detailPassed = check(body, {
        'Project detail contains id': (data) =>
          data.id !== null &&
          data.id !== undefined,

        'Project detail contains propertyTypes': (
          data
        ) => Array.isArray(data.propertyTypes),
      });

      businessErrors.add(!detailPassed);
    }
  });

  randomThinkTime(1, 3);

  group('Load project media', () => {
    const response = http.get(
      `${BASE_URL}/api/projects/${PROJECT_ID}/media`,
      {
        ...defaultParams,
        tags: {
          name: 'GET /api/projects/:id/media',
          activity: 'project-media',
        },
      }
    );

    projectMediaDuration.add(
      response.timings.duration
    );

    verifyResponse(response, 'Project media');
  });

  randomThinkTime(1, 3);

  group('Load project meter', () => {
    const response = http.get(
      `${BASE_URL}/api/projects/${PROJECT_ID}/meter`,
      {
        ...defaultParams,
        tags: {
          name: 'GET /api/projects/:id/meter',
          activity: 'project-meter',
        },
      }
    );

    projectMeterDuration.add(
      response.timings.duration
    );

    verifyResponse(response, 'Project meter');
  });

  randomThinkTime(1, 3);

  group('Load project floor plans', () => {
    const response = http.get(
      `${BASE_URL}/api/projects/${PROJECT_ID}/floor-plans`,
      {
        ...defaultParams,
        tags: {
          name: 'GET /api/projects/:id/floor-plans',
          activity: 'project-floor-plans',
        },
      }
    );

    projectFloorPlansDuration.add(
      response.timings.duration
    );

    verifyResponse(response, 'Project floor plans');
  });

  randomThinkTime(2, 5);
}