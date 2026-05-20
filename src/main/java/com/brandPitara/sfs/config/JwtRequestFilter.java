package com.brandPitara.sfs.config;

import com.brandPitara.sfs.observability.LogEvents;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.observability.LoggingConstants;
import com.brandPitara.sfs.util.JwtTokenUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger SECURITY_LOG =
            LoggerFactory.getLogger(LoggingConstants.LOGGER_SECURITY);

    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final LogSanitizer logSanitizer;

    public JwtRequestFilter(
            @Qualifier("appUserDetailsService") UserDetailsService userDetailsService,
            JwtTokenUtil jwtTokenUtil,
            LogSanitizer logSanitizer
    ) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.logSanitizer = logSanitizer;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/api/dashboard/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        String subject   = null;
        String jwtToken  = null;
        String jwtEvent  = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtToken = authHeader.substring(7);

            try {
                subject = jwtTokenUtil.getUsernameFromToken(jwtToken);
            } catch (ExpiredJwtException e) {
                jwtEvent = LogEvents.JWT_EXPIRED;
                logSecurityEvent(jwtEvent, request, "Access token expired");
            } catch (MalformedJwtException e) {
                jwtEvent = LogEvents.JWT_MALFORMED;
                logSecurityEvent(jwtEvent, request, "Malformed JWT token");
            } catch (SignatureException e) {
                jwtEvent = LogEvents.JWT_SIGNATURE_INVALID;
                logSecurityEvent(jwtEvent, request, "JWT signature validation failed");
            } catch (UnsupportedJwtException e) {
                jwtEvent = LogEvents.JWT_INVALID;
                logSecurityEvent(jwtEvent, request, "Unsupported JWT token");
            } catch (IllegalArgumentException e) {
                jwtEvent = LogEvents.JWT_INVALID;
                logSecurityEvent(jwtEvent, request, "Invalid JWT token");
            } catch (Exception e) {
                jwtEvent = LogEvents.JWT_AUTH_FAILED;
                logSecurityEvent(jwtEvent, request, "JWT processing failed");
            }
        }

        if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String principalType = null;

            try {
                principalType = jwtTokenUtil.getPrincipalTypeFromToken(jwtToken);
            } catch (Exception e) {
                logger.error("Unable to read principalType from JWT");
            }

            if ("USER".equalsIgnoreCase(principalType)) {
                authenticateAppUser(request, jwtToken, subject);
            } else if ("GUEST".equalsIgnoreCase(principalType)) {
                authenticateGuestUser(request, jwtToken);
            }
        }

        chain.doFilter(request, response);
    }

    private void authenticateAppUser(
            HttpServletRequest request,
            String jwtToken,
            String subject
    ) {
        try {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(subject);

            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // Populate MDC so ApiRequestLoggingFilter can read after SecurityContext is cleared
                try {
                    Long userId = jwtTokenUtil.getUserIdFromToken(jwtToken);
                    String role = jwtTokenUtil.getRoleFromToken(jwtToken);
                    if (userId != null) MDC.put(LoggingConstants.MDC_USER_ID, userId.toString());
                    if (role   != null) MDC.put(LoggingConstants.MDC_ROLE, role);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            logger.error("Unable to authenticate app user JWT");
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticateGuestUser(
            HttpServletRequest request,
            String jwtToken
    ) {
        String installationId = null;

        try {
            installationId = jwtTokenUtil.getInstallationIdFromToken(jwtToken);
        } catch (Exception e) {
            logger.error("Unable to read installationId from guest JWT");
        }

        if (installationId != null && jwtTokenUtil.validateGuestToken(jwtToken, installationId)) {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            installationId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST"))
                    );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            MDC.put(LoggingConstants.MDC_USER_ID, "guest:" + installationId.substring(0, Math.min(8, installationId.length())));
            MDC.put(LoggingConstants.MDC_ROLE, "GUEST");
        }
    }

    private void logSecurityEvent(String event, HttpServletRequest request, String message) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event",   event);
        fields.put("path",    logSanitizer.sanitizePath(request.getRequestURI()));
        fields.put("message", message);
        SECURITY_LOG.warn("{}", StructuredArguments.entries(fields));
    }
}
