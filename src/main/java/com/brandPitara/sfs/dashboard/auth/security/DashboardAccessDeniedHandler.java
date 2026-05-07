package com.brandPitara.sfs.dashboard.auth.security;

import com.brandPitara.sfs.dashboard.common.response.DashboardApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class DashboardAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        DashboardApiErrorResponse body = DashboardApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.name(),
                "DASHBOARD_ACCESS_DENIED",
                cleanMessage(
                        accessDeniedException.getMessage(),
                        "You do not have permission to perform this dashboard action."
                ),
                request.getRequestURI(),
                request.getMethod(),
                resolveRequestId(request)
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private String cleanMessage(String message, String fallback) {
        if (message == null || message.isBlank() || "Access Denied".equalsIgnoreCase(message.trim())) {
            return fallback;
        }

        return message.trim();
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");

        if (requestId != null && !requestId.isBlank()) {
            return requestId.trim();
        }

        requestId = request.getHeader("X-Correlation-Id");

        if (requestId != null && !requestId.isBlank()) {
            return requestId.trim();
        }

        return null;
    }
}