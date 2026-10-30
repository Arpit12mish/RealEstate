package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.SectionContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FestivePicksSectionLoaderTest {

    private final FestivePicksSectionLoader loader = new FestivePicksSectionLoader();

    @Test
    void usesDefaultFestiveCopyAndAllEightTabsInOrder() {
        HomeSectionDto<?> section = loader.load(new HomeSectionConfigEntity(), SectionContext.builder().cityId(0L).build());

        assertThat(loader.supports()).isEqualTo(HomeSectionType.FESTIVE_PICKS);
        assertThat(section.getKey()).isEqualTo("FESTIVE_PICKS");
        assertThat(section.getTitle()).isEqualTo("Festive picks on SFS");
        assertThat(section.getSubtitle()).isEqualTo("Verified builders, homes, shops and pros in one place");
        assertThat(section.getItems()).extracting(i -> ((FestivePicksSectionLoader.Tab) i).key()).containsExactly(
                "TRUSTED_BUILDERS", "VERIFIED_PROPERTIES", "LOCAL_SHOPS", "HOME_SERVICES",
                "TOP_ARCHITECTS", "INTERIOR_DESIGNERS", "SMART_CALCULATORS", "PARTNER_BRANDS");
    }

    @Test
    void configuredCopyWinsSoTheNextOccasionNeedsNoRelease() {
        HomeSectionConfigEntity cfg = new HomeSectionConfigEntity();
        cfg.setTitle("Wedding season picks");
        cfg.setSubtitle("Trusted pros for your new home");

        HomeSectionDto<?> section = loader.load(cfg, SectionContext.builder().build());

        assertThat(section.getTitle()).isEqualTo("Wedding season picks");
        assertThat(section.getSubtitle()).isEqualTo("Trusted pros for your new home");
        assertThat((List<?>) section.getItems()).hasSize(8);
    }
}
