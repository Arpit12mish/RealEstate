package com.brandPitara.sfs.ratelimit.filter;

import com.brandPitara.sfs.config.SecurityRequestBypass;
import com.brandPitara.sfs.observability.LoggingConstants;
import com.brandPitara.sfs.ratelimit.config.RateLimitProperties;
import com.brandPitara.sfs.ratelimit.enums.RateLimitFailureMode;
import com.brandPitara.sfs.ratelimit.enums.RateLimitIdentityType;
import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.identity.RateLimitAuthenticationAttributes;
import com.brandPitara.sfs.ratelimit.metrics.RateLimitMetrics;
import com.brandPitara.sfs.ratelimit.model.RateLimitDecision;
import com.brandPitara.sfs.ratelimit.model.RateLimitErrorResponse;
import com.brandPitara.sfs.ratelimit.resolver.ClientIpResolver;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitPolicyResolver;
import com.brandPitara.sfs.ratelimit.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Optional;

/** Cheap canonical-IP abuse protection which executes before any JWT parsing. */
@Component
@Slf4j
public class PreAuthenticationAbuseFilter extends OncePerRequestFilter {
    private final RateLimitPolicyResolver policyResolver;
    private final ClientIpResolver clientIpResolver;
    private final RateLimitService service;
    private final RateLimitProperties properties;
    private final RateLimitMetrics metrics;
    private final ObjectMapper objectMapper;

    public PreAuthenticationAbuseFilter(RateLimitPolicyResolver policyResolver,
                                        ClientIpResolver clientIpResolver,
                                        RateLimitService service,
                                        RateLimitProperties properties,
                                        RateLimitMetrics metrics,
                                        ObjectMapper objectMapper) {
        this.policyResolver = policyResolver;
        this.clientIpResolver = clientIpResolver;
        this.service = service;
        this.properties = properties;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SecurityRequestBypass.shouldBypass(request)
                || request.getRequestURI().startsWith("/api/dashboard/")
                || request.getRequestURI().startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (!properties.isEnabled() || authorization == null || authorization.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        Optional<RateLimitPolicy> match = policyResolver.resolve(request);
        if (match.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        RateLimitPolicy policy = match.get();
        try {
            RateLimitDecision decision = service.checkAndConsumeAbuse(policy, clientIpResolver.resolve(request));
            metrics.decision(policy, RateLimitIdentityType.INVALID_AUTH, decision.allowed());
            if (!decision.allowed()) {
                metrics.invalidAuthenticationRejected(policy);
                writeBlocked(request, response, policy, decision.retryAfterSeconds());
                return;
            }
            request.setAttribute(RateLimitAuthenticationAttributes.IP_ABUSE_CONSUMED, Boolean.TRUE);
            chain.doFilter(request, response);
        } catch (RuntimeException ex) {
            RateLimitFailureMode mode = failureMode(policy);
            metrics.enforcementFailure(policy, mode);
            log.error("Pre-auth rate-limit enforcement failed: policy={} mode={} exceptionClass={}",
                    policy, mode, ex.getClass().getSimpleName());
            if (mode == RateLimitFailureMode.FAIL_OPEN) chain.doFilter(request, response);
            else writeUnavailable(request, response, policy);
        }
    }

    private RateLimitFailureMode failureMode(RateLimitPolicy policy) {
        var config = properties.getPolicies().get(policy);
        if (properties.getFailClosedPolicies().contains(policy)) return RateLimitFailureMode.FAIL_CLOSED;
        return config == null || config.getFailureMode() == null
                ? RateLimitFailureMode.FAIL_OPEN : config.getFailureMode();
    }

    private void writeBlocked(HttpServletRequest request, HttpServletResponse response,
                              RateLimitPolicy policy, long retryAfter) throws IOException {
        long seconds = Math.max(1, retryAfter);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", Long.toString(seconds));
        write(request, response, policy, HttpStatus.TOO_MANY_REQUESTS,
                "TOO_MANY_REQUESTS", "Too many requests. Please try again later.", seconds);
    }

    private void writeUnavailable(HttpServletRequest request, HttpServletResponse response,
                                  RateLimitPolicy policy) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setHeader("Retry-After", "1");
        write(request, response, policy, HttpStatus.SERVICE_UNAVAILABLE,
                "RATE_LIMIT_UNAVAILABLE", "Request protection is temporarily unavailable. Please try again.", 1);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, RateLimitPolicy policy,
                       HttpStatus status, String error, String message, long retryAfter) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Object requestId = request.getAttribute(LoggingConstants.ATTR_REQUEST_ID);
        objectMapper.writeValue(response.getWriter(), RateLimitErrorResponse.builder()
                .timestamp(OffsetDateTime.now().toString()).status(status.value()).error(error).message(message)
                .retryAfterSeconds(retryAfter).policy(policy.name()).path(request.getRequestURI())
                .requestId(requestId instanceof String value ? value : request.getHeader(LoggingConstants.HEADER_REQUEST_ID))
                .build());
    }
}
