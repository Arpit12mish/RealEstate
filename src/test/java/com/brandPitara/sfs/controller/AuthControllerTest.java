package com.brandPitara.sfs.controller;

import com.brandPitara.sfs.dto.LogoutRequest;
import com.brandPitara.sfs.dto.SendOtpRequest;
import com.brandPitara.sfs.dto.VerifyOtpRequest;
import com.brandPitara.sfs.entity.RefreshToken;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.enums.OnboardingStatus;
import com.brandPitara.sfs.enums.Role;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.service.AppUserDetailsService;
import com.brandPitara.sfs.service.GuestSessionService;
import com.brandPitara.sfs.service.LoginHistoryService;
import com.brandPitara.sfs.service.OnboardingService;
import com.brandPitara.sfs.service.OtpService;
import com.brandPitara.sfs.service.RefreshTokenService;
import com.brandPitara.sfs.service.UserService;
import com.brandPitara.sfs.service.model.OtpSendResult;
import com.brandPitara.sfs.service.model.OtpVerificationResult;
import com.brandPitara.sfs.service.model.UserLoginResult;
import com.brandPitara.sfs.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtTokenUtil jwtTokenUtil;
    @Mock
    private AppUserDetailsService userDetailsService;
    @Mock
    private OtpService otpService;
    @Mock
    private UserService userService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private LoginHistoryService loginHistoryService;
    @Mock
    private OnboardingService onboardingService;
    @Mock
    private GuestSessionService guestSessionService;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private LogSanitizer logSanitizer;

    @InjectMocks
    private AuthController authController;

    @Test
    void requestOtpReturnsStructuredSuccessContract() {
        SendOtpRequest request = new SendOtpRequest();
        request.setPhoneNumber("9876543210");

        when(otpService.sendOtp("9876543210")).thenReturn(
                OtpSendResult.builder()
                        .status("OTP_SENT")
                        .message("OTP sent successfully")
                        .resendAfterSeconds(30)
                        .expiresInSeconds(600)
                        .normalizedPhoneNumber("+919876543210")
                        .build()
        );
        when(logSanitizer.maskPhone("+919876543210")).thenReturn("98******10");

        ResponseEntity<?> response = authController.requestOtp(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("success")).isEqualTo(true);
        assertThat(body.get("status")).isEqualTo("OTP_SENT");
        assertThat(body.get("resendAfterSeconds")).isEqualTo(30L);
        assertThat(body.get("expiresInSeconds")).isEqualTo(600L);
        assertThat(body.get("maskedDestination")).isEqualTo("98******10");
    }

    @Test
    void resendOtpDelegatesToSameOtpServiceSendOtpAsRequestOtp() {
        SendOtpRequest request = new SendOtpRequest();
        request.setPhoneNumber("9876543210");

        when(otpService.sendOtp("9876543210")).thenReturn(
                OtpSendResult.builder()
                        .status("OTP_SENT")
                        .message("OTP sent successfully")
                        .resendAfterSeconds(30)
                        .expiresInSeconds(600)
                        .normalizedPhoneNumber("+919876543210")
                        .build()
        );
        when(logSanitizer.maskPhone("+919876543210")).thenReturn("98******10");

        ResponseEntity<?> response = authController.resendOtp(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("success")).isEqualTo(true);
        assertThat(body.get("maskedDestination")).isEqualTo("98******10");
        // Resend has no separate service method - it must call the exact same
        // sendOtp used by request-otp, which is what keeps rate limits/cooldown unified.
        verify(otpService).sendOtp("9876543210");
    }

    @Test
    void verifyOtpUsesNormalizedPhoneForUserCreationAndJwtClaims() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber("9876543210");
        request.setCode("111111");
        request.setDeviceId("device-1");
        request.setFcmToken("fcm-1");

        User user = new User();
        user.setId(42L);
        user.setPhoneNumber("+919876543210");
        user.setEmail("phone_919876543210@phone.local");
        user.setPassword("encoded");
        user.setRole(Role.CUSTOMER);
        user.setVerified(true);
        user.setOnboardingStatus(OnboardingStatus.ROLE_PENDING);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("+919876543210")
                .password("encoded")
                .roles("CUSTOMER")
                .build();

        when(otpService.verifyOtp("9876543210", "111111")).thenReturn(
                OtpVerificationResult.builder()
                        .approved(true)
                        .normalizedPhoneNumber("+919876543210")
                        .build()
        );
        when(userService.findOrCreateVerifiedUserByPhone("+919876543210"))
                .thenReturn(new UserLoginResult(user, true));
        when(userDetailsService.loadUserByUsername("+919876543210")).thenReturn(userDetails);
        when(jwtTokenUtil.generateToken(eq(userDetails), eq(42L), eq("+919876543210"), eq("CUSTOMER")))
                .thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user, "device-1", "fcm-1")).thenReturn("refresh-token");

        ResponseEntity<?> response = authController.verifyOtp(request, httpServletRequest);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("accessToken")).isEqualTo("access-token");
        assertThat(body.get("refreshToken")).isEqualTo("refresh-token");

        verify(userService).findOrCreateVerifiedUserByPhone("+919876543210");
        verify(userDetailsService).loadUserByUsername("+919876543210");
        verify(jwtTokenUtil).generateToken(userDetails, 42L, "+919876543210", "CUSTOMER");
        verify(guestSessionService).linkGuestSessionToUser("device-1", user);
    }

    @Test
    void verifyOtpFailureDoesNotCreateUser() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber("+919876543210");
        request.setCode("000000");

        when(otpService.verifyOtp("+919876543210", "000000")).thenReturn(
                OtpVerificationResult.builder()
                        .approved(false)
                        .normalizedPhoneNumber("+919876543210")
                        .build()
        );

        ResponseEntity<?> response = authController.verifyOtp(request, httpServletRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        org.mockito.Mockito.verify(userService, org.mockito.Mockito.never())
                .findOrCreateVerifiedUserByPhone(any());
    }

    @Test
    void verifyOtpConflictDoesNotCreateJwtOrAttachGuestSession() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber("9876543210");
        request.setCode("111111");
        request.setDeviceId("device-1");
        request.setFcmToken("fcm-1");

        when(otpService.verifyOtp("9876543210", "111111")).thenReturn(
                OtpVerificationResult.builder()
                        .approved(true)
                        .normalizedPhoneNumber("+919876543210")
                        .build()
        );
        when(userService.findOrCreateVerifiedUserByPhone("+919876543210"))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Multiple accounts found for this phone number. Please contact support."
                ));

        assertThatThrownBy(() -> authController.verifyOtp(request, httpServletRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(guestSessionService, never()).linkGuestSessionToUser(any(), any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtTokenUtil, never()).generateToken(any(), any(), any(), any());
        verify(refreshTokenService, never()).createRefreshToken(any(), any(), any());
    }

    @Test
    void logoutAllResolvesUserThroughVerifyForLogoutOnlyAndRevokesAllSessions() {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("raw-refresh-token");

        User user = new User();
        user.setId(42L);
        RefreshToken refreshToken = RefreshToken.builder().user(user).build();

        when(refreshTokenService.verifyForLogoutOnly("raw-refresh-token")).thenReturn(refreshToken);

        ResponseEntity<?> response = authController.logoutAll(request, httpServletRequest);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(refreshTokenService).verifyForLogoutOnly("raw-refresh-token");
        verify(refreshTokenService).revokeAllByUser(42L);
    }

    @Test
    void logoutAllReturns401ForInvalidRefreshToken() {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("bad-token");

        when(refreshTokenService.verifyForLogoutOnly("bad-token"))
                .thenThrow(new IllegalArgumentException("Invalid refresh token"));

        ResponseEntity<?> response = authController.logoutAll(request, httpServletRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        verify(refreshTokenService, never()).revokeAllByUser(any());
    }
}
