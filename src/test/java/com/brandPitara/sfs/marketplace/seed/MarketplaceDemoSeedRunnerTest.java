package com.brandPitara.sfs.marketplace.seed;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

class MarketplaceDemoSeedRunnerTest {

    @ParameterizedTest
    @CsvSource({
            "8790012345, +918790012345",
            "'+91 87900 12345', +918790012345",
            "'087900-12345', +918790012345",
            "918790012345, +918790012345",
            "'+44 20 7946 0958', +442079460958"
    })
    void normalizesCommonIndianAndInternationalFormats(String raw, String expected) {
        assertThat(MarketplaceDemoSeedRunner.normalizePhone(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "879XXXXXXX", "12345", "0124", "+0123456789"})
    void blankOrUnrecognisableNumbersHideTheContactButtons(String raw) {
        assertThat(MarketplaceDemoSeedRunner.normalizePhone(raw)).isNull();
    }

    @org.junit.jupiter.api.Test
    void workerLoginNumbersMustBeUniqueIndianMobilesInWorkerOrder() {
        assertThat(MarketplaceDemoSeedRunner.workerLoginPhones("9876500001, +44 20 7946 0958, 9876500001"))
                .isEqualTo(Arrays.asList("+919876500001", null, null, null));
        assertThat(MarketplaceDemoSeedRunner.workerLoginPhones(""))
                .isEqualTo(Arrays.asList(null, null, null, null));
        assertThat(MarketplaceDemoSeedRunner.workerLoginPhones("9876500001,9876500002,9876500003,9876500004,9876500005"))
                .containsExactly("+919876500001", "+919876500002", "+919876500003", "+919876500004");
    }
}
