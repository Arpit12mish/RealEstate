package com.brandPitara.sfs.ratelimit.filter;

import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.enums.RateLimitKeyType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.enums.RateLimitFailureMode;
import com.brandPitara.sfs.ratelimit.enums.RateLimitIdentityType;
import com.brandPitara.sfs.ratelimit.exception.RequestBodyTooLargeException;
import com.brandPitara.sfs.ratelimit.model.RateLimitDecision;
import com.brandPitara.sfs.ratelimit.model.RateLimitErrorResponse;
import com.brandPitara.sfs.ratelimit.model.RateLimitRequestContext;
import com.brandPitara.sfs.ratelimit.model.RateLimitIdentity;
import com.brandPitara.sfs.ratelimit.metrics.RateLimitMetrics;
import com.brandPitara.sfs.ratelimit.resolver.ClientIpResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitIdentityResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitKeyResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitPolicyResolver;
import com.brandPitara.sfs.ratelimit.service.RateLimitService;
import com.brandPitara.sfs.util.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.EnumSet;
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
public class RateLimitingFilter extends OncePerRequestFilter {

    /**
     * Policies whose key material requires reading the JSON request body, PLUS
     * (PUBLIC_ANALYTICS_INGEST) policies that need the shared body-size bound for its
     * own sake even though nothing is extracted from the body for keying. Without this,
     * a public/permitAll JSON endpoint has no cap on request body size before Jackson's
     * @RequestBody binding fully parses and allocates it - see CachedBodyHttpServletRequest.
     */
    private static final Set<RateLimitPolicy> BODY_AWARE_POLICIES = EnumSet.of(
            RateLimitPolicy.MOBILE_OTP_REQUEST,
            RateLimitPolicy.MOBILE_OTP_VERIFY,
            RateLimitPolicy.MOBILE_TOKEN_REFRESH,
            RateLimitPolicy.MOBILE_GUEST_SESSION,
            RateLimitPolicy.PUBLIC_LOCATION_RESOLVE,
            // Only the calculator write policy needs a body fingerprint. Auth bodies
            // are wrapped to obtain the OTP phone where applicable, but refresh token,
            // installation ID and device ID are never used as cache identities.
            RateLimitPolicy.PUBLIC_CALCULATOR_WRITE,
            // Body-size bound only - no field is extracted from an analytics batch body.
            RateLimitPolicy.PUBLIC_ANALYTICS_INGEST
    );

    private final RateLimitPolicyResolver policyResolver;
    private final RateLimitKeyResolver keyResolver;
    private final ClientIpResolver clientIpResolver;
    private final RateLimitIdentityResolver identityResolver;
    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final RateLimitMetrics metrics;

    @Autowired
    public RateLimitingFilter(
            RateLimitPolicyResolver policyResolver,
            RateLimitKeyResolver keyResolver,
            ClientIpResolver clientIpResolver,
            RateLimitIdentityResolver identityResolver,
            RateLimitService rateLimitService,
            RateLimitProperties properties,
            ObjectMapper objectMapper,
            RateLimitMetrics metrics
    ) {
        this.policyResolver = policyResolver;
        this.keyResolver = keyResolver;
        this.clientIpResolver = clientIpResolver;
        this.identityResolver = identityResolver;
        this.rateLimitService = rateLimitService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    /** Compatibility constructor for isolated tests that do not run JwtRequestFilter. */
    public RateLimitingFilter(
            RateLimitPolicyResolver policyResolver,
            RateLimitKeyResolver keyResolver,
            ClientIpResolver clientIpResolver,
            RateLimitService rateLimitService,
            RateLimitProperties properties,
            ObjectMapper objectMapper,
            JwtTokenUtil jwtTokenUtil
    ) {
        this(policyResolver, keyResolver, clientIpResolver,
                new RateLimitIdentityResolver(clientIpResolver),
                rateLimitService, properties, objectMapper, RateLimitMetrics.isolated());
    }

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
                writePayloadTooLargeResponse(request, response, policy);
                return;
            }

