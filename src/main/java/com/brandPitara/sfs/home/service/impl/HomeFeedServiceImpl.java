// HomeFeedServiceImpl.java
package com.brandPitara.sfs.home.service.impl;

import com.brandPitara.sfs.common.contentVersion.entity.ContentVersionEntity;
import com.brandPitara.sfs.common.contentVersion.repository.ContentVersionRepository;
import com.brandPitara.sfs.dto.PromoBannerResponse;
import com.brandPitara.sfs.feed.enums.FeedScreen;
import com.brandPitara.sfs.home.dto.HomeFeedResponse;
import com.brandPitara.sfs.home.dto.HomeHeaderDto;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.PromoBannerSlotConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.repository.HomeSectionConfigRepository;
import com.brandPitara.sfs.home.repository.PromoBannerSlotConfigRepository;
import com.brandPitara.sfs.home.service.HomeFeedService;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.repository.CityRepository;
import com.brandPitara.sfs.service.PromoBannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeFeedServiceImpl implements HomeFeedService {

  private static final Long GLOBAL_CITY_ID = 0L;

  private final ContentVersionRepository contentVersionRepository;
  private final HomeSectionConfigRepository homeSectionConfigRepository;
  private final List<HomeSectionLoader> loaders;

  private final PromoBannerSlotConfigRepository promoBannerSlotConfigRepository;
  private final PromoBannerService promoBannerService;
  private final PromoBannerInjector promoBannerInjector;
  private final CityRepository cityRepository;

  @Override
  public HomeFeedResponse getHome(
      Long cityId,
      Long categoryId,
      Long builderId,
      Long clientVersion
  ) {

    /*
     * cityId behavior:
     * cityId > 0  => city-specific feed
     * cityId = 0  => global/all-available feed
     * cityId null => global/all-available feed
     *
     * Do NOT store "Global" as a real city row.
     */
    Long resolvedCityId = normalizeCityId(cityId);

    boolean isAllCategory = categoryId == null || categoryId == 0;
    Long homeCategoryId = isAllCategory ? 0L : categoryId;

    String contentVersionKey = isAllCategory ? "HOME:ALL" : "HOME:" + categoryId;

    long version = contentVersionRepository.findById(contentVersionKey)
        .map(ContentVersionEntity::getVersion)
        .orElse(1L);

    var configs = homeSectionConfigRepository
        .findByHomeCategory_IdAndEnabledTrueOrderBySortOrderAscIdAsc(homeCategoryId);

    Map<HomeSectionType, HomeSectionLoader> loaderMap =
        new EnumMap<>(HomeSectionType.class);

    for (HomeSectionLoader loader : loaders) {
      loaderMap.put(loader.supports(), loader);
    }

    SectionContext ctx = SectionContext.builder()
        .cityId(resolvedCityId)
        .categoryId(homeCategoryId)
        .builderId(builderId)
        .build();

    List<HomeSectionDto<?>> sections = new ArrayList<>();

    for (var cfg : configs) {
      HomeSectionLoader loader = loaderMap.get(cfg.getSectionType());

      if (loader == null) {
        log.warn(
            "No HomeSectionLoader found for sectionType={} configId={}",
            cfg.getSectionType(),
            cfg.getId()
        );
        continue;
      }

      HomeSectionDto<?> section = loader.load(cfg, ctx);

      if (
          section != null
              && section.getItems() != null
              && !section.getItems().isEmpty()
      ) {
        sections.add(section);
      }
    }

    List<PromoBannerSlotConfigEntity> rules =
        loadPromoRules(FeedScreen.HOME, homeCategoryId, resolvedCityId);

    sections = promoBannerInjector.inject(
        sections,
        rules,
        rule -> {
          String slotKey = rule.getSlotKey();

          List<PromoBannerResponse> items =
              promoBannerService.getBannersForCategoryAndSlot(
                  homeCategoryId,
                  slotKey,
                  rule.getMaxItems()
              );

          if (items == null || items.isEmpty()) {
            if ("HERO".equalsIgnoreCase(slotKey)) {
              log.error(
                  "HERO banner missing for screen={} category={} requestedCityId={} resolvedCityId={}",
                  FeedScreen.HOME,
                  homeCategoryId,
                  cityId,
                  resolvedCityId
              );
            }

            return null;
          }

          return HomeSectionDto.<PromoBannerResponse>builder()
              .type(HomeSectionType.PROMO_BANNERS)
              .key(slotKey)
              .title(null)
              .items(items)
              .build();
        }
    );

    sections = moveTrendingCitiesAfterProjectAnalytics(sections);

    String cityName = resolveCityName(resolvedCityId);

    return HomeFeedResponse.builder()
        .version(version)
        .header(
            HomeHeaderDto.builder()
                .cityId(resolvedCityId)
                .cityName(cityName)
                .sentences(List.of("Verified professionals", "Trusted brands near you"))
                .build()
        )
        .sections(sections)
        .build();
  }

  private Long normalizeCityId(Long cityId) {
    if (cityId == null) {
      return null;
    }

    if (cityId <= GLOBAL_CITY_ID) {
      return null;
    }

    return cityId;
  }

  private List<PromoBannerSlotConfigEntity> loadPromoRules(
      FeedScreen screen,
      Long homeCategoryId,
      Long resolvedCityId
  ) {
    if (resolvedCityId != null) {
      List<PromoBannerSlotConfigEntity> cityRules =
          promoBannerSlotConfigRepository
              .findByScreenAndHomeCategory_IdAndCity_IdAndActiveTrueOrderByPriorityAscIdAsc(
                  screen,
                  homeCategoryId,
                  resolvedCityId
              );

      if (cityRules != null && !cityRules.isEmpty()) {
        return cityRules;
      }
    }

    return promoBannerSlotConfigRepository
        .findByScreenAndHomeCategory_IdAndActiveTrueOrderByPriorityAscIdAsc(
            screen,
            homeCategoryId
        );
  }

  static List<HomeSectionDto<?>> moveTrendingCitiesAfterProjectAnalytics(List<HomeSectionDto<?>> sections) {
    if (sections == null || sections.isEmpty()) {
      return sections;
    }

    int projectAnalyticsIndex = indexOfSectionType(sections, HomeSectionType.PROJECT_ANALYTICS);
    int trendingCitiesIndex = indexOfSectionType(sections, HomeSectionType.TRENDING_CITIES);

    if (
        projectAnalyticsIndex < 0
            || trendingCitiesIndex < 0
            || trendingCitiesIndex == projectAnalyticsIndex + 1
    ) {
      return sections;
    }

    List<HomeSectionDto<?>> ordered = new ArrayList<>(sections);
    HomeSectionDto<?> trendingCities = ordered.remove(trendingCitiesIndex);
    int insertAfterIndex = indexOfSectionType(ordered, HomeSectionType.PROJECT_ANALYTICS);
    ordered.add(insertAfterIndex + 1, trendingCities);
    return ordered;
  }

  private static int indexOfSectionType(
      List<HomeSectionDto<?>> sections,
      HomeSectionType sectionType
  ) {
    for (int i = 0; i < sections.size(); i++) {
      HomeSectionDto<?> section = sections.get(i);
      if (section != null && section.getType() == sectionType) {
        return i;
      }
    }

    return -1;
  }

  private String resolveCityName(Long resolvedCityId) {
    if (resolvedCityId == null) {
      return null;
    }

    return cityRepository.findById(resolvedCityId)
        .map(city -> {
          if (city.getName() == null || city.getName().isBlank()) {
            return null;
          }

          return city.getName().trim();
        })
        .orElse(null);
  }
}
