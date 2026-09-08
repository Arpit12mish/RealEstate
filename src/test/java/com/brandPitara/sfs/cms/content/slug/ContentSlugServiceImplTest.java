package com.brandPitara.sfs.cms.content.slug;

import com.brandPitara.sfs.cms.content.exception.CmsContentApiException;
import com.brandPitara.sfs.cms.content.repository.ContentPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentSlugServiceImplTest {

    private ContentPostRepository repository;
    private ContentSlugServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ContentPostRepository.class);
        service = new ContentSlugServiceImpl(repository);
    }

    @Test
    void normalizesCaseWhitespacePunctuationRepeatedHyphensAndLatinDiacritics() {
        assertThat(service.normalize("  Best Places: Buy Property -- in Gurugrám!  "))
                .isEqualTo("best-places-buy-property-in-gurugram");
    }

    @Test
    void createsSlugFromTitleAndSuffixesAutomaticCollisionsDeterministically() {
        when(repository.existsBySlug("gurgaon-property-guide")).thenReturn(true);
        when(repository.existsBySlug("gurgaon-property-guide-2")).thenReturn(true);
        when(repository.existsBySlug("gurgaon-property-guide-3")).thenReturn(false);

        assertThat(service.resolveForCreate(null, "Gurgaon Property Guide"))
                .isEqualTo("gurgaon-property-guide-3");
    }

    @Test
    void acceptsAndNormalizesCustomSlugButRejectsDuplicateManualSlug() {
        when(repository.existsBySlug("market-guide-2026")).thenReturn(false);
        assertThat(service.resolveForCreate(" Market Guide 2026 ", "ignored"))
                .isEqualTo("market-guide-2026");

        when(repository.existsBySlug("market-guide-2026")).thenReturn(true);
        assertThatThrownBy(() -> service.resolveForCreate("MARKET-GUIDE-2026", "ignored"))
                .isInstanceOf(CmsContentApiException.class)
                .extracting(exception -> ((CmsContentApiException) exception).getCode())
                .isEqualTo("CONTENT_SLUG_CONFLICT");
    }

    @Test
    void rejectsReservedOrUnsafeEmptyManualSlugs() {
        assertThatThrownBy(() -> service.resolveForCreate("dashboard", "ignored"))
                .isInstanceOf(CmsContentApiException.class)
                .hasMessageContaining("reserved");
        assertThatThrownBy(() -> service.resolveForCreate("🔥🔥", "ignored"))
                .isInstanceOf(CmsContentApiException.class)
                .hasMessageContaining("at least 3");
    }

    @Test
    void manualUpdateChecksGlobalUniquenessExcludingCurrentPost() {
        when(repository.existsBySlugAndIdNot("updated-guide", 9L)).thenReturn(false);
        assertThat(service.resolveForUpdate("Updated Guide", 9L)).isEqualTo("updated-guide");

        when(repository.existsBySlugAndIdNot("taken-guide", 9L)).thenReturn(true);
        assertThatThrownBy(() -> service.resolveForUpdate("taken-guide", 9L))
                .isInstanceOf(CmsContentApiException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void outputNeverExceedsDatabaseLimitEvenWithSuffix() {
        String longTitle = "word ".repeat(100);
        when(repository.existsBySlug(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(true, false);

        assertThat(service.resolveForCreate(null, longTitle))
                .hasSizeLessThanOrEqualTo(180)
                .endsWith("-2");
    }
}
