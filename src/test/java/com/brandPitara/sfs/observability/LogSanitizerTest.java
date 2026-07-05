package com.brandPitara.sfs.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves LogSanitizer actually masks the sensitive values production logging
 * hardening depends on: Authorization headers, passwordHash, access/refresh
 * tokens, OTPs, and the X-Goog-Api-Key header GooglePlacesClient/
 * GoogleNearbyPlaceProvider send on every outbound request.
 */
class LogSanitizerTest {

    private final LogSanitizer sanitizer = new LogSanitizer();

    @Test
    void maskAuthorizationHeaderHidesBearerTokenButKeepsTheScheme() {
        String masked = sanitizer.maskAuthorizationHeader("Bearer super-secret-jwt-value");

        assertThat(masked).isEqualTo("Bearer ****");
        assertThat(masked).doesNotContain("super-secret-jwt-value");
    }

    @Test
    void maskAuthorizationHeaderHandlesNonBearerSchemes() {
        assertThat(sanitizer.maskAuthorizationHeader("Basic dXNlcjpwYXNz")).isEqualTo("****");
    }

    @Test
    void maskTokenNeverReturnsTheRawValue() {
        String rawToken = "eyJhbGciOiJIUzI1NiJ9.super-secret-refresh-token";

        assertThat(sanitizer.maskToken(rawToken)).isEqualTo("****");
        assertThat(sanitizer.maskToken(rawToken)).doesNotContain(rawToken);
    }

    @Test
    void sanitizeQueryStringMasksPasswordAndPasswordHash() {
        assertThat(sanitizer.sanitizeQueryString("password=hunter2"))
                .isEqualTo("password=****")
                .doesNotContain("hunter2");
        assertThat(sanitizer.sanitizeQueryString("passwordHash=$2a$10$abcdefghijklmnopqrstuv"))
                .isEqualTo("passwordHash=****")
                .doesNotContain("$2a$10$abcdefghijklmnopqrstuv");
    }

    @Test
    void sanitizeQueryStringMasksOtpAndTokenFields() {
        assertThat(sanitizer.sanitizeQueryString("otp=482913")).isEqualTo("otp=****");
        assertThat(sanitizer.sanitizeQueryString("accessToken=raw-access-token-value"))
                .isEqualTo("accessToken=****")
                .doesNotContain("raw-access-token-value");
        assertThat(sanitizer.sanitizeQueryString("refreshToken=raw-refresh-token-value"))
                .isEqualTo("refreshToken=****")
                .doesNotContain("raw-refresh-token-value");
    }

    @Test
    void sanitizeQueryStringMasksGoogleApiKeyIfEverPassedAsAQueryParam() {
        assertThat(sanitizer.sanitizeQueryString("X-Goog-Api-Key=AIzaSyRealKeyValue1234567890"))
                .isEqualTo("X-Goog-Api-Key=****")
                .doesNotContain("AIzaSyRealKeyValue1234567890");
    }

    @Test
    void sanitizeQueryStringLeavesNonSensitiveFieldsReadable() {
        assertThat(sanitizer.sanitizeQueryString("cityId=1&page=0")).isEqualTo("cityId=1&page=0");
    }

    @Test
    void sanitizeQueryStringStripsNewlinesToPreventLogInjection() {
        String malicious = "q=test\nfake-log-line=injected";

        assertThat(sanitizer.sanitizeQueryString(malicious)).doesNotContain("\n");
    }

    @Test
    void sanitizeMapMasksSensitiveFormParamsTheSameWayAsQueryStrings() {
        String masked = sanitizer.sanitizeMap(
                java.util.Map.of("password", new String[]{"hunter2"}, "cityId", new String[]{"1"})
        );

        assertThat(masked).contains("password=****");
        assertThat(masked).doesNotContain("hunter2");
        assertThat(masked).contains("cityId=1");
    }

    @Test
    void maskPhoneNeverExposesTheFullNumber() {
        String masked = sanitizer.maskPhone("+919876543210");

        assertThat(masked).doesNotContain("9876543210");
    }

    @Test
    void maskEmailNeverExposesTheFullLocalPart() {
        String masked = sanitizer.maskEmail("sensitive.user@example.com");

        assertThat(masked).doesNotContain("sensitive.user");
        assertThat(masked).contains("@example.com");
    }
}
