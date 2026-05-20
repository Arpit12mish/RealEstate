package com.brandPitara.sfs.dashboard.auth.security;

import com.brandPitara.sfs.dashboard.common.response.DashboardApiErrorResponse;
import com.brandPitara.sfs.observability.LogEvents;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.observability.LoggingConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DashboardAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger SECURITY_LOG =
            LoggerFactory.getLogger(LoggingConstants.LOGGER_SECURITY);

    private final ObjectMapper objectMapper;
    private final LogSanitizer logSanitizer;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        String requestId = resolveRequestId(request);

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event",   LogEvents.AUTH_REQUIRED);
        fields.put("path",    logSanitizer.sanitizePath(request.getRequestURI()));
        fields.put("status",  401);
        fields.put("message", "Dashboard authentication required or token is invalid/expired");
        SECURITY_LOG.warn("{}", StructuredArguments.entries(fields));

        DashboardApiErrorResponse body = DashboardApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.name(),
                "DASHBOARD_UNAUTHORIZED",
                "Dashboard authentication is required or the access token is invalid/expired.",
                request.getRequestURI(),
                request.getMethod(),
                requestId
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private String resolveRequestId(HttpServletRequest request) {
        Object attr = request.getAttribute(LoggingConstants.ATTR_REQUEST_ID);
        if (attr instanceof String id && !id.isBlank()) return id;

        String header = request.getHeader(LoggingConstants.HEADER_REQUEST_ID);
        if (header != null && !header.isBlank()) return header.trim();

        String mdc = MDC.get(LoggingConstants.MDC_REQUEST_ID);
        return (mdc != null && !mdc.isBlank()) ? mdc : null;
    }
}
