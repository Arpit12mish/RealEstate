package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.home.dto.ComparePropertiesCardDto;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.SectionContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComparePropertiesSectionLoaderTest {

    private final ComparePropertiesSectionLoader loader = new ComparePropertiesSectionLoader();

    private static final String CDN_URL = "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/home/comparision-animation.json";

    private HomeSectionConfigEntity cfg(String title, String subtitle, String param1) {
        return HomeSectionConfigEntity.builder()
                .sectionType(HomeSectionType.COMPARE_PROPERTIES)
                .title(title)
                .subtitle(subtitle)
                .param1(param1)
                .maxItems(1)
                .build();
    }

    private SectionContext ctx() {
        return new SectionContext(null, null, null, null, null, null, null, null, null, null, null, false, null);
    }

    // 1. supports() returns COMPARE_PROPERTIES
    @Test
    void supports_returnsCompareProperties() {
        assertThat(loader.supports()).isEqualTo(HomeSectionType.COMPARE_PROPERTIES);
    }

    // 2. mediaUrl missing → returns null (section dropped by feed)
    @Test
    void missingMediaUrl_returnsNull() {
        HomeSectionConfigEntity cfgNoUrl = cfg("Compare Properties", "Subtitle", null);
        assertThat(loader.load(cfgNoUrl, ctx())).isNull();
    }

    // 3. blank mediaUrl → returns null
    @Test
    void blankMediaUrl_returnsNull() {
        HomeSectionConfigEntity cfgBlank = cfg("Compare Properties", "Subtitle", "   ");
        assertThat(loader.load(cfgBlank, ctx())).isNull();
    }

    // 4. valid mediaUrl → returns section with one item
    @Test
    void validMediaUrl_returnsSectionWithOneItem() {
        HomeSectionDto<?> section = loader.load(cfg("Compare Properties", "Sub", CDN_URL), ctx());
        assertThat(section).isNotNull();
        assertThat(section.getItems()).hasSize(1);
    }

    // 5. section type is COMPARE_PROPERTIES
    @Test
    void validMediaUrl_sectionTypeIsCompareProperties() {
        HomeSectionDto<?> section = loader.load(cfg("Compare Properties", "Sub", CDN_URL), ctx());
        assertThat(section.getType()).isEqualTo(HomeSectionType.COMPARE_PROPERTIES);
    }

    // 6. section key is "COMPARE_PROPERTIES"
    @Test
    void validMediaUrl_sectionKeyIsCompareProperties() {
        HomeSectionDto<?> section = loader.load(cfg("Compare Properties", "Sub", CDN_URL), ctx());
        assertThat(section.getKey()).isEqualTo("COMPARE_PROPERTIES");
    }

    // 7. card mediaType is "LOTTIE_JSON"
    @Test
    void card_mediaTypeIsLottieJson() {
        HomeSectionDto<?> section = loader.load(cfg("Compare Properties", "Sub", CDN_URL), ctx());
        ComparePropertiesCardDto card = (ComparePropertiesCardDto) section.getItems().get(0);
        assertThat(card.getMediaType()).isEqualTo("LOTTIE_JSON");
    }

    // 8. card mediaUrl matches param1
    @Test
    void card_mediaUrlMatchesParam1() {
        HomeSectionDto<?> section = loader.load(cfg("Compare Properties", "Sub", CDN_URL), ctx());
        ComparePropertiesCardDto card = (ComparePropertiesCardDto) section.getItems().get(0);
        assertThat(card.getMediaUrl()).isEqualTo(CDN_URL);
    }

    // 9. card actionType is OPEN_COMPARE_SEARCH
    @Test
    void card_actionTypeIsOpenCompareSearch() {
        HomeSectionDto<?> section = loader.load(cfg("Compare Properties", "Sub", CDN_URL), ctx());
        ComparePropertiesCardDto card = (ComparePropertiesCardDto) section.getItems().get(0);
        assertThat(card.getActionType()).isEqualTo("OPEN_COMPARE_SEARCH");
    }

    // 10. card actionValue is /search?mode=compare
    @Test
    void card_actionValueIsCompareSearchPath() {
        HomeSectionDto<?> section = loader.load(cfg("Compare Properties", "Sub", CDN_URL), ctx());
        ComparePropertiesCardDto card = (ComparePropertiesCardDto) section.getItems().get(0);
        assertThat(card.getActionValue()).isEqualTo("/search?mode=compare");
    }

    // 11. section title comes from cfg.title
    @Test
    void sectionTitle_usesConfigTitle() {
        HomeSectionDto<?> section = loader.load(cfg("Custom Title", "Sub", CDN_URL), ctx());
        assertThat(section.getTitle()).isEqualTo("Custom Title");
    }

    // 12. section subtitle comes from cfg.subtitle
    @Test
    void sectionSubtitle_usesConfigSubtitle() {
        HomeSectionDto<?> section = loader.load(cfg("T", "Custom Subtitle", CDN_URL), ctx());
        assertThat(section.getSubtitle()).isEqualTo("Custom Subtitle");
    }

    // 13. null title falls back to default title
    @Test
    void nullConfigTitle_usesDefaultTitle() {
        HomeSectionDto<?> section = loader.load(cfg(null, "Sub", CDN_URL), ctx());
        assertThat(section.getTitle()).isEqualTo("Compare Properties");
    }

    // 14. null subtitle falls back to default subtitle
    @Test
    void nullConfigSubtitle_usesDefaultSubtitle() {
        HomeSectionDto<?> section = loader.load(cfg("T", null, CDN_URL), ctx());
        assertThat(section.getSubtitle()).contains("Compare prices");
    }
}
