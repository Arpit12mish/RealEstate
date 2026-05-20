package com.brandPitara.sfs.observability;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Logs every completed API request to sfs-api.log.
 *
 * ── Filter execution order ────────────────────────────────────────────────
 *
 *   CorrelationIdFilter  (HIGHEST_PRECEDENCE = Integer.MIN_VALUE)
 *     → ApiRequestLoggingFilter (-200)   ← THIS FILTER
 *       → Spring Security FilterChainProxy (-100)
 *         → JwtRequestFilter / DashboardJwtAuthenticationFilter (inside security chain)
 *           → Controller  OR  sendError(401/403)
 *
 * Order = -200 places this filter BEFORE Spring Security FilterChainProxy
 * (-100), wrapping all of Spring Security's processing.  When Security calls
 * response.sendError(401/403) it returns normally to this filter's finally
 * block — so every request, including rejected ones, produces exactly one
 * api_request_completed log entry.
 *
 * ── Why the previous @Order(-50) approach missed 401/403 ─────────────────
 * FilterChainProxy runs its internal VirtualFilterChain and only calls the
 * outer servlet chain (which would reach a -50 filter) when authentication
 * succeeds.  On 401/403 it calls sendError() and returns WITHOUT forwarding —
 * so a filter at -50 never sees security rejections at all.
 *
 * ── Identity resolution ───────────────────────────────────────────────────
 * When this filter's finally block runs, SecurityContextHolderFilter (inside
 * Spring Security's chain) has already cleared the SecurityContext.  Identity
 * is read exclusively from MDC, populated by JwtRequestFilter /
 * DashboardJwtAuthenticationFilter on successful auth.  Unauthenticated
 * requests (401) log userId="anonymous", role="NONE".
 *
 * ── What is NOT logged ────────────────────────────────────────────────────
 *  • Request bodies
 *  • Authorization / Cookie headers
 *  • Sensitive query params (masked by LogSanitizer)
 *  • /actuator/health when status < 400
 *  • OPTIONS pre-flight requests
 *  • DispatcherType.ERROR re-dispatches (prevents duplicate entry when Tomcat
 *    internally forwards the original 4xx/5xx response to /error)
 */
@Component
@Order(-200)
@RequiredArgsConstructor
public class ApiRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger API_LOG = LoggerFactory.getLogger(LoggingConstants.LOGGER_API);

    private final LogSanitizer sanitizer;

    @Value("${sfs.logging.slow-api-threshold-ms:1500}")
    private long slowApiThresholdMs;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (request.getDispatcherType() == DispatcherType.ERROR)            return true;
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()))                return true;
        if (LoggingConstants.PATH_FAVICON.equals(request.getRequestURI())) return true;
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        long startNs = System.nanoTime();
        Throwable caughtException = null;

        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException ex) {
            caughtException = ex;
            throw ex;
        } finally {
            long durationMs = (System.nanoTime() - startNs) / 1_000_000;
            logRequest(request, response, durationMs, caughtException);
        }
    }

    private void logRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            long durationMs,
            Throwable exception
    ) {
        String path   = sanitizer.sanitizePath(request.getRequestURI());
        String method = request.getMethod();
        int    status = resolveStatus(response, exception);

        if (LoggingConstants.PATH_ACTUATOR_HEALTH.equals(path) && status < 400) return;

        String clientIp  = sanitizer.maskIp(resolveClientIp(request));
        String userAgent = sanitizer.simplifyUserAgent(request.getHeader("User-Agent"));
        String query     = sanitizer.sanitizeQueryString(request.getQueryString());

        String event = (exception != null || status >= 500)
                ? LogEvents.API_REQUEST_FAILED
                : LogEvents.API_REQUEST_COMPLETED;

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event",      event);
        fields.put("method",     method);
        fields.put("path",       path);
        if (query != null)         fields.put("query",      query);
        fields.put("status",     status);
        fields.put("durationMs", durationMs);
        fields.put("userId",     resolveUserId());
        fields.put("role",       resolveRole());
        fields.put("clientIp",   clientIp);
        fields.put("userAgent",  userAgent);
        if (exception != null) {
            fields.put("exceptionClass", exception.getClass().getSimpleName());
            fields.put("message", sanitizer.sanitizeMessage(exception.getMessage()));
        }

        if (status >= 500 || exception != null) {
            API_LOG.error("{}", StructuredArguments.entries(fields));
        } else if (status >= 400) {
            API_LOG.warn("{}", StructuredArguments.entries(fields));
        } else {
            API_LOG.info("{}", StructuredArguments.entries(fields));
        }

        if (durationMs >= slowApiThresholdMs) {
            Map<String, Object> slowFields = new LinkedHashMap<>();
            slowFields.put("event",      LogEvents.SLOW_API);
            slowFields.put("method",     method);
            slowFields.put("path",       path);
            slowFields.put("status",     status);
            slowFields.put("durationMs", durationMs);
            API_LOG.warn("{}", StructuredArguments.entries(slowFields));
        }
    }

    // ── Identity from MDC (SecurityContext already cleared at this point) ────

    private String resolveUserId() {
        String mdc = MDC.get(LoggingConstants.MDC_USER_ID);
        return (mdc != null && !mdc.isBlank()) ? mdc : "anonymous";
    }

    private String resolveRole() {
        String mdc = MDC.get(LoggingConstants.MDC_ROLE);
        return (mdc != null && !mdc.isBlank()) ? mdc : "NONE";
    }

    // ── Misc helpers ─────────────────────────────────────────────────────────

    private int resolveStatus(HttpServletResponse response, Throwable exception) {
        int status = response.getStatus();
        if (status == 0 || (status == 200 && exception != null)) {
            return exception != null ? 500 : 200;
        }
        return status;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
