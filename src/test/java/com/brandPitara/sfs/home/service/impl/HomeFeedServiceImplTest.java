package com.brandPitara.sfs.home.service.impl;

import com.brandPitara.sfs.common.contentVersion.repository.ContentVersionRepository;
import com.brandPitara.sfs.dto.PromoBannerResponse;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.feed.enums.FeedScreen;
import com.brandPitara.sfs.home.dto.HomeFeedResponse;
import com.brandPitara.sfs.home.dto.HomeFeedRequest;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.entity.PromoBannerSlotConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.repository.HomeSectionConfigRepository;
import com.brandPitara.sfs.home.repository.PromoBannerSlotConfigRepository;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.home.service.section.impl.NearbyListingsSectionLoader;
import com.brandPitara.sfs.home.service.section.impl.PromoBannersSectionLoader;
import com.brandPitara.sfs.project.dto.ProjectNearbyListingCardDto;
import com.brandPitara.sfs.project.service.PublicProjectNearbyListingService;
import com.brandPitara.sfs.repository.CityRepository;
import com.brandPitara.sfs.service.PromoBannerService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeFeedServiceImplTest {

  private static final String LOTTIE_URL =
      "https://sfs-s3bucket.s3.ap-south-1.amazonaws.com/home/hero-animation.json";

  @Test
  void getHomeMovesTrendingCitiesImmediatelyAfterProjectAnalytics() {
    ContentVersionRepository contentVersionRepository = mock(ContentVersionRepository.class);
    HomeSectionConfigRepository homeSectionConfigRepository = mock(HomeSectionConfigRepository.class);
    PromoBannerSlotConfigRepository promoBannerSlotConfigRepository = mock(PromoBannerSlotConfigRepository.class);
    PromoBannerService promoBannerService = mock(PromoBannerService.class);
    CityRepository cityRepository = mock(CityRepository.class);

    HomeFeedServiceImpl service = new HomeFeedServiceImpl(
        contentVersionRepository,
        homeSectionConfigRepository,
        List.of(
            loader(HomeSectionType.PROJECT_ANALYTICS),
            loader(HomeSectionType.NEARBY_LISTINGS),
            loader(HomeSectionType.TOP_PROJECTS),
            loader(HomeSectionType.ARCHITECTS),
            loader(HomeSectionType.DESIGNERS),
            loader(HomeSectionType.TOP_BUILDERS),
            loader(HomeSectionType.TRENDING_CITIES)
        ),
        promoBannerSlotConfigRepository,
        promoBannerService,
        new PromoBannerInjector(),
        cityRepository
    );

    when(contentVersionRepository.findById("HOME:ALL")).thenReturn(Optional.empty());
    when(homeSectionConfigRepository.findByHomeCategory_IdAndEnabledTrueOrderBySortOrderAscIdAsc(0L))
        .thenReturn(List.of(
            config(HomeSectionType.PROJECT_ANALYTICS),
            config(HomeSectionType.TOP_PROJECTS),
            config(HomeSectionType.ARCHITECTS),
            config(HomeSectionType.DESIGNERS),
            config(HomeSectionType.TOP_BUILDERS),
            config(HomeSectionType.NEARBY_LISTINGS),
            config(HomeSectionType.TRENDING_CITIES)
        ));
    when(promoBannerSlotConfigRepository
        .findByScreenAndHomeCategory_IdAndActiveTrueOrderByPriorityAscIdAsc(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq(0L)
        ))
        .thenReturn(List.of());

    HomeFeedResponse response = service.getHome(null, null, null, null);
    List<HomeSectionType> sectionTypes = response.getSections().stream()
        .map(HomeSectionDto::getType)
        .toList();

    // Canonical order (PROJECT_ANALYTICS, NEARBY_LISTINGS, TRENDING_CITIES, ARCHITECTS, DESIGNERS)
    // wins regardless of home_section_config sort_order; TOP_PROJECTS/TOP_BUILDERS are legacy
    // types with no slot in the canonical order, so they trail at the end, in their original
    // relative order, instead of being dropped.
    assertThat(sectionTypes).containsExactly(
        HomeSectionType.PROJECT_ANALYTICS,
        HomeSectionType.NEARBY_LISTINGS,
        HomeSectionType.TRENDING_CITIES,
        HomeSectionType.ARCHITECTS,
        HomeSectionType.DESIGNERS,
        HomeSectionType.TOP_PROJECTS,
        HomeSectionType.TOP_BUILDERS
    );
    assertThat(sectionTypes).filteredOn(type -> type == HomeSectionType.TRENDING_CITIES).hasSize(1);
    assertThat(response.getSections()).allSatisfy(section -> assertThat(section.getItems()).isNotEmpty());
  }

  @Test
  void getHomeAppliesCanonicalOrderAcrossAllElevenSections() {
    ContentVersionRepository contentVersionRepository = mock(ContentVersionRepository.class);
    HomeSectionConfigRepository homeSectionConfigRepository = mock(HomeSectionConfigRepository.class);
    PromoBannerSlotConfigRepository promoBannerSlotConfigRepository = mock(PromoBannerSlotConfigRepository.class);
    PromoBannerService promoBannerService = mock(PromoBannerService.class);
    CityRepository cityRepository = mock(CityRepository.class);

    // Deliberately scrambled relative to the desired order, to prove the fix doesn't just
    // happen to match because home_section_config already agrees with it.
    List<HomeSectionType> dbOrder = List.of(
        HomeSectionType.DESIGNERS,
        HomeSectionType.INSTAGRAM_REELS,
        HomeSectionType.ARCHITECTS,
        HomeSectionType.SMART_CALCULATORS,
        HomeSectionType.TRENDING_CITIES,
        HomeSectionType.CONNECTED_BRANDS,
        HomeSectionType.BUILDER_CREDIBILITY_CARDS,
        HomeSectionType.COMPARE_PROPERTIES,
        HomeSectionType.NEARBY_LISTINGS,
        HomeSectionType.PROJECT_ANALYTICS
    );

    HomeFeedServiceImpl service = new HomeFeedServiceImpl(
        contentVersionRepository,
        homeSectionConfigRepository,
        dbOrder.stream().map(HomeFeedServiceImplTest::loader).toList(),
        promoBannerSlotConfigRepository,
        promoBannerService,
        new PromoBannerInjector(),
        cityRepository
    );

    when(contentVersionRepository.findById("HOME:ALL")).thenReturn(Optional.empty());
    when(homeSectionConfigRepository.findByHomeCategory_IdAndEnabledTrueOrderBySortOrderAscIdAsc(0L))
        .thenReturn(dbOrder.stream().map(HomeFeedServiceImplTest::config).toList());
    when(promoBannerSlotConfigRepository
        .findByScreenAndHomeCategory_IdAndActiveTrueOrderByPriorityAscIdAsc(any(), eq(0L)))
        .thenReturn(List.of());

    HomeFeedResponse response = service.getHome(null, null, null, null);
    List<HomeSectionType> sectionTypes = response.getSections().stream()
        .map(HomeSectionDto::getType)
        .toList();

    assertThat(sectionTypes).containsExactly(
        HomeSectionType.PROJECT_ANALYTICS,
        HomeSectionType.NEARBY_LISTINGS,
        HomeSectionType.COMPARE_PROPERTIES,
        HomeSectionType.BUILDER_CREDIBILITY_CARDS,
        HomeSectionType.CONNECTED_BRANDS,
        HomeSectionType.TRENDING_CITIES,
        HomeSectionType.SMART_CALCULATORS,
        HomeSectionType.INSTAGRAM_REELS,
        HomeSectionType.ARCHITECTS,
        HomeSectionType.DESIGNERS
    );
  }

  @Test
  void getHomeWithoutLocationUsesRecommendedNearbyFallback() {
    PublicProjectNearbyListingService listingService = mock(PublicProjectNearbyListingService.class);
    HomeFeedServiceImpl service = homeServiceWithNearbyLoader(listingService, mock(CityRepository.class));

    when(listingService.listNearby(null, null, null, 1))
        .thenReturn(List.of(ProjectNearbyListingCardDto.builder()
            .projectId(101L)
            .distanceLabel(null)
            .build()));

    HomeSectionDto<?> nearby = nearbySection(service.getHome(HomeFeedRequest.builder().build()));

    assertThat(nearby.getTitle()).isEqualTo("Recommended Projects");
    assertThat(nearby.getSubtitle()).isEqualTo("Popular homes and projects");
    assertThat(((ProjectNearbyListingCardDto) nearby.getItems().get(0)).getDistanceLabel()).isNull();
  }

  @Test
  void getHomeWithCoordinatesUsesNearbyCopyAndDistanceLabels() {
    PublicProjectNearbyListingService listingService = mock(PublicProjectNearbyListingService.class);
    HomeFeedServiceImpl service = homeServiceWithNearbyLoader(listingService, mock(CityRepository.class));

    when(listingService.listNearby(28.6139, 77.2090, null, 1))
        .thenReturn(List.of(ProjectNearbyListingCardDto.builder()
            .projectId(101L)
            .distanceKm(1.23)
            .distanceLabel("1.2 km")
            .build()));

    HomeSectionDto<?> nearby = nearbySection(service.getHome(HomeFeedRequest.builder()
        .latitude(28.6139)
        .longitude(77.2090)
        .build()));

    assertThat(nearby.getTitle()).isEqualTo("Nearby Listings");
    assertThat(nearby.getSubtitle()).isEqualTo("Near you");
    assertThat(((ProjectNearbyListingCardDto) nearby.getItems().get(0)).getDistanceLabel()).isEqualTo("1.2 km");
  }

  @Test
  void getHomeWithCityOnlyUsesCityFallbackCopyWithoutDistanceLabels() {
    PublicProjectNearbyListingService listingService = mock(PublicProjectNearbyListingService.class);
    CityRepository cityRepository = mock(CityRepository.class);
    HomeFeedServiceImpl service = homeServiceWithNearbyLoader(listingService, cityRepository);

    when(cityRepository.findById(1L))
        .thenReturn(Optional.of(CityEntity.builder()
            .id(1L)
            .name("New Delhi")
            .slug("new-delhi")
            .countryCode("IN")
            .build()));
    when(listingService.listNearby(null, null, 1L, 1))
        .thenReturn(List.of(ProjectNearbyListingCardDto.builder()
            .projectId(101L)
            .distanceLabel(null)
            .build()));

    HomeSectionDto<?> nearby = nearbySection(service.getHome(HomeFeedRequest.builder()
        .cityId(1L)
        .build()));

    assertThat(nearby.getTitle()).isEqualTo("Projects in New Delhi");
    assertThat(nearby.getSubtitle()).isEqualTo("Popular projects in your selected city");
    assertThat(((ProjectNearbyListingCardDto) nearby.getItems().get(0)).getDistanceLabel()).isNull();
  }

  @Test
  void getHomeWithDetectedNoDataCityReturnsGlobalNearbyFeed() {
    PublicProjectNearbyListingService listingService = mock(PublicProjectNearbyListingService.class);
    HomeFeedServiceImpl service = homeServiceWithNearbyLoader(listingService, mock(CityRepository.class));

    when(listingService.listNearby(null, null, null, 1))
        .thenReturn(List.of(ProjectNearbyListingCardDto.builder()
            .projectId(303L)
            .projectName("Global Project")
            .build()));

    HomeFeedResponse response = service.getHome(HomeFeedRequest.builder()
        .deviceCity("Noida")
        .build());
    HomeSectionDto<?> nearby = nearbySection(response);

    assertThat(response.getHeader().getCityId()).isNull();
    assertThat(response.getHeader().getCityName()).isNull();
    assertThat(nearby.getTitle()).isEqualTo("Recommended Projects");
    assertThat(nearby.getSubtitle()).isEqualTo("Popular homes and projects");
    assertThat(((ProjectNearbyListingCardDto) nearby.getItems().get(0)).getProjectId()).isEqualTo(303L);
  }

  @Test
  void getHomeInjectsHeroBannerWithDisplayDuration() {
    ContentVersionRepository contentVersionRepository = mock(ContentVersionRepository.class);
    HomeSectionConfigRepository homeSectionConfigRepository = mock(HomeSectionConfigRepository.class);
    PromoBannerSlotConfigRepository promoBannerSlotConfigRepository = mock(PromoBannerSlotConfigRepository.class);
    PromoBannerService promoBannerService = mock(PromoBannerService.class);
    CityRepository cityRepository = mock(CityRepository.class);

    HomeFeedServiceImpl service = new HomeFeedServiceImpl(
        contentVersionRepository,
        homeSectionConfigRepository,
        List.of(loader(HomeSectionType.TOP_PROJECTS)),
        promoBannerSlotConfigRepository,
        promoBannerService,
        new PromoBannerInjector(),
        cityRepository
    );

    PromoBannerSlotConfigEntity heroRule = PromoBannerSlotConfigEntity.builder()
        .screen(FeedScreen.HOME)
        .slotKey("HERO")
        .maxItems(10)
        .active(true)
        .build();
    PromoBannerResponse lottieBanner = PromoBannerResponse.builder()
        .id(999L)
        .title("Compare Smarter")
        .subtitle("Compare projects side by side")
        .imageUrl(null)
        .mediaType("LOTTIE_JSON")
        .mediaUrl(LOTTIE_URL)
        .targetUrl("/compare-projects/select")
        .priority(2)
        .active(true)
        .displayDurationMs(12000)
        .build();

    when(contentVersionRepository.findById("HOME:ALL")).thenReturn(Optional.empty());
    when(homeSectionConfigRepository.findByHomeCategory_IdAndEnabledTrueOrderBySortOrderAscIdAsc(0L))
        .thenReturn(List.of(config(HomeSectionType.TOP_PROJECTS)));
    when(promoBannerSlotConfigRepository
        .findByScreenAndHomeCategory_IdAndActiveTrueOrderByPriorityAscIdAsc(any(), eq(0L)))
        .thenReturn(List.of(heroRule));
    // when(promoBannerService.getBannersForCategoryAndSlot(0L, "HERO", 10))
    //     .thenReturn(List.of(lottieBanner));

    HomeFeedResponse response = service.getHome(null, null, null, null);

    HomeSectionDto<?> heroSection = response.getSections().stream()
        .filter(section -> section.getType() == HomeSectionType.PROMO_BANNERS)
        .findFirst()
        .orElseThrow();
    PromoBannerResponse heroItem = (PromoBannerResponse) heroSection.getItems().get(0);

    assertThat(heroSection.getKey()).isEqualTo("HERO");
    assertThat(heroItem.getMediaType()).isEqualTo("LOTTIE_JSON");
    assertThat(heroItem.getMediaUrl()).isEqualTo(LOTTIE_URL);
    assertThat(heroItem.getTargetUrl()).isEqualTo("/compare-projects/select");
    assertThat(heroItem.getDisplayDurationMs()).isEqualTo(12000);
    assertThat(response.getSections()).extracting(HomeSectionDto::getType)
        .contains(HomeSectionType.TOP_PROJECTS);
  }

  
    ContentVersionRepository contentVersionRepository = mock(ContentVersionRepository.class);
    HomeSectionConfigRepository homeSectionConfigRepository = mock(HomeSectionConfigRepository.class);
    PromoBannerSlotConfigRepository promoBannerSlotConfigRepository = mock(PromoBannerSlotConfigRepository.class);
    PromoBannerService promoBannerService = mock(PromoBannerService.class);
    CityRepository cityRepository = mock(CityRepository.class);

    // Reproduces the real duplicate-HERO scenario: a home_section_config PROMO_BANNERS/HERO
    // row (loader-driven, max_items=4) coexists with a promo_banner_slot_config HERO rule
    // (injector-driven, max_items=1) for the same category/screen.
    HomeSectionConfigEntity heroConfig = HomeSectionConfigEntity.builder()
        .sectionType(HomeSectionType.PROMO_BANNERS)
        .maxItems(4)
        .build();
    HomeSectionConfigEntity midConfigPlaceholder = config(HomeSectionType.TOP_PROJECTS);

    PromoBannerSlotConfigEntity heroRule = PromoBannerSlotConfigEntity.builder()
        .screen(FeedScreen.HOME)
        .slotKey("HERO")
        .maxItems(1)
        .active(true)
        .build();
    PromoBannerSlotConfigEntity midRule = PromoBannerSlotConfigEntity.builder()
        .screen(FeedScreen.HOME)
        .slotKey("MID")
        .insertAfterSectionType("TOP_PROJECTS")
        .maxItems(10)
        .active(true)
        .build();

    // PromoBannerResponse banner25 = bannerResponse(25L, 1);
    PromoBannerResponse banner29 = bannerResponse(29L, 2);
    // PromoBannerResponse banner27 = bannerResponse(27L, 3);
    // PromoBannerResponse banner28 = bannerResponse(28L, 4);
    PromoBannerResponse midBanner21 = bannerResponse(21L, 1);

    HomeFeedServiceImpl service = new HomeFeedServiceImpl(
        contentVersionRepository,
        homeSectionConfigRepository,
        List.of(new PromoBannersSectionLoader(promoBannerService), loader(HomeSectionType.TOP_PROJECTS)),
        promoBannerSlotConfigRepository,
        promoBannerService,
        new PromoBannerInjector(),
        cityRepository
    );

    @Test
void getHomeSkipsGlobalPromoBannerConfigAndUsesSlotConfigHero() {
  ContentVersionRepository contentVersionRepository = mock(ContentVersionRepository.class);
  HomeSectionConfigRepository homeSectionConfigRepository = mock(HomeSectionConfigRepository.class);
  PromoBannerSlotConfigRepository promoBannerSlotConfigRepository = mock(PromoBannerSlotConfigRepository.class);
  PromoBannerService promoBannerService = mock(PromoBannerService.class);
  CityRepository cityRepository = mock(CityRepository.class);

  HomeSectionConfigEntity heroConfig = HomeSectionConfigEntity.builder()
      .sectionType(HomeSectionType.PROMO_BANNERS)
      .maxItems(4)
      .build();
  HomeSectionConfigEntity midConfigPlaceholder = config(HomeSectionType.TOP_PROJECTS);

  PromoBannerSlotConfigEntity heroRule = PromoBannerSlotConfigEntity.builder()
      .screen(FeedScreen.HOME)
      .slotKey("HERO")
      .maxItems(1)
      .active(true)
      .build();
  PromoBannerSlotConfigEntity midRule = PromoBannerSlotConfigEntity.builder()
      .screen(FeedScreen.HOME)
      .slotKey("MID")
      .insertAfterSectionType("TOP_PROJECTS")
      .maxItems(10)
      .active(true)
      .build();

  PromoBannerResponse banner25 = bannerResponse(25L, 1);
  PromoBannerResponse midBanner21 = bannerResponse(21L, 1);

  HomeFeedServiceImpl service = new HomeFeedServiceImpl(
      contentVersionRepository,
      homeSectionConfigRepository,
      List.of(new PromoBannersSectionLoader(promoBannerService), loader(HomeSectionType.TOP_PROJECTS)),
      promoBannerSlotConfigRepository,
      promoBannerService,
      new PromoBannerInjector(),
      cityRepository
  );

  when(contentVersionRepository.findById("HOME:ALL")).thenReturn(Optional.empty());
  when(homeSectionConfigRepository.findByHomeCategory_IdAndEnabledTrueOrderBySortOrderAscIdAsc(0L))
      .thenReturn(List.of(heroConfig, midConfigPlaceholder));
  when(promoBannerSlotConfigRepository
      .findByScreenAndHomeCategory_IdAndActiveTrueOrderByPriorityAscIdAsc(any(), eq(0L)))
      .thenReturn(List.of(heroRule, midRule));

  when(promoBannerService.getBannersForCategoryAndSlot(0L, "HERO", 1))
      .thenReturn(List.of(banner25));
  when(promoBannerService.getBannersForCategoryAndSlot(0L, "MID", 10))
      .thenReturn(List.of(midBanner21));

  HomeFeedResponse response = service.getHome(null, null, null, null);

  List<HomeSectionDto<?>> heroSections = response.getSections().stream()
      .filter(section -> section.getType() == HomeSectionType.PROMO_BANNERS)
      .filter(section -> "HERO".equals(section.getKey()))
      .toList();
  List<HomeSectionDto<?>> midSections = response.getSections().stream()
      .filter(section -> section.getType() == HomeSectionType.PROMO_BANNERS)
      .filter(section -> "MID".equals(section.getKey()))
      .toList();

  assertThat(heroSections).hasSize(1);
  assertThat(heroSections.get(0).getItems())
      .extracting(item -> ((PromoBannerResponse) item).getId())
      .containsExactly(25L);

  assertThat(midSections).hasSize(1);
  assertThat(midSections.get(0).getItems())
      .extracting(item -> ((PromoBannerResponse) item).getId())
      .containsExactly(21L);
}

  private static PromoBannerResponse bannerResponse(Long id, int priority) {
    return PromoBannerResponse.builder()
        .id(id)
        .priority(priority)
        .active(true)
        .build();
  }

  private static HomeSectionConfigEntity config(HomeSectionType type) {
    return HomeSectionConfigEntity.builder()
        .sectionType(type)
        .title(type.name())
        .maxItems(1)
        .build();
  }

  private static HomeFeedServiceImpl homeServiceWithNearbyLoader(
      PublicProjectNearbyListingService listingService,
      CityRepository cityRepository
  ) {
    ContentVersionRepository contentVersionRepository = mock(ContentVersionRepository.class);
    HomeSectionConfigRepository homeSectionConfigRepository = mock(HomeSectionConfigRepository.class);
    PromoBannerSlotConfigRepository promoBannerSlotConfigRepository = mock(PromoBannerSlotConfigRepository.class);
    PromoBannerService promoBannerService = mock(PromoBannerService.class);

    when(contentVersionRepository.findById("HOME:ALL")).thenReturn(Optional.empty());
    when(homeSectionConfigRepository.findByHomeCategory_IdAndEnabledTrueOrderBySortOrderAscIdAsc(0L))
        .thenReturn(List.of(
            config(HomeSectionType.PROJECT_ANALYTICS),
            config(HomeSectionType.NEARBY_LISTINGS),
            config(HomeSectionType.TRENDING_CITIES)
        ));
    when(promoBannerSlotConfigRepository
        .findByScreenAndHomeCategory_IdAndActiveTrueOrderByPriorityAscIdAsc(any(), eq(0L)))
        .thenReturn(List.of());

    return new HomeFeedServiceImpl(
        contentVersionRepository,
        homeSectionConfigRepository,
        List.of(
            loader(HomeSectionType.PROJECT_ANALYTICS),
            new NearbyListingsSectionLoader(listingService),
            loader(HomeSectionType.TRENDING_CITIES)
        ),
        promoBannerSlotConfigRepository,
        promoBannerService,
        new PromoBannerInjector(),
        cityRepository
    );
  }

  private static HomeSectionDto<?> nearbySection(HomeFeedResponse response) {
    List<HomeSectionType> sectionTypes = response.getSections().stream()
        .map(HomeSectionDto::getType)
        .toList();

    assertThat(sectionTypes).containsSubsequence(
        HomeSectionType.PROJECT_ANALYTICS,
        HomeSectionType.NEARBY_LISTINGS,
        HomeSectionType.TRENDING_CITIES
    );

    return response.getSections().stream()
        .filter(section -> section.getType() == HomeSectionType.NEARBY_LISTINGS)
        .findFirst()
        .orElseThrow();
  }

  private static HomeSectionLoader loader(HomeSectionType type) {
    return new HomeSectionLoader() {
      @Override
      public HomeSectionType supports() {
        return type;
      }

      @Override
      public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {
        return HomeSectionDto.<String>builder()
            .type(type)
            .key(type == HomeSectionType.TRENDING_CITIES ? "TRENDING_CITIES" : null)
            .title(cfg.getTitle())
            .items(List.of(type.name() + "_ITEM"))
            .build();
      }
    };
  }
}
