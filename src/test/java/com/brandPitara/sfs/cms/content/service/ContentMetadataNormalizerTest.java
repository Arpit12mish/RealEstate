package com.brandPitara.sfs.cms.content.service;

import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentMetadataNormalizerTest {

    private final ContentMetadataNormalizer normalizer = new ContentMetadataNormalizer();

    @Test
    void trimsMetadataAndTreatsBlankOptionalValuesAsNull() {
        assertThat(normalizer.title("  Gurgaon   Market Guide  ")).isEqualTo("Gurgaon Market Guide");
        assertThat(normalizer.excerpt("  Summary  ")).isEqualTo("Summary");
        assertThat(normalizer.seoTitle(" ")).isNull();
        assertThat(normalizer.canonicalUrl(null)).isNull();
    }

    @Test
    void acceptsExternalHttpsCanonicalAndRejectsUnsafeProtocolsOrCredentials() {
        assertThat(normalizer.canonicalUrl("https://example.com/guides/../market"))
                .isEqualTo("https://example.com/market");

        for (String unsafe : new String[]{
                "http://example.com/post",
                "javascript:alert(1)",
                "data:text/plain,test",
                "file:///tmp/post",
                "https://user:pass@example.com/post"
        }) {
            assertThatThrownBy(() -> normalizer.canonicalUrl(unsafe))
                    .isInstanceOf(CmsContentApiException.class)
                    .extracting(exception -> ((CmsContentApiException) exception).getCode())
                    .isEqualTo("CONTENT_VALIDATION_ERROR");
        }
    }

    @Test
    void enforcesNormalizedTitleAndSeoLengthLimits() {
        assertThatThrownBy(() -> normalizer.title("ab"))
                .isInstanceOf(CmsContentApiException.class);
        assertThatThrownBy(() -> normalizer.title("a".repeat(221)))
                .isInstanceOf(CmsContentApiException.class);
        assertThatThrownBy(() -> normalizer.seoDescription("a".repeat(501)))
                .isInstanceOf(CmsContentApiException.class);
        assertThatThrownBy(() -> normalizer.title("Valid\u0000Title"))
                .isInstanceOf(CmsContentApiException.class);
    }
}
