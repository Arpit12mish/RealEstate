# Rate Limiting — Mobile App Integration Notes

This is for the frontend/mobile team consuming the SFS API. It explains what
a rate-limit response looks like and how the app should react, so a real user
hitting a limit gets a decent experience instead of a confusing crash or an
unintended logout.

## 1. What a 429 looks like

Every endpoint governed by `com.brandPitara.sfs.ratelimit` returns the exact
same shape when blocked - one contract, regardless of which screen/action
triggered it:

```
HTTP/1.1 429 Too Many Requests
Retry-After: 42
Content-Type: application/json

{
  "status": 429,
  "error": "TOO_MANY_REQUESTS",
  "message": "Too many requests. Please try again later.",
  "retryAfterSeconds": 42,
  "policy": "MOBILE_OTP_REQUEST"
}
```

- `retryAfterSeconds` and the `Retry-After` header always agree, and
  `retryAfterSeconds` is always `>= 1` (never `0`).
- `policy` names which internal rule was hit (e.g. `MOBILE_REVIEW_WRITE`,
  `PUBLIC_SEARCH`). Treat it as a debugging hint, not something to branch UI
  logic on - the numeric `retryAfterSeconds` is what should drive behavior.
- This is a distinct status/shape from a normal validation error (`400`) or an
  auth failure (`401`/`403`) - don't conflate them.

## 2. How to read `Retry-After`

- Prefer the JSON body's `retryAfterSeconds` (already parsed as a number) over
  parsing the `Retry-After` header string, but either is safe to use.
- Use it to schedule a retry/countdown - don't retry immediately, and don't
  poll faster than once per second regardless of what the value says.

## 3. When to show "Please try again later"

Show a lightweight, non-blocking message for any `429` where the underlying
action isn't critical to app function - e.g. a search box, a list refresh, a
"load more" tap. Use `retryAfterSeconds` to say roughly how long
("Try again in a moment" for < 5s, "Try again in about a minute" for larger
values) rather than a live ticking countdown for every case - reserve an
actual countdown UI for OTP (below), where the user is actively waiting.

## 4. Do not retry aggressively on 429

- No automatic immediate retry loop. A `429` means "the server is telling you
  to slow down" - hammering it faster only makes the backoff worse for that
  user and doesn't help anyone.
- If you implement any automatic retry, it must wait at least
  `retryAfterSeconds` and should still require a real user action (e.g.
  pull-to-refresh) rather than firing on its own for most flows.
- Do not treat a burst of `429`s as a reason to fall back to a different
  endpoint or a cache-bypass request - that just moves the same problem
  elsewhere.

## 5. OTP flows: show a countdown timer

`MOBILE_OTP_REQUEST` and `MOBILE_OTP_VERIFY` are the tightest, most
user-visible limits (a handful of attempts per minute, with an hourly cap
too). On a `429` from `/api/auth/request-otp` or `/api/auth/verify-otp`:

- Disable the "Resend OTP" / "Verify" button immediately.
- Start a visible countdown from `retryAfterSeconds`.
- Re-enable the button only when the countdown reaches zero - do not let the
  user bypass it by backgrounding/foregrounding the app or retrying the
  network call manually.
- Show a clear, calm message ("Too many attempts. You can try again in
  {seconds}s.") - never imply the account is locked or blocked permanently;
  this is a short, self-resetting window.

## 6. Search / public read APIs: debounce and throttle client-side

`PUBLIC_SEARCH`, `PUBLIC_HOME_READ`, `PUBLIC_PROJECT_READ`, and the other
public `GET` policies are generous (60-120/min per IP) specifically so normal
scrolling/searching never trips them. If you ever see a `429` from one of
these in normal use, it almost always means the client is calling far more
often than a human would - add/verify:

- A debounce on search-as-you-type (e.g. 300ms after the last keystroke, not
  on every keystroke).
- Throttling on scroll-triggered "load more" / infinite-scroll calls (don't
  fire a new page request before the previous one resolves).
- No polling loop that re-fetches the same list on a fixed short interval
  "just in case" - fetch on user action or a reasonable app-lifecycle event
  instead.

## 7. Favorite / review actions: disable the button temporarily after 429

`MOBILE_FAVORITE_WRITE`, `MOBILE_REVIEW_WRITE`, and similar action policies
are tighter (5-60/min) because they're real state changes, not reads. On a
`429`:

- Disable the specific button/control that triggered the action (don't lock
  the whole screen).
- Re-enable after `retryAfterSeconds`, or on next user-initiated retry if
  that's simpler - either is fine as long as it's not an immediate auto-retry.
- Do not optimistically flip the UI state (e.g. showing "favorited") until the
  request actually succeeds - a `429` should leave the UI exactly as it was
  before the tap.

## 8. Do not treat 429 as logout

A `429` is not a `401`. It says nothing about the validity of the current
session/token. Do not:

- Clear the stored access/refresh token.
- Navigate to the login screen.
- Force a token refresh in response to a `429` on an unrelated endpoint.

If your HTTP client has a global interceptor that reacts to non-2xx statuses,
make sure it special-cases `429` separately from `401`/`403` before doing
anything session-related.

## 9. Do not refresh the token repeatedly on 429 - unless it's the refresh endpoint itself

`MOBILE_TOKEN_REFRESH` (`POST /api/auth/refresh`) is itself rate-limited
(`IP_AND_TOKEN` keyed). If *that specific* call returns `429`:

- Back off using `retryAfterSeconds` before calling `/api/auth/refresh` again.
- Do not spin up a second, parallel refresh attempt while one is already
  pending/backing off.

For every *other* endpoint, a `429` is never a signal to trigger a token
refresh - refreshing the token doesn't change the rate-limit key (IP, phone,
userId, etc. are unaffected by getting a new access token), so it would only
add load without helping the user get past the limit sooner.
