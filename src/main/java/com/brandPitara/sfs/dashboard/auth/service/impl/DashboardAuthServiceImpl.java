package com.brandPitara.sfs.dashboard.auth.service.impl;

import com.brandPitara.sfs.dashboard.audit.service.DashboardLoginAuditService;
import com.brandPitara.sfs.dashboard.auth.dto.*;
import com.brandPitara.sfs.dashboard.auth.entity.DashboardRefreshTokenEntity;
import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetails;
import com.brandPitara.sfs.dashboard.auth.service.DashboardAuthService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardJwtService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardRefreshTokenService;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DashboardAuthServiceImpl implements DashboardAuthService {

    private final DashboardUserRepository dashboardUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final DashboardJwtService dashboardJwtService;
    private final DashboardRefreshTokenService refreshTokenService;
    private final DashboardLoginAuditService loginAuditService;

    @Override
    @Transactional
    public DashboardAuthResponse login(DashboardLoginRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.getEmail());

        try {
            DashboardUserEntity user = dashboardUserRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

            if (!Boolean.TRUE.equals(user.getActive())) {
                throw new BadCredentialsException("Dashboard user is disabled");
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new BadCredentialsException("Invalid email or password");
            }

            user.setLastLoginAt(OffsetDateTime.now());
            dashboardUserRepository.save(user);

            String accessToken = dashboardJwtService.generateAccessToken(user);
            String refreshToken = refreshTokenService.createRefreshToken(user);

            loginAuditService.recordSuccess(user, httpRequest);

            return buildAuthResponse(user, accessToken, refreshToken);

        } catch (BadCredentialsException ex) {
            loginAuditService.recordFailure(email, ex.getMessage(), httpRequest);
            throw ex;
        }
    }

    @Override
    @Transactional
    public DashboardAuthResponse refresh(DashboardRefreshRequest request) {
        DashboardRefreshTokenEntity oldToken =
                refreshTokenService.validateRefreshToken(request.getRefreshToken());

        DashboardUserEntity user = oldToken.getDashboardUser();

        String newRefreshToken = refreshTokenService.rotateRefreshToken(oldToken);
        String newAccessToken = dashboardJwtService.generateAccessToken(user);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(DashboardLogoutRequest request) {
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardUserResponse me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof DashboardUserDetails details)) {
            throw new BadCredentialsException("Dashboard authentication required");
        }

        return DashboardUserResponse.from(details.getUser());
    }

    private DashboardAuthResponse buildAuthResponse(
            DashboardUserEntity user,
            String accessToken,
            String refreshToken
    ) {
        return DashboardAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInMs(dashboardJwtService.getAccessExpirationMs())
                .user(DashboardUserResponse.from(user))
                .build();
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BadCredentialsException("Email is required");
        }

        return email.trim().toLowerCase();
    }
}