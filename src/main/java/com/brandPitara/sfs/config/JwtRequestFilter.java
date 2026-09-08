package com.brandPitara.sfs.config;

import com.brandPitara.sfs.observability.LogEvents;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.observability.LoggingConstants;
import com.brandPitara.sfs.ratelimit.identity.RateLimitAuthenticationAttributes;
import com.brandPitara.sfs.security.identity.MobileAuthenticationIdentityCache;
import com.brandPitara.sfs.security.identity.MobileAuthenticationUserSnapshot;
import com.brandPitara.sfs.util.JwtTokenUtil;
import com.brandPitara.sfs.util.ValidatedJwtClaims;
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
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger SECURITY_LOG = LoggerFactory.getLogger(LoggingConstants.LOGGER_SECURITY);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER = "USER";
    private static final String GUEST = "GUEST";
    private static final String GUEST_PRINCIPAL = "guest";

    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final LogSanitizer logSanitizer;
    private final MobileAuthenticationIdentityCache identityCache;

    @Autowired
    public JwtRequestFilter(
            @Qualifier("appUserDetailsService") UserDetailsService userDetailsService,
            JwtTokenUtil jwtTokenUtil,
            LogSanitizer logSanitizer,
            MobileAuthenticationIdentityCache identityCache
    ) {
        this.userDetailsService = Objects.requireNonNull(userDetailsService);
        this.jwtTokenUtil = Objects.requireNonNull(jwtTokenUtil);
        this.logSanitizer = Objects.requireNonNull(logSanitizer);
        this.identityCache = identityCache;
    }

    JwtRequestFilter(UserDetailsService userDetailsService, JwtTokenUtil jwtTokenUtil, LogSanitizer logSanitizer) {
        this.userDetailsService = Objects.requireNonNull(userDetailsService);
        this.jwtTokenUtil = Objects.requireNonNull(jwtTokenUtil);
        this.logSanitizer = Objects.requireNonNull(logSanitizer);
        this.identityCache = null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SecurityRequestBypass.shouldBypass(request)
                || DeterministicPublicRequestMatcher.matches(request)
                || (path != null && (path.startsWith("/api/dashboard/") || path.startsWith("/api/admin/")));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        clearAuthenticationAttributes(request);
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        if (!header.startsWith(BEARER_PREFIX) || header.length() == BEARER_PREFIX.length()) {
            invalidate(request, LogEvents.JWT_INVALID, "Unsupported or empty authorization header");
            chain.doFilter(request, response);
            return;
        }

        ValidatedJwtClaims claims = parseOnce(header.substring(BEARER_PREFIX.length()), request);
        if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            boolean authenticated = USER.equalsIgnoreCase(claims.principalType())
                    ? authenticateUser(request, claims)
                    : GUEST.equalsIgnoreCase(claims.principalType()) && authenticateGuest(request, claims);
            if (!authenticated) markInvalidAuthentication(request);
        }
        chain.doFilter(request, response);
    }

    private ValidatedJwtClaims parseOnce(String token, HttpServletRequest request) {
        try {
            return jwtTokenUtil.parseAndValidate(token);
        } catch (ExpiredJwtException ex) {
            invalidate(request, LogEvents.JWT_EXPIRED, "Access token expired");
        } catch (MalformedJwtException ex) {
            invalidate(request, LogEvents.JWT_MALFORMED, "Malformed JWT token");
        } catch (SignatureException ex) {
            invalidate(request, LogEvents.JWT_SIGNATURE_INVALID, "JWT signature validation failed");
        } catch (UnsupportedJwtException | IllegalArgumentException ex) {
            invalidate(request, LogEvents.JWT_INVALID, "Invalid JWT token");
        } catch (Exception ex) {
            invalidate(request, LogEvents.JWT_AUTH_FAILED, "JWT processing failed");
        }
        return null;
    }

    private boolean authenticateUser(HttpServletRequest request, ValidatedJwtClaims claims) {
        try {
            UserDetails loaded = claims.userId() != null && identityCache != null
                    ? identityCache.get(claims.userId())
                    : userDetailsService.loadUserByUsername(claims.subject());
            if (!(loaded instanceof MobileAuthenticationUserSnapshot snapshot)
                    || !snapshot.enabled()
                    || snapshot.userId() == null
                    || !claims.subject().equals(snapshot.username())) {
                return false;
            }
            if (claims.userId() != null && !claims.userId().equals(snapshot.userId())) {
                logSecurityEvent(LogEvents.JWT_AUTH_FAILED, request, "JWT identity claims were inconsistent");
                return false;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    snapshot, null, snapshot.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute(RateLimitAuthenticationAttributes.AUTH_STATE,
                    RateLimitAuthenticationAttributes.STATE_USER);
            request.setAttribute(RateLimitAuthenticationAttributes.USER_ID, snapshot.userId());
            MDC.put(LoggingConstants.MDC_USER_ID, snapshot.userId().toString());
            snapshot.getAuthorities().stream().findFirst().ifPresent(authority ->
                    MDC.put(LoggingConstants.MDC_ROLE, authority.getAuthority().replaceFirst("^ROLE_", "")));
            return true;
        } catch (Exception ex) {
            logSecurityEvent(LogEvents.JWT_AUTH_FAILED, request, "Unable to authenticate app user JWT");
            return false;
        }
    }

    private boolean authenticateGuest(HttpServletRequest request, ValidatedJwtClaims claims) {
        if (claims.guestSessionId() == null || claims.guestSessionId() <= 0
                || claims.installationId() == null || claims.installationId().isBlank()
                || !claims.subject().equals(claims.installationId())) {
            return false;
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                GUEST_PRINCIPAL, null, List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute(RateLimitAuthenticationAttributes.AUTH_STATE,
                RateLimitAuthenticationAttributes.STATE_GUEST);
        request.setAttribute(RateLimitAuthenticationAttributes.GUEST_SESSION_ID, claims.guestSessionId());
        MDC.remove(LoggingConstants.MDC_USER_ID);
        MDC.put(LoggingConstants.MDC_ROLE, GUEST);
        return true;
    }

    private void invalidate(HttpServletRequest request, String event, String message) {
        markInvalidAuthentication(request);
        logSecurityEvent(event, request, message);
    }

    private void clearAuthenticationAttributes(HttpServletRequest request) {
        request.removeAttribute(RateLimitAuthenticationAttributes.AUTH_STATE);
        request.removeAttribute(RateLimitAuthenticationAttributes.USER_ID);
        request.removeAttribute(RateLimitAuthenticationAttributes.GUEST_SESSION_ID);
    }

    private void markInvalidAuthentication(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        clearAuthenticationAttributes(request);
        request.setAttribute(RateLimitAuthenticationAttributes.AUTH_STATE,
                RateLimitAuthenticationAttributes.STATE_INVALID);
        MDC.remove(LoggingConstants.MDC_USER_ID);
        MDC.remove(LoggingConstants.MDC_ROLE);
    }

    private void logSecurityEvent(String event, HttpServletRequest request, String message) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event", event);
        fields.put("path", logSanitizer.sanitizePath(request.getRequestURI()));
        fields.put("message", message);
        SECURITY_LOG.warn("{}", StructuredArguments.entries(fields));
    }
}
