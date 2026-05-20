package com.brandPitara.sfs.config;

import com.brandPitara.sfs.observability.LogEvents;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.observability.LoggingConstants;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger SECURITY_LOG =
            LoggerFactory.getLogger(LoggingConstants.LOGGER_SECURITY);

    private final LogSanitizer logSanitizer;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event",   LogEvents.AUTH_REQUIRED);
        fields.put("path",    logSanitizer.sanitizePath(request.getRequestURI()));
        fields.put("status",  401);
        fields.put("message", "Authentication required");
        SECURITY_LOG.warn("{}", StructuredArguments.entries(fields));

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}
