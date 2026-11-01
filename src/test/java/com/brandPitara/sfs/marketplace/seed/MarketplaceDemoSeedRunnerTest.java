package com.brandPitara.sfs.marketplace.seed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

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
        int n = MarketplaceDemoSeedRunner.WORKERS.size();
        List<String> mixed = MarketplaceDemoSeedRunner.workerLoginPhones("9876500001, +44 20 7946 0958, 9876500001");
        assertThat(mixed).hasSize(n);
        assertThat(mixed.subList(0, 3)).isEqualTo(Arrays.asList("+919876500001", null, null));
        assertThat(mixed.subList(3, n)).containsOnlyNulls();
        assertThat(MarketplaceDemoSeedRunner.workerLoginPhones("")).hasSize(n).containsOnlyNulls();
        // The original four numbers keep working; later workers stay skipped until theirs are appended.
        List<String> four = MarketplaceDemoSeedRunner.workerLoginPhones("9876500001,9876500002,9876500003,9876500004");
        assertThat(four.subList(0, 4)).containsExactly("+919876500001", "+919876500002", "+919876500003", "+919876500004");
        assertThat(four.subList(4, n)).containsOnlyNulls();
        List<String> more = MarketplaceDemoSeedRunner.workerLoginPhones("9876500001,9876500002,9876500003,9876500004,9876500005,9876500006");
        assertThat(more.subList(4, 6)).containsExactly("+919876500005", "+919876500006");
    }

    @Test
    void imageBaseUrlIsNormalisedAndRejectedWhenNotHttp() {
        assertThat(MarketplaceDemoSeedRunner.normalizeImageBase("https://appapi.squarefootstory.com/")).isEqualTo("https://appapi.squarefootstory.com");
        assertThat(MarketplaceDemoSeedRunner.normalizeImageBase(" http://10.0.2.2:8084 ")).isEqualTo("http://10.0.2.2:8084");
        assertThat(MarketplaceDemoSeedRunner.normalizeImageBase("")).isNull();
        assertThat(MarketplaceDemoSeedRunner.normalizeImageBase(null)).isNull();
        assertThat(MarketplaceDemoSeedRunner.normalizeImageBase("/api/public/demo-images")).isNull();
        assertThat(MarketplaceDemoSeedRunner.normalizeImageBase("javascript:alert(1)")).isNull();
    }
}
