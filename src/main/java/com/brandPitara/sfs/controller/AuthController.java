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
import com.brandPitara.sfs.service.LoginHistoryService;
import com.brandPitara.sfs.service.OtpService;
import com.brandPitara.sfs.service.RefreshTokenService;
import com.brandPitara.sfs.service.UserService;
import com.brandPitara.sfs.service.model.UserLoginResult;
// import com.brandPitara.sfs.service.PhoneAuthService;
// import com.brandPitara.sfs.service.UserService;
import com.brandPitara.sfs.util.JwtTokenUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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

    // 1️⃣ Request OTP  TODO: add rate limiting per phone/IP here
    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@Valid @RequestBody SendOtpRequest request) {
        
        otpService.sendOtp(request.getPhoneNumber());
        return ResponseEntity.ok(Map.of("status", "OTP_SENT"));
    }

    // 2️⃣ Verify OTP => issue access + refresh tokens
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request, HttpServletRequest httpRequest) {

        boolean ok = otpService.verifyOtp(request.getPhoneNumber(), request.getCode());

        if (!ok) {
            loginHistoryService.recordLogin(null, "OTP", false, request.getDeviceId(),
                    request.getFcmToken(), httpRequest);

            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_OTP"));
        }

        // Find or create user
        UserLoginResult loginResult = userService.findOrCreateVerifiedUserByPhone(request.getPhoneNumber());

        User user = loginResult.getUser();
        boolean isNewUser = loginResult.isNewUser();

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

        // Refresh token
        RefreshToken refreshToken =
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
        payload.put("refreshToken", refreshToken.getToken());
        payload.put("user", response);
        payload.put("isNewUser", isNewUser); // or track separately if needed

        return ResponseEntity.ok(payload);
    }

    // 3️⃣ Refresh token -> new access token
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {

        RefreshToken rt;
        try {
            rt = refreshTokenService.verifyAndGet(request.getRefreshToken());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", "INVALID_REFRESH_TOKEN"));
        }

        User user = rt.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        loginHistoryService.recordLogin(
            user, "REFRESH", true, rt.getDeviceId(), rt.getFcmToken(), httpRequest
        );

        String newAccessToken = jwtTokenUtil.generateToken(
                userDetails,
                user.getId(),
                user.getPhoneNumber(),
                user.getRole().name()
        );

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken
        ));
    }

    // 4️⃣ Logout -> revoke refresh token
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody LogoutRequest request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("status", "LOGGED_OUT"));
    }
}
