package com.brandPitara.sfs.ratelimit.filter;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.exception.RequestBodyTooLargeException;
import com.brandPitara.sfs.ratelimit.model.RateLimitDecision;
import com.brandPitara.sfs.ratelimit.model.RateLimitErrorResponse;
import com.brandPitara.sfs.ratelimit.model.RateLimitRequestContext;
import com.brandPitara.sfs.ratelimit.resolver.ClientIpResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitKeyResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitPolicyResolver;
import com.brandPitara.sfs.ratelimit.service.RateLimitService;
import com.brandPitara.sfs.util.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Runs before controller dispatch: resolves which RateLimitPolicy (if any)
 * governs the incoming request, checks it against RateLimitService, and
 * either lets the request through or short-circuits with 429 + Retry-After.
 * <p>
 * Placed after JwtRequestFilter in the app security filter chain so
 * SecurityContext is already populated for optionally-authenticated public
 * endpoints (e.g. PUBLIC_PROJECT_COMPARE's IP_OR_USER key).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    /** Policies whose key material requires reading the JSON request body. */
    private static final Set<RateLimitPolicy> BODY_AWARE_POLICIES = EnumSet.of(
            RateLimitPolicy.MOBILE_OTP_REQUEST,
            RateLimitPolicy.MOBILE_OTP_VERIFY,
            RateLimitPolicy.MOBILE_TOKEN_REFRESH,
            RateLimitPolicy.MOBILE_GUEST_SESSION,
            RateLimitPolicy.PUBLIC_LOCATION_RESOLVE,
            // Phase 2: only the calculator write policy needs body content (for its
            // BODY_FINGERPRINT dimension). Profile/favorite/review write policies key
            // on IP_OR_USER alone and are intentionally NOT body-wrapped.
            RateLimitPolicy.PUBLIC_CALCULATOR_WRITE
    );

    private final RateLimitPolicyResolver policyResolver;
    private final RateLimitKeyResolver keyResolver;
    private final ClientIpResolver clientIpResolver;
    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        if (!properties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        Optional<RateLimitPolicy> policyMatch = policyResolver.resolve(request);
        if (policyMatch.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        RateLimitPolicy policy = policyMatch.get();
        HttpServletRequest effectiveRequest = request;

        Map<String, Object> body = Map.of();
        if (BODY_AWARE_POLICIES.contains(policy) && HttpMethod.POST.matches(request.getMethod())) {
            long maxBodyBytes = properties.getMaxCachedBodyBytes();

            // Fast path: an honest Content-Length lets us reject before touching the
            // stream at all. CachedBodyHttpServletRequest itself still bounds the read
            // for chunked/absent-Content-Length requests, or a Content-Length that lies.
            long declaredLength = request.getContentLengthLong();
            if (declaredLength > maxBodyBytes) {
                writePayloadTooLargeResponse(response, policy);
                return;
            }

            try {
                CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, maxBodyBytes);
                effectiveRequest = cachedRequest;
                body = tryParseJsonBody(cachedRequest);
            } catch (RequestBodyTooLargeException ex) {
                writePayloadTooLargeResponse(response, policy);
                return;
            }
        }

        RateLimitRequestContext context = buildContext(policy, effectiveRequest, body);
        Set<RateLimitKeyType> requiredKeyTypes = configuredKeyTypes(policy);
        Map<RateLimitKeyType, String> resolvedKeys = keyResolver.resolveKeys(requiredKeyTypes, context);

        RateLimitDecision decision = rateLimitService.checkAndConsume(policy, resolvedKeys);

        if (!decision.allowed()) {
            String maskedKeyHash = maskedKeyHash(resolvedKeys.get(decision.blockedOnKeyType()));
            log.warn(
                    "Rate limit exceeded: policy={} method={} path={} keyType={} keyHash={} retryAfterSeconds={}",
                    policy, request.getMethod(), request.getRequestURI(),
                    decision.blockedOnKeyType(), maskedKeyHash, decision.retryAfterSeconds()
            );
            writeBlockedResponse(response, policy, decision.retryAfterSeconds());
            return;
        }

        chain.doFilter(effectiveRequest, response);
    }

    private Set<RateLimitKeyType> configuredKeyTypes(RateLimitPolicy policy) {
        var config = properties.getPolicies().get(policy);
        if (config == null || config.getLimits() == null) {
            return Set.of();
        }
        Set<RateLimitKeyType> keyTypes = EnumSet.noneOf(RateLimitKeyType.class);
        config.getLimits().forEach(limit -> keyTypes.add(limit.getKeyType()));
        return keyTypes;
    }

    private RateLimitRequestContext buildContext(
            RateLimitPolicy policy,
            HttpServletRequest request,
            Map<String, Object> body
    ) {
        String ip = clientIpResolver.resolve(request);

        return RateLimitRequestContext.builder()
                .ip(ip)
                .phoneNumber(stringField(body, "phoneNumber"))
                .refreshToken(stringField(body, "refreshToken"))
                .installationId(stringField(body, "installationId"))
                .deviceId(resolveDeviceId(policy, request, body))
                .query(policy == RateLimitPolicy.PUBLIC_SEARCH ? request.getParameter("q") : null)
                .userId(currentUserId(request))
                .bodyFingerprint(policy == RateLimitPolicy.PUBLIC_CALCULATOR_WRITE ? canonicalBodyJson(body) : null)
                .build();
    }

    /**
     * Deterministic JSON representation of the parsed body (nested map keys
     * sorted recursively) so identical calculator requests always fingerprint
     * to the same value regardless of client-side key ordering. The result is
     * still the request's own field values, never logged directly - only the
     * SHA-256 hash RateLimitKeyResolver derives from it ever becomes key
     * material or appears (further hashed) in a log line.
     */
    private String canonicalBodyJson(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(canonicalize(body));
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return null;
        }
    }

    private Object canonicalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            var sorted = new java.util.TreeMap<String, Object>();
            map.forEach((k, v) -> sorted.put(String.valueOf(k), canonicalize(v)));
            return sorted;
        }
        if (value instanceof java.util.List<?> list) {
            return list.stream().map(this::canonicalize).toList();
        }
        return value;
    }

    private String resolveDeviceId(RateLimitPolicy policy, HttpServletRequest request, Map<String, Object> body) {
        if (policy == RateLimitPolicy.PUBLIC_LOCATION_RESOLVE) {
            // LocationResolveRequest does not currently declare a deviceId field;
            // read it defensively in case a caller sends one anyway, else fall back
            // to IP-only enforcement for this dimension (see RateLimitKeyResolver).
            return stringField(body, "deviceId");
        }
        return request.getParameter("deviceId");
    }

    private String stringField(Map<String, Object> body, String field) {
        Object value = body.get(field);
        return value instanceof String s ? s : null;
    }

    private Map<String, Object> tryParseJsonBody(CachedBodyHttpServletRequest request) {
        byte[] cachedBody = request.getCachedBody();
        if (cachedBody.length == 0) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(cachedBody, Map.class);
            return parsed != null ? parsed : Map.of();
        } catch (IOException ex) {
            // Malformed JSON: let the real controller's @RequestBody binding produce the
            // usual 400 response; the rate limiter just proceeds without body-derived keys.
            return Map.of();
        }
    }

    /**
     * App JWT principals resolve to canonical phone-number usernames on the
     * SecurityContext (see AppUserDetailsService), not a numeric id, so for
     * IP_OR_USER keying we re-read the userId claim directly off the bearer
     * token the same way JwtRequestFilter already does for MDC population.
     */
    private Long currentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            return jwtTokenUtil.getUserIdFromToken(authHeader.substring(7));
        } catch (Exception ex) {
            return null;
        }
    }

    private String maskedKeyHash(String rawKey) {
        if (rawKey == null) {
            return "n/a";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 6); // short fingerprint, not reversible
        } catch (NoSuchAlgorithmException ex) {
            return "n/a";
        }
    }

    private void writePayloadTooLargeResponse(HttpServletResponse response, RateLimitPolicy policy)
            throws IOException {
        log.warn("Rejecting oversized request body: policy={} maxBodyBytes={}", policy, properties.getMaxCachedBodyBytes());

        response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        RateLimitErrorResponse body = RateLimitErrorResponse.builder()
                .status(HttpStatus.CONTENT_TOO_LARGE.value())
                .error("PAYLOAD_TOO_LARGE")
                .message("Request body is too large.")
                .retryAfterSeconds(0)
                .policy(policy.name())
                .build();

        objectMapper.writeValue(response.getWriter(), body);
    }

    private void writeBlockedResponse(HttpServletResponse response, RateLimitPolicy policy, long retryAfterSeconds)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        RateLimitErrorResponse body = RateLimitErrorResponse.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("TOO_MANY_REQUESTS")
                .message("Too many requests. Please try again later.")
                .retryAfterSeconds(retryAfterSeconds)
                .policy(policy.name())
                .build();

        objectMapper.writeValue(response.getWriter(), body);
    }
}
