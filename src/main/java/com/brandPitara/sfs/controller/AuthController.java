package com.brandPitara.sfs.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.brandPitara.sfs.dto.AuthResponse;
import com.brandPitara.sfs.dto.LogoutRequest;
import com.brandPitara.sfs.dto.RefreshRequest;
import com.brandPitara.sfs.dto.VerifyOtpRequest;
import com.brandPitara.sfs.dto.SendOtpRequest;
import com.brandPitara.sfs.entity.RefreshToken;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.service.AppUserDetailsService;
import com.brandPitara.sfs.service.GuestSessionService;
import com.brandPitara.sfs.service.LoginHistoryService;
import com.brandPitara.sfs.service.OnboardingService;
import com.brandPitara.sfs.service.OtpService;
import com.brandPitara.sfs.service.RefreshTokenService;
import com.brandPitara.sfs.service.UserService;
import com.brandPitara.sfs.service.model.OtpVerificationResult;
import com.brandPitara.sfs.service.model.RefreshTokenRotationResult;
import com.brandPitara.sfs.service.model.UserLoginResult;
import com.brandPitara.sfs.util.JwtTokenUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenUtil jwtTokenUtil;
    private final AppUserDetailsService userDetailsService;
    private final OtpService otpService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final LoginHistoryService loginHistoryService;
    private final OnboardingService onboardingService ;
    private final GuestSessionService guestSessionService;

    // 1️⃣ Request OTP  TODO: add rate limiting per phone/IP here
    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@Valid @RequestBody SendOtpRequest request) {

        var result = otpService.sendOtp(request.getPhoneNumber());

        return ResponseEntity.ok(Map.of(
                "status", result.getStatus(),
                "message", result.getMessage(),
                "resendAfterSeconds", result.getResendAfterSeconds()
        ));
    }

    // 2️⃣ Verify OTP => issue access + refresh tokens
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request, HttpServletRequest httpRequest) {

        log.debug("verify-otp requested for deviceId={}", request.getDeviceId());
        OtpVerificationResult verification = otpService.verifyOtp(request.getPhoneNumber(), request.getCode());

        if (!verification.isApproved()) {
            loginHistoryService.recordLogin(null, "OTP", false, request.getDeviceId(),
                    request.getFcmToken(), httpRequest);

            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_OTP"));
        }

        // Find or create user
        UserLoginResult loginResult = userService.findOrCreateVerifiedUserByPhone(verification.getNormalizedPhoneNumber());

        User user = loginResult.getUser();
        boolean isNewUser = loginResult.isNewUser();
        guestSessionService.linkGuestSessionToUser(request.getDeviceId(), user);

        loginHistoryService.recordLogin(user, "OTP", true, request.getDeviceId(), request.getFcmToken(), httpRequest);

        // Load UserDetails (by email)
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getPhoneNumber());

        // Access token
        String accessToken = jwtTokenUtil.generateToken(
                userDetails,
                user.getId(),
                user.getPhoneNumber(),
                user.getRole().name()
        );

        // Refresh token — raw value returned to client; SHA-256 hash is stored in DB
        String refreshToken =
                refreshTokenService.createRefreshToken(user, request.getDeviceId(), request.getFcmToken());

        AuthResponse response = AuthResponse.builder()
                .token(accessToken)
                .userId(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .verified(user.isVerified())
                .build();

        Map<String, Object> payload = new HashMap<>();
        payload.put("accessToken", accessToken);
        payload.put("refreshToken", refreshToken);
        payload.put("user", response);
        payload.put("isNewUser", isNewUser); // or track separately if needed
        // add onboarding payload so frontend can route
        payload.put("onboardingStatus", user.getOnboardingStatus().name());
        payload.put("role", user.getRole().name());
        payload.put("session", onboardingService.getSession(user.getId()));

        return ResponseEntity.ok(payload);
    }

    // 3️⃣ Refresh token -> new access token
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {

        RefreshTokenRotationResult rotation;
        try {
            rotation = refreshTokenService.rotateRefreshToken(request.getRefreshToken());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", "INVALID_REFRESH_TOKEN"));
        }

        User user = rotation.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getPhoneNumber());

        loginHistoryService.recordLogin(
            user, "REFRESH", true, rotation.getDeviceId(), rotation.getFcmToken(), httpRequest
        );

        String newAccessToken = jwtTokenUtil.generateToken(
                userDetails,
                user.getId(),
                user.getPhoneNumber(),
                user.getRole().name()
        );

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", rotation.getRefreshToken()
        ));
    }

    // 4️⃣ Logout -> revoke refresh token
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody LogoutRequest request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("status", "LOGGED_OUT"));
    }

    // 5️⃣ Logout all devices -> revoke every refresh token for this user
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(@Valid @RequestBody LogoutRequest request) {
        RefreshToken rt;
        try {
            rt = refreshTokenService.verifyForLogoutOnly(request.getRefreshToken());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", "INVALID_REFRESH_TOKEN"));
        }
        refreshTokenService.revokeAllByUser(rt.getUser().getId());
        return ResponseEntity.ok(Map.of("status", "ALL_SESSIONS_REVOKED"));
    }
}
