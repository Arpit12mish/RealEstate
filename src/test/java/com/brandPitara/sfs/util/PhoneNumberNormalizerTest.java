package com.brandPitara.sfs.util;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberNormalizerTest {

    @Test
    void normalizesCommonIndianPhoneVariants() {
        assertThat(PhoneNumberNormalizer.normalize("+91 98765 43210")).isEqualTo("+919876543210");
        assertThat(PhoneNumberNormalizer.normalize("9876543210")).isEqualTo("+919876543210");
        assertThat(PhoneNumberNormalizer.normalize("09876543210")).isEqualTo("+919876543210");
        assertThat(PhoneNumberNormalizer.normalize("919876543210")).isEqualTo("+919876543210");
    }

    @Test
    void rejectsMalformedPhoneNumbers() {
        assertThatThrownBy(() -> PhoneNumberNormalizer.normalize("12345"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("valid E.164");
    }

    @Test
    void returnsLegacyLookupValuesForIndianNumbers() {
        assertThat(PhoneNumberNormalizer.equivalentLookupValues("9876543210"))
                .containsExactly("+919876543210", "9876543210", "919876543210", "09876543210");
    }
}