            try {
                CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, maxBodyBytes);
                effectiveRequest = cachedRequest;
                body = tryParseJsonBody(cachedRequest);
            } catch (RequestBodyTooLargeException ex) {
                writePayloadTooLargeResponse(request, response, policy);
                return;
            }
        }

        RateLimitIdentity identity;
        RateLimitDecision decision;
        Map<RateLimitKeyType, String> resolvedKeys;
        try {
            identity = identityResolver.resolve(effectiveRequest);
            if (identity.fallback()) metrics.identityFallback(identity.type());
            RateLimitRequestContext context = buildContext(policy, effectiveRequest, body, identity);
            boolean abuseAlreadyConsumed = Boolean.TRUE.equals(request.getAttribute(
                    com.brandPitara.sfs.ratelimit.identity.RateLimitAuthenticationAttributes.IP_ABUSE_CONSUMED));
            Set<RateLimitKeyType> requiredKeyTypes = configuredKeyTypes(policy, !abuseAlreadyConsumed);
            resolvedKeys = keyResolver.resolveKeys(requiredKeyTypes, context);
            decision = rateLimitService.checkAndConsume(policy, resolvedKeys);
            metrics.decision(policy, identity.type(), decision.allowed());
        } catch (RuntimeException ex) {
            RateLimitFailureMode mode = failureMode(policy);
            metrics.enforcementFailure(policy, mode);
            log.error("Rate-limit enforcement failed: policy={} mode={} exceptionClass={}",
                    policy, mode, ex.getClass().getSimpleName());
            if (mode == RateLimitFailureMode.FAIL_OPEN) {
                chain.doFilter(effectiveRequest, response);
            } else {
                writeUnavailableResponse(effectiveRequest, response, policy);
            }
            return;
        }

        if (!decision.allowed()) {
            if (identity.type() == RateLimitIdentityType.INVALID_AUTH) {
                metrics.invalidAuthenticationRejected(policy);
            }
            writeBlockedResponse(effectiveRequest, response, policy, decision.retryAfterSeconds());
            return;
        }

        chain.doFilter(effectiveRequest, response);
    }

    private Set<RateLimitKeyType> configuredKeyTypes(RateLimitPolicy policy, boolean includeAbuse) {
        var config = properties.getPolicies().get(policy);
        if (config == null || config.getLimits() == null) {
            return Set.of();
        }
        Set<RateLimitKeyType> keyTypes = EnumSet.noneOf(RateLimitKeyType.class);
        config.getLimits().forEach(limit -> keyTypes.add(limit.getKeyType()));
        if (includeAbuse) keyTypes.add(RateLimitKeyType.IP_ABUSE);
        return keyTypes;
    }

    private RateLimitRequestContext buildContext(
            RateLimitPolicy policy,
            HttpServletRequest request,
            Map<String, Object> body,
            RateLimitIdentity identity
    ) {
        return RateLimitRequestContext.builder()
                .ip(identity.primaryKey())
                .phoneNumber(stringField(body, "phoneNumber"))
                .primaryIdentity(identity.primaryKey())
                .abuseIp(identity.abuseIp())
                .bodyFingerprint(policy == RateLimitPolicy.PUBLIC_CALCULATOR_WRITE ? canonicalBodyJson(body) : null)
                .build();
    }

    /**
     * Deterministic JSON representation of the parsed body (nested map keys
     * sorted recursively) so identical calculator requests always fingerprint
     * to the same value regardless of client-side key ordering. The result is
     * still the request's own field values, never logged directly; only the
     * SHA-256 digest RateLimitKeyResolver derives from it becomes key material.
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

    private void writePayloadTooLargeResponse(HttpServletRequest request, HttpServletResponse response,
                                              RateLimitPolicy policy)
            throws IOException {
        metrics.payloadRejected(policy);
        response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        RateLimitErrorResponse body = RateLimitErrorResponse.builder()
                .timestamp(OffsetDateTime.now().toString())
                .status(HttpStatus.CONTENT_TOO_LARGE.value())
                .error("PAYLOAD_TOO_LARGE")
                .message("Request body is too large.")
                .retryAfterSeconds(0)
                .policy(policy.name())
                .path(request.getRequestURI())
                .requestId(resolveRequestId(request))
                .build();

        objectMapper.writeValue(response.getWriter(), body);
    }

    private void writeBlockedResponse(HttpServletRequest request, HttpServletResponse response,
                                      RateLimitPolicy policy, long retryAfterSeconds)
            throws IOException {
        retryAfterSeconds = Math.max(1, retryAfterSeconds);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        RateLimitErrorResponse body = RateLimitErrorResponse.builder()
                .timestamp(OffsetDateTime.now().toString())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("TOO_MANY_REQUESTS")
                .message("Too many requests. Please try again later.")
                .retryAfterSeconds(retryAfterSeconds)
                .policy(policy.name())
                .path(request.getRequestURI())
                .requestId(resolveRequestId(request))
                .build();

        objectMapper.writeValue(response.getWriter(), body);
    }

    private void writeUnavailableResponse(HttpServletRequest request, HttpServletResponse response,
                                          RateLimitPolicy policy) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setHeader("Retry-After", "1");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        RateLimitErrorResponse body = RateLimitErrorResponse.builder()
                .timestamp(OffsetDateTime.now().toString())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("RATE_LIMIT_UNAVAILABLE")
                .message("Request protection is temporarily unavailable. Please try again.")
                .retryAfterSeconds(1)
                .policy(policy.name())
                .path(request.getRequestURI())
                .requestId(resolveRequestId(request))
                .build();
        objectMapper.writeValue(response.getWriter(), body);
    }

    private RateLimitFailureMode failureMode(RateLimitPolicy policy) {
        RateLimitProperties.PolicyConfig config = properties.getPolicies().get(policy);
        if (properties.getFailClosedPolicies().contains(policy)) return RateLimitFailureMode.FAIL_CLOSED;
        return config == null || config.getFailureMode() == null
                ? RateLimitFailureMode.FAIL_OPEN
                : config.getFailureMode();
    }

    private String resolveRequestId(HttpServletRequest request) {
        Object attribute = request.getAttribute(com.brandPitara.sfs.observability.LoggingConstants.ATTR_REQUEST_ID);
        if (attribute instanceof String value && !value.isBlank()) return value;
        String header = request.getHeader(com.brandPitara.sfs.observability.LoggingConstants.HEADER_REQUEST_ID);
        if (header != null && !header.isBlank()) return header.trim();
        return org.slf4j.MDC.get(com.brandPitara.sfs.observability.LoggingConstants.MDC_REQUEST_ID);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return com.brandPitara.sfs.config.SecurityRequestBypass.shouldBypass(request)
                || (path != null && (path.startsWith("/api/dashboard/") || path.startsWith("/api/admin/")));
    }
}
