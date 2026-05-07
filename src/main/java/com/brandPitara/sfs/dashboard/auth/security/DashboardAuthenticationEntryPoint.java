package com.brandPitara.sfs.dashboard.auth.security;

import com.brandPitara.sfs.dashboard.common.response.DashboardApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class DashboardAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        DashboardApiErrorResponse body = DashboardApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.name(),
                "DASHBOARD_UNAUTHORIZED",
                "Dashboard authentication is required or the access token is invalid/expired.",
                request.getRequestURI(),
                request.getMethod(),
                resolveRequestId(request)
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
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