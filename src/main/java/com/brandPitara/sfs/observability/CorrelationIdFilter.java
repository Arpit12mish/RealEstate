package com.brandPitara.sfs.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Runs first (HIGHEST_PRECEDENCE) on every request.
 *
 * Responsibilities:
 * - Reuse an incoming X-Request-Id if it is safe (alphanumeric, -, _, max 80 chars).
 * - Generate a new UUID-based ID otherwise.
 * - Put requestId into MDC so every subsequent log statement includes it.
 * - Write the final requestId into the response header X-Request-Id.
 * - Set a request attribute so downstream code (exception handlers, etc.) can read it.
 * - Clear MDC in the finally block — this is the outermost filter, so MDC is always clean.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class CorrelationIdFilter extends OncePerRequestFilter {

    private final LogSanitizer sanitizer;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String requestId = resolveRequestId(request);

        MDC.put(LoggingConstants.MDC_REQUEST_ID, requestId);
        request.setAttribute(LoggingConstants.ATTR_REQUEST_ID, requestId);
        response.setHeader(LoggingConstants.HEADER_REQUEST_ID, requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(LoggingConstants.HEADER_REQUEST_ID);
        if (incoming != null && sanitizer.isSafeCorrelationId(incoming)) {
            return incoming;
        }
        return "req-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
