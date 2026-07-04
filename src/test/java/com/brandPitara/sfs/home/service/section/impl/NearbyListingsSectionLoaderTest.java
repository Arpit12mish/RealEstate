package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.project.dto.ProjectNearbyListingCardDto;
import com.brandPitara.sfs.project.service.PublicProjectNearbyListingService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NearbyListingsSectionLoaderTest {

  @Test
  void loadBuildsNearbyListingsSectionWithLocationContext() {
    PublicProjectNearbyListingService listingService = mock(PublicProjectNearbyListingService.class);
    NearbyListingsSectionLoader loader = new NearbyListingsSectionLoader(listingService);

    HomeSectionConfigEntity cfg = HomeSectionConfigEntity.builder()
        .title("Nearby Listings")
        .maxItems(5)
        .build();
    SectionContext ctx = SectionContext.builder()
        .cityId(1L)
        .latitude(28.6139)
        .longitude(77.2090)
        .resolvedCityName("Delhi NCR")
        .hasUserCoordinates(true)
        .build();
    ProjectNearbyListingCardDto card = ProjectNearbyListingCardDto.builder()
        .projectId(101L)
        .projectName("Park Residences")
        .distanceLabel("1.2 km")
        .build();

    when(listingService.listNearby(28.6139, 77.2090, 1L, 5)).thenReturn(List.of(card));

    HomeSectionDto<?> section = loader.load(cfg, ctx);

    assertThat(section.getType()).isEqualTo(HomeSectionType.NEARBY_LISTINGS);
    assertThat(section.getKey()).isEqualTo("NEARBY_LISTINGS");
    assertThat(section.getTitle()).isEqualTo("Nearby Listings");
    assertThat(section.getSubtitle()).isEqualTo("Around Delhi NCR");
    assertThat(section.getItems()).hasSize(1);
    assertThat(section.getItems().get(0)).isSameAs(card);
    assertThat(((ProjectNearbyListingCardDto) section.getItems().get(0)).getDistanceLabel()).isEqualTo("1.2 km");
    verify(listingService).listNearby(28.6139, 77.2090, 1L, 5);
  }

  @Test
  void loadUsesRecommendedCopyForGlobalFallback() {
    PublicProjectNearbyListingService listingService = mock(PublicProjectNearbyListingService.class);
    NearbyListingsSectionLoader loader = new NearbyListingsSectionLoader(listingService);

    HomeSectionConfigEntity cfg = HomeSectionConfigEntity.builder()
        .title("Nearby Listings")
        .subtitle("Nearby homes and projects")
        .maxItems(99)
        .build();
    ProjectNearbyListingCardDto card = ProjectNearbyListingCardDto.builder()
        .projectId(101L)
        .projectName("Park Residences")
        .distanceLabel(null)
        .build();

    when(listingService.listNearby(null, null, null, 10)).thenReturn(List.of(card));

    HomeSectionDto<?> section = loader.load(cfg, null);

    assertThat(section.getTitle()).isEqualTo("Recommended Projects");
    assertThat(section.getSubtitle()).isEqualTo("Popular homes and projects");
    assertThat(section.getItems()).hasSize(1);
    assertThat(((ProjectNearbyListingCardDto) section.getItems().get(0)).getDistanceLabel()).isNull();
    verify(listingService).listNearby(null, null, null, 10);
  }

  @Test
  void loadUsesCityCopyWhenCityIsSelectedWithoutCoordinates() {
    PublicProjectNearbyListingService listingService = mock(PublicProjectNearbyListingService.class);
    NearbyListingsSectionLoader loader = new NearbyListingsSectionLoader(listingService);

    HomeSectionConfigEntity cfg = HomeSectionConfigEntity.builder()
        .title("Nearby Listings")
        .subtitle("Nearby homes and projects")
        .maxItems(5)
        .build();
    SectionContext ctx = SectionContext.builder()
        .cityId(1L)
        .resolvedCityName("New Delhi")
        .hasUserCoordinates(false)
        .build();
    ProjectNearbyListingCardDto card = ProjectNearbyListingCardDto.builder()
        .projectId(101L)
        .projectName("Park Residences")
        .distanceLabel(null)
        .build();

    when(listingService.listNearby(null, null, 1L, 5)).thenReturn(List.of(card));

    HomeSectionDto<?> section = loader.load(cfg, ctx);

    assertThat(section.getTitle()).isEqualTo("Projects in New Delhi");
    assertThat(section.getSubtitle()).isEqualTo("Popular projects in your selected city");
    assertThat(section.getItems()).hasSize(1);
    assertThat(((ProjectNearbyListingCardDto) section.getItems().get(0)).getDistanceLabel()).isNull();
    verify(listingService).listNearby(null, null, 1L, 5);
  }

  @Test
  void loadUsesGlobalCopyForDetectedDeviceCityWithoutExplicitCityFilter() {
    PublicProjectNearbyListingService listingService = mock(PublicProjectNearbyListingService.class);
    NearbyListingsSectionLoader loader = new NearbyListingsSectionLoader(listingService);

    HomeSectionConfigEntity cfg = HomeSectionConfigEntity.builder()
        .title("Nearby Listings")
        .maxItems(5)
        .build();
    SectionContext ctx = SectionContext.builder()
        .deviceCity("Noida")
        .resolvedCityName("Noida")
        .hasUserCoordinates(false)
        .build();
    ProjectNearbyListingCardDto card = ProjectNearbyListingCardDto.builder()
        .projectId(202L)
        .projectName("Global Project")
        .build();

    when(listingService.listNearby(null, null, null, 5)).thenReturn(List.of(card));

    HomeSectionDto<?> section = loader.load(cfg, ctx);

    assertThat(section.getTitle()).isEqualTo("Recommended Projects");
    assertThat(section.getSubtitle()).isEqualTo("Popular homes and projects");
    assertThat(section.getItems()).hasSize(1);
    verify(listingService).listNearby(null, null, null, 5);
  }
}
