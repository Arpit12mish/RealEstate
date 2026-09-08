package com.brandPitara.sfs.controller;

import com.brandPitara.sfs.exception.GlobalExceptionHandler;
import com.brandPitara.sfs.exception.OtpRequestException;
import com.brandPitara.sfs.exception.OtpRequestExceptionHandler;
import com.brandPitara.sfs.observability.LogSanitizer;
import com.brandPitara.sfs.service.AppUserDetailsService;
import com.brandPitara.sfs.service.GuestSessionService;
import com.brandPitara.sfs.service.LoginHistoryService;
import com.brandPitara.sfs.service.OnboardingService;
import com.brandPitara.sfs.service.OtpService;
import com.brandPitara.sfs.service.RefreshTokenService;
import com.brandPitara.sfs.service.UserService;
import com.brandPitara.sfs.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the two @RestControllerAdvice beans that can both claim a
 * ResponseStatusException-family exception actually dispatch the way
 * OtpRequestException's javadoc assumes: Spring's ExceptionHandlerMethodResolver
 * picks the most-specific @ExceptionHandler match across every registered advice,
 * so OtpRequestException (a ResponseStatusException subclass) resolves to
 * OtpRequestExceptionHandler's structured {success,code,retryAfterSeconds} body,
 * while a plain ResponseStatusException elsewhere still resolves to
 * GlobalExceptionHandler's generic ApiError shape. Uses the same
 * MockMvcBuilders.standaloneSetup + explicit advice list pattern as
 * PublicCityControllerTest (this codebase's existing MockMvc precedent).
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerExceptionHandlingTest {

    @Mock private JwtTokenUtil jwtTokenUtil;
    @Mock private AppUserDetailsService userDetailsService;
    @Mock private OtpService otpService;
    @Mock private UserService userService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private LoginHistoryService loginHistoryService;
    @Mock private OnboardingService onboardingService;
    @Mock private GuestSessionService guestSessionService;
    @Mock private LogSanitizer logSanitizer;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                jwtTokenUtil,
                userDetailsService,
                otpService,
                userService,
                refreshTokenService,
                loginHistoryService,
                onboardingService,
                guestSessionService,
                logSanitizer
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler(logSanitizer),
                        new OtpRequestExceptionHandler(logSanitizer)
                )
                .build();
    }

    @Test
    void otpRequestExceptionFromRequestOtpResolvesToStructuredContractNotGenericApiError() throws Exception {
        when(otpService.sendOtp("+919900000001")).thenThrow(
                new OtpRequestException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "OTP_RESEND_TOO_SOON",
                        "Please wait before requesting another OTP.",
                        18L
                )
        );

        mockMvc.perform(post("/api/auth/request-otp")
                        .contentType("application/json")
                        .content("{\"phoneNumber\":\"+919900000001\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("OTP_RESEND_TOO_SOON"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(18))
                // The generic ApiError shape (status/error/path/requestId) must NOT leak in.
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    @Test
    void sameRouteWithPlainResponseStatusExceptionStillUsesGenericApiErrorShape() throws Exception {
        when(otpService.sendOtp("+919900000002"))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "DB down"));

        mockMvc.perform(post("/api/auth/request-otp")
                        .contentType("application/json")
                        .content("{\"phoneNumber\":\"+919900000002\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("DB down"))
                .andExpect(jsonPath("$.status").value(503))
                // The OTP-specific shape must NOT leak into unrelated ResponseStatusExceptions.
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.code").doesNotExist());
    }
}
