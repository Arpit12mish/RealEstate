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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DashboardAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger SECURITY_LOG =
            LoggerFactory.getLogger(LoggingConstants.LOGGER_SECURITY);

    private final ObjectMapper objectMapper;
    private final LogSanitizer logSanitizer;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        String requestId = resolveRequestId(request);
        String role      = resolveRole();

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event",  LogEvents.ACCESS_DENIED_ROLE);
        fields.put("path",   logSanitizer.sanitizePath(request.getRequestURI()));
        fields.put("status", 403);
        fields.put("role",   role);
        fields.put("message",   "Dashboard access denied — insufficient role");
        SECURITY_LOG.warn("{}", StructuredArguments.entries(fields));

        String message = cleanMessage(accessDeniedException.getMessage());

        DashboardApiErrorResponse body = DashboardApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.name(),
                "DASHBOARD_ACCESS_DENIED",
                message,
                request.getRequestURI(),
                request.getMethod(),
                requestId
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private String resolveRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "NONE";
        return auth.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("NONE");
    }

    private String cleanMessage(String message) {
        if (message == null || message.isBlank() || "Access Denied".equalsIgnoreCase(message.trim())) {
            return "You do not have permission to perform this dashboard action.";
        }
        return message.trim();
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
