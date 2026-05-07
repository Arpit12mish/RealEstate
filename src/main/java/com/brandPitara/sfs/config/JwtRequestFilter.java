package com.brandPitara.sfs.config;

import com.brandPitara.sfs.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;

    public JwtRequestFilter(
            @Qualifier("appUserDetailsService") UserDetailsService userDetailsService,
            JwtTokenUtil jwtTokenUtil
    ) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        /*
         * Important:
         * This filter is only for mobile app USER/GUEST tokens.
         * Dashboard APIs will use DashboardJwtAuthenticationFilter separately.
         */
        return path != null && path.startsWith("/api/dashboard/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String subject = null;
        String jwtToken = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);

            try {
                subject = jwtTokenUtil.getUsernameFromToken(jwtToken);
            } catch (IllegalArgumentException e) {
                logger.error("Unable to get JWT Token");
            } catch (Exception e) {
                logger.error("Invalid JWT Token");
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
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
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
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            installationId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST"))
                    );

            authenticationToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
    }
}