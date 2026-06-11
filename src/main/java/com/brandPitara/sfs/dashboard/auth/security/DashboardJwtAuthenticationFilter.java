package com.brandPitara.sfs.dashboard.auth.security;

import com.brandPitara.sfs.dashboard.auth.service.DashboardJwtService;
import com.brandPitara.sfs.observability.LogEvents;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.observability.LoggingConstants;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DashboardJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger SECURITY_LOG =
            LoggerFactory.getLogger(LoggingConstants.LOGGER_SECURITY);

    private final DashboardJwtService dashboardJwtService;
    private final DashboardUserDetailsService dashboardUserDetailsService;
    private final LogSanitizer logSanitizer;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) return true;
        return !path.startsWith("/api/dashboard/") && !path.startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveBearerToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String email = dashboardJwtService.getSubject(token);
                UserDetails userDetails = dashboardUserDetailsService.loadUserByUsername(email);

                if (userDetails instanceof DashboardUserDetails dashboardUserDetails
                        && dashboardJwtService.validateAccessToken(token, dashboardUserDetails)) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    dashboardUserDetails,
                                    null,
                                    dashboardUserDetails.getAuthorities()
                            );
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Populate MDC for ApiRequestLoggingFilter
                    MDC.put(LoggingConstants.MDC_USER_ID, String.valueOf(dashboardUserDetails.getUser().getId()));
                    MDC.put(LoggingConstants.MDC_ROLE, "DASHBOARD_" + dashboardUserDetails.getUser().getRole().name());
                }

            } catch (ExpiredJwtException ex) {
                SecurityContextHolder.clearContext();
                logSecurityEvent(LogEvents.DASHBOARD_JWT_EXPIRED, request, "Dashboard access token expired");
            } catch (MalformedJwtException | UnsupportedJwtException | SignatureException ex) {
                SecurityContextHolder.clearContext();
                logSecurityEvent(LogEvents.DASHBOARD_JWT_INVALID, request, "Dashboard JWT is invalid");
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                logSecurityEvent(LogEvents.DASHBOARD_JWT_INVALID, request, "Dashboard JWT processing failed");
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }

    private void logSecurityEvent(String event, HttpServletRequest request, String message) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event",   event);
        fields.put("path",    logSanitizer.sanitizePath(request.getRequestURI()));
        fields.put("message", message);
        SECURITY_LOG.warn("{}", StructuredArguments.entries(fields));
    }
}
