package com.brandPitara.sfs.dashboard.auth.service.impl;

import com.brandPitara.sfs.dashboard.audit.service.DashboardLoginAuditService;
import com.brandPitara.sfs.dashboard.auth.dto.DashboardLoginRequest;
import com.brandPitara.sfs.dashboard.auth.dto.DashboardLogoutRequest;
import com.brandPitara.sfs.dashboard.auth.dto.DashboardRefreshRequest;
import com.brandPitara.sfs.dashboard.auth.entity.DashboardRefreshTokenEntity;
import com.brandPitara.sfs.dashboard.auth.security.DashboardUserDetails;
import com.brandPitara.sfs.dashboard.auth.service.DashboardJwtService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardRefreshTokenService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Locks in the dashboard authentication invariant made explicit on
 * DashboardUserDetails#getPassword: real credential verification happens only
 * here (via PasswordEncoder against the stored hash), never through Spring
 * Security's AuthenticationManager - there is no such bean wired to a dashboard
 * UserDetailsService in this application.
 */
@ExtendWith(MockitoExtension.class)
class DashboardAuthServiceImplTest {

    @Mock
    private DashboardUserRepository dashboardUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private DashboardJwtService dashboardJwtService;
    @Mock
    private DashboardRefreshTokenService refreshTokenService;
    @Mock
    private DashboardLoginAuditService loginAuditService;
    @Mock
    private HttpServletRequest httpRequest;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private DashboardAuthServiceImpl newService() {
        return new DashboardAuthServiceImpl(
                dashboardUserRepository,
                passwordEncoder,
                dashboardJwtService,
                refreshTokenService,
                loginAuditService
        );
    }

    private DashboardUserEntity activeUser(String email, String passwordHash) {
        DashboardUserEntity user = new DashboardUserEntity();
        user.setId(1L);
        user.setEmail(email);
        user.setName("Test Admin");
        user.setRole(DashboardRole.ADMIN);
        user.setActive(true);
        user.setPasswordHash(passwordHash);
        return user;
    }

    @Test
    void validLoginReturnsTokensAndRecordsSuccessAudit() {
        DashboardAuthServiceImpl authService = newService();
        DashboardUserEntity user = activeUser("admin@example.com", "hashed-password");
        DashboardLoginRequest request = new DashboardLoginRequest();
        request.setEmail("admin@example.com");
        request.setPassword("correct-password");

        when(dashboardUserRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(dashboardJwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        var response = authService.login(request, httpRequest);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser().getPermissions())
                .containsExactlyInAnyOrder(com.brandPitara.sfs.dashboard.common.enums.DashboardPermission.values());
        assertThat(response.getUser().getPermissionProfiles())
                .containsExactlyInAnyOrder(
                        com.brandPitara.sfs.cms.security.CmsPermissionProfile.EDITOR,
                        com.brandPitara.sfs.cms.security.CmsPermissionProfile.PUBLISHER
                );
        verify(loginAuditService).recordSuccess(user, httpRequest);
        verify(loginAuditService, never()).recordFailure(anyString(), anyString(), any());
    }

    @Test
    void wrongPasswordFailsAndRecordsFailureAuditWithoutIssuingTokens() {
        DashboardAuthServiceImpl authService = newService();
        DashboardUserEntity user = activeUser("admin@example.com", "hashed-password");
        DashboardLoginRequest request = new DashboardLoginRequest();
        request.setEmail("admin@example.com");
        request.setPassword("wrong-password");

        when(dashboardUserRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginAuditService).recordFailure(eq("admin@example.com"), anyString(), eq(httpRequest));
        verify(dashboardJwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void nonExistentEmailFailsWithoutLeakingWhetherTheAccountExists() {
        DashboardAuthServiceImpl authService = newService();
        DashboardLoginRequest request = new DashboardLoginRequest();
        request.setEmail("nobody@example.com");
        request.setPassword("whatever");

        when(dashboardUserRepository.findByEmailIgnoreCase("nobody@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(loginAuditService).recordFailure(eq("nobody@example.com"), anyString(), eq(httpRequest));
    }

    @Test
    void disabledUserFailsEvenWithCorrectPassword() {
        DashboardAuthServiceImpl authService = newService();
        DashboardUserEntity user = activeUser("admin@example.com", "hashed-password");
        user.setActive(false);
        DashboardLoginRequest request = new DashboardLoginRequest();
        request.setEmail("admin@example.com");
        request.setPassword("correct-password");

        when(dashboardUserRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Dashboard user is disabled");

        // A disabled account must fail before the password is even compared.
        verify(passwordEncoder, never()).matches(any(), any());
        verify(dashboardJwtService, never()).generateAccessToken(any());
    }

    @Test
    void refreshRotatesTokenAndIssuesNewAccessToken() {
        DashboardAuthServiceImpl authService = newService();
        DashboardUserEntity user = activeUser("admin@example.com", "hashed-password");
        DashboardRefreshTokenEntity oldToken = DashboardRefreshTokenEntity.builder()
                .dashboardUser(user)
                .tokenHash("old-hash")
                .revoked(false)
                .build();
        DashboardRefreshRequest request = new DashboardRefreshRequest();
        request.setRefreshToken("raw-refresh-token");

        when(refreshTokenService.validateRefreshToken("raw-refresh-token")).thenReturn(oldToken);
        when(refreshTokenService.rotateRefreshToken(oldToken)).thenReturn("new-refresh-token");
        when(dashboardJwtService.generateAccessToken(user)).thenReturn("new-access-token");

        var response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void logoutRevokesTheSuppliedRefreshToken() {
        DashboardAuthServiceImpl authService = newService();
        DashboardLogoutRequest request = new DashboardLogoutRequest();
        request.setRefreshToken("raw-refresh-token");

        authService.logout(request);

        verify(refreshTokenService).revokeRefreshToken("raw-refresh-token");
    }

    @Test
    void meReturnsTheAuthenticatedPrincipalFromTheSecurityContext() {
        DashboardAuthServiceImpl authService = newService();
        DashboardUserEntity user = activeUser("admin@example.com", "hashed-password");
        DashboardUserDetails principal = new DashboardUserDetails(user);
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(principal, null));

        var response = authService.me();

        assertThat(response.getEmail()).isEqualTo("admin@example.com");
    }

    @Test
    void meFailsWhenNoDashboardPrincipalIsAuthenticated() {
        DashboardAuthServiceImpl authService = newService();
        SecurityContextHolder.clearContext();

        assertThatThrownBy(authService::me).isInstanceOf(BadCredentialsException.class);
    }
}
