const PRODUCTION_HOSTS = new Set([
  'appapi.squarefootstory.com',
  '13.235.101.204',
]);

export function requireSafeTarget() {
  const targetEnv = (__ENV.TARGET_ENV || '').trim().toLowerCase();
  const baseUrl = (__ENV.BASE_URL || '').trim().replace(/\/+$/, '');

  if (!targetEnv) throw new Error('TARGET_ENV is required (local or staging)');
  if (!baseUrl) throw new Error('BASE_URL is required and has no default');
  if (!['local', 'staging'].includes(targetEnv)) {
    throw new Error(`TARGET_ENV=${targetEnv} is forbidden; use local or staging`);
  }

  const parsed = /^(https?):\/\/(\[[0-9a-fA-F:]+\]|[^\s\/:?#@]+)(?::\d{1,5})?(?:[\/?#]|$)/.exec(baseUrl);
  if (!parsed) {
    throw new Error('BASE_URL must be an absolute http(s) URL');
  }
  const hostname = parsed[2].replace(/^\[|\]$/g, '').toLowerCase();
  if (PRODUCTION_HOSTS.has(hostname)) {
    throw new Error(`Production host is forbidden: ${hostname}`);
  }
  // `nginx` is the private Compose service name used when k6 itself runs on
  // the local-staging bridge. It is not resolvable outside that Docker network.
  if (targetEnv === 'local' && !['127.0.0.1', 'localhost', '::1', 'nginx'].includes(hostname)) {
    throw new Error('TARGET_ENV=local requires loopback or the local Compose nginx service');
  }

  return Object.freeze({ targetEnv, baseUrl });
}
