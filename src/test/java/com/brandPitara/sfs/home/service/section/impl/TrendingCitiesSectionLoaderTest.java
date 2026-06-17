package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.dto.TrendingCityCardResponse;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.service.PublicCityService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TrendingCitiesSectionLoaderTest {

    @Test
    void loadBuildsTrendingCitiesHomeSection() {
        PublicCityService publicCityService = mock(PublicCityService.class);
        TrendingCitiesSectionLoader loader = new TrendingCitiesSectionLoader(publicCityService);

        HomeSectionConfigEntity cfg = HomeSectionConfigEntity.builder()
                .title("Trending Cities")
                .subtitle("Hot real estate markets")
                .maxItems(6)
                .build();
        TrendingCityCardResponse city = TrendingCityCardResponse.builder()
                .id(7L)
                .name("Mumbai")
                .slug("mumbai")
                .projectCount(8420L)
                .growthPercent(12.4)
                .displayOrder(1)
                .build();

        when(publicCityService.getTrendingCities(6)).thenReturn(List.of(city));

        HomeSectionDto<?> section = loader.load(cfg, null);

        assertThat(section.getType()).isEqualTo(HomeSectionType.TRENDING_CITIES);
        assertThat(section.getKey()).isEqualTo("TRENDING_CITIES");
        assertThat(section.getTitle()).isEqualTo("Trending Cities");
        assertThat(section.getSubtitle()).isEqualTo("Hot real estate markets");
        assertThat(section.getItems()).hasSize(1);
        assertThat(section.getItems().get(0)).isSameAs(city);
        verify(publicCityService).getTrendingCities(6);
    }

    @Test
    void loadUsesDefaultCopyWhenConfigCopyIsBlank() {
        PublicCityService publicCityService = mock(PublicCityService.class);
        TrendingCitiesSectionLoader loader = new TrendingCitiesSectionLoader(publicCityService);

        HomeSectionConfigEntity cfg = HomeSectionConfigEntity.builder()
                .title(" ")
                .subtitle(null)
                .maxItems(0)
                .build();
        when(publicCityService.getTrendingCities(1)).thenReturn(List.of());

        HomeSectionDto<?> section = loader.load(cfg, null);

        assertThat(section.getTitle()).isEqualTo("Trending Cities");
        assertThat(section.getSubtitle()).isEqualTo("Hot real estate markets");
        verify(publicCityService).getTrendingCities(1);
    }
}
