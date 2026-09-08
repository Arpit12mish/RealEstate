package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.config.LocalFakeOtpProperties;
import com.brandPitara.sfs.service.model.OtpSendResult;
import com.brandPitara.sfs.service.model.OtpVerificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakeOtpServiceTest {

    private static final Set<String> TEST_NUMBERS = IntStream.rangeClosed(1, 20)
            .mapToObj(index -> "+9199000000" + String.format("%02d", index))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private LocalFakeOtpProperties properties;
    private FakeOtpService service;

    @BeforeEach
    void setUp() {
        properties = new LocalFakeOtpProperties();
        properties.setEnabled(true);
        properties.setFixedOtp("123456");
        properties.setPhoneNumbers(TEST_NUMBERS);
        service = new FakeOtpService(properties);
    }

    static Stream<String> allTwentyAllowlistedNumbers() {
        return TEST_NUMBERS.stream();
    }

    @ParameterizedTest
    @MethodSource("allTwentyAllowlistedNumbers")
    void approvesAllTwentyAllowlistedNumbersWithFixedOtp(String phoneNumber) {
        assertThat(service.verifyOtp(phoneNumber, "123456").isApproved()).isTrue();
    }

    @Test
    void retainsAllTwentyNumbersInCanonicalNormalizedForm() {
        assertThat(properties.getPhoneNumbers()).containsExactlyInAnyOrderElementsOf(TEST_NUMBERS);
    }

    @Test
    void rejectsWrongOtpForAllowlistedNumber() {
        assertThat(service.verifyOtp("+919900000001", "654321").isApproved()).isFalse();
        assertThat(service.verifyOtp("+919900000020", "654321").isApproved()).isFalse();
    }

    @Test
    void rejectsFixedOtpForNumberOneAboveTheAllowlistedRange() {
        assertThat(service.verifyOtp("+919900000021", "123456").isApproved()).isFalse();
    }

    @Test
    void rejectsFixedOtpForUnrelatedRealLookingNumber() {
        assertThat(service.verifyOtp("+919876543210", "123456").isApproved()).isFalse();
    }

    @Test
    void rejectsFixedOtpForExactMatchNearMiss() {
        assertThat(service.verifyOtp("+91990000001", "123456").isApproved()).isFalse();
    }

    @Test
    void rejectsAllowlistedNumberWhenBypassIsDisabled() {
        properties.setEnabled(false);

        assertThat(service.verifyOtp("+919900000001", "123456").isApproved()).isFalse();
        assertThat(service.verifyOtp("+919900000020", "123456").isApproved()).isFalse();
    }

    @Test
    void normalizesSupportedPhoneInputBeforeExactAllowlistMatch() {
        OtpVerificationResult sendNormalized = service.verifyOtp("9900000001", "123456");
        OtpVerificationResult verifyNormalized = service.verifyOtp("+919900000001", "123456");

        assertThat(sendNormalized.isApproved()).isTrue();
        assertThat(sendNormalized.getNormalizedPhoneNumber()).isEqualTo("+919900000001");
        assertThat(verifyNormalized.getNormalizedPhoneNumber())
                .isEqualTo(sendNormalized.getNormalizedPhoneNumber());
    }

    @Test
    void requestOtpReturnsExistingContractWithoutExternalProvider() {
        OtpSendResult result = service.sendOtp("+919900000001");

        assertThat(result.getStatus()).isEqualTo("OTP_SENT");
        assertThat(result.getMessage()).isEqualTo("OTP sent successfully");
        assertThat(result.getResendAfterSeconds()).isEqualTo(30);
        assertThat(result.getExpiresInSeconds()).isPositive();
        assertThat(result.getNormalizedPhoneNumber()).isEqualTo("+919900000001");
    }

    @Test
    void duplicateAllowlistEntriesAreSafelyDeduplicated() {
        Set<String> withDuplicates = new LinkedHashSet<>(TEST_NUMBERS);
        withDuplicates.add("9900000001");
        withDuplicates.add("+919900000001");

        LocalFakeOtpProperties dedupedProperties = new LocalFakeOtpProperties();
        dedupedProperties.setPhoneNumbers(withDuplicates);

        assertThat(dedupedProperties.getPhoneNumbers()).containsExactlyInAnyOrderElementsOf(TEST_NUMBERS);
    }

    @Test
    void blankAllowlistEntryIsRejectedRatherThanSilentlyIgnored() {
        LocalFakeOtpProperties blankEntryProperties = new LocalFakeOtpProperties();

        assertThatThrownBy(() -> blankEntryProperties.setPhoneNumbers(Set.of("+919900000001", "   ")))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
