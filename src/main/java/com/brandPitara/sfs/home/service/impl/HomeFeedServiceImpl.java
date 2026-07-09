// HomeFeedServiceImpl.java
package com.brandPitara.sfs.home.service.impl;

import com.brandPitara.sfs.common.contentVersion.entity.ContentVersionEntity;
import com.brandPitara.sfs.common.contentVersion.repository.ContentVersionRepository;
import com.brandPitara.sfs.dto.PromoBannerResponse;
import com.brandPitara.sfs.feed.enums.FeedScreen;
import com.brandPitara.sfs.home.dto.HomeFeedRequest;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeFeedServiceImpl implements HomeFeedService {

  private static final Long GLOBAL_CITY_ID = 0L;
  private static final Long GLOBAL_HOME_CATEGORY_ID = 0L;
  private static final int DEFAULT_PROMO_MAX_ITEMS = 10;

  /**
   * Canonical Home API section order. Keyed by HomeSectionType#name() so a not-yet-implemented
   * type (QUICK_SQUARE) can be pre-registered without needing the enum constant to exist yet.
   * Types absent from this map (legacy/overlapping sections such as TOP_PROJECTS, TOP_BUILDERS)
   * sort after every canonical section, in their original relative order - see
   * docs/home-api-section-order-audit.md for the full rationale.
   */
  private static final Map<String, Integer> HOME_SECTION_ORDER = Map.ofEntries(
      Map.entry("TRENDING_PROPERTIES", 10),
      Map.entry("PROJECT_ANALYTICS", 10),
      Map.entry("NEARBY_LISTINGS", 20),
      Map.entry("COMPARE_PROPERTIES", 30),
      Map.entry("BUILDER_CREDIBILITY_CARDS", 40),
      Map.entry("CONNECTED_BRANDS", 50),
      Map.entry("TRENDING_CITIES", 60),
      Map.entry("SMART_CALCULATORS", 70),
      Map.entry("INSTAGRAM_REELS", 80),
      Map.entry("QUICK_SQUARE", 90),
      Map.entry("ARCHITECTS", 100),
      Map.entry("DESIGNERS", 110)
  );

  private final ContentVersionRepository contentVersionRepository;
  private final HomeSectionConfigRepository homeSectionConfigRepository;
  private final List<HomeSectionLoader> loaders;

  private final PromoBannerSlotConfigRepository promoBannerSlotConfigRepository;
  private final PromoBannerService promoBannerService;
  private final PromoBannerInjector promoBannerInjector;
  private final CityRepository cityRepository;

  @Override
  @Transactional(readOnly = true)
  public HomeFeedResponse getHome(
      Long cityId,
      Long categoryId,
      Long builderId,
      Long clientVersion
  ) {
    return getHome(HomeFeedRequest.builder()
        .cityId(cityId)
        .categoryId(categoryId)
        .builderId(builderId)
        .clientVersion(clientVersion)
        .build());
  }

  @Override
  @Transactional(readOnly = true)
  public HomeFeedResponse getHome(HomeFeedRequest request) {
    HomeFeedRequest safeRequest = request == null ? HomeFeedRequest.builder().build() : request;

    /*
     * cityId behavior:
     * cityId > 0  => city-specific feed
     * cityId = 0  => global/all-available feed
     * cityId null => global/all-available feed
     *
     * Do NOT store "Global" as a real city row.
     */
    Long resolvedCityId = normalizeCityId(safeRequest.getCityId());
    Double latitude = normalizeLatitude(safeRequest.getLatitude());
    Double longitude = normalizeLongitude(safeRequest.getLongitude());

    if ((latitude == null) != (longitude == null)) {
      latitude = null;
      longitude = null;
    }

    boolean hasUserCoordinates = latitude != null && longitude != null;

    boolean isAllCategory =
        safeRequest.getCategoryId() == null || safeRequest.getCategoryId() == 0;

    Long homeCategoryId = isAllCategory
        ? GLOBAL_HOME_CATEGORY_ID
        : safeRequest.getCategoryId();

    String contentVersionKey = isAllCategory
        ? "HOME:ALL"
        : "HOME:" + safeRequest.getCategoryId();

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

    String resolvedCityName = resolveCityName(resolvedCityId);

    SectionContext ctx = SectionContext.builder()
        .cityId(resolvedCityId)
        .categoryId(homeCategoryId)
        .builderId(safeRequest.getBuilderId())
        .latitude(latitude)
        .longitude(longitude)
        .accuracyMeters(safeRequest.getAccuracyMeters())
        .deviceCity(clean(safeRequest.getDeviceCity()))
        .resolvedCityName(resolvedCityName)
        .hasUserCoordinates(hasUserCoordinates)
        .build();

    List<HomeSectionDto<?>> sections = new ArrayList<>();

    for (var cfg : configs) {
      if (isGlobalPromoBannerConfig(homeCategoryId, cfg.getSectionType())) {
        log.warn(
            "Skipping global PROMO_BANNERS configId={} because global promo placements are controlled by promo_banner_slot_config",
            cfg.getId()
        );
        continue;
      }

      HomeSectionLoader loader = loaderMap.get(cfg.getSectionType());

      if (loader == null) {
        log.warn(
            "No HomeSectionLoader found for sectionType={} configId={}",
            cfg.getSectionType(),
            cfg.getId()
        );
        continue;
      }

      long startNanos = System.nanoTime();
      HomeSectionDto<?> section;

      try {
        section = loader.load(cfg, ctx);
      } catch (Exception ex) {
        log.error(
            "Home section failed sectionType={} configId={}",
            cfg.getSectionType(),
            cfg.getId(),
            ex
        );
        continue;
      } finally {
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        log.debug(
            "Home section loaded sectionType={} configId={} elapsedMs={}",
            cfg.getSectionType(),
            cfg.getId(),
            elapsedMs
        );
      }

      if (shouldIncludeSection(section)) {
        sections.add(section);
      }
    }

    sections = sortHomeSections(sections);

    List<PromoBannerSlotConfigEntity> rules =
        loadPromoRules(FeedScreen.HOME, homeCategoryId, resolvedCityId);

    Long promoResolvedCityId = resolvedCityId;

    sections = promoBannerInjector.inject(
        sections,
        rules,
        rule -> {
          String slotKey = normalizePromoSlotKey(rule.getSlotKey());
          Integer maxItems = normalizePromoMaxItems(rule.getMaxItems());

          List<PromoBannerResponse> items =
              promoBannerService.getBannersForCategoryAndSlot(
                  homeCategoryId,
                  slotKey,
                  maxItems
              );

          if (items == null || items.isEmpty()) {
            if ("HERO".equalsIgnoreCase(slotKey)) {
              log.error(
                  "HERO banner missing for screen={} category={} requestedCityId={} resolvedCityId={}",
                  FeedScreen.HOME,
                  homeCategoryId,
                  safeRequest.getCityId(),
                  promoResolvedCityId
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

    return HomeFeedResponse.builder()
        .version(version)
        .header(
            HomeHeaderDto.builder()
                .cityId(resolvedCityId)
                .cityName(resolvedCityName)
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

  /**
   * Applies the canonical Home API section order (see HOME_SECTION_ORDER). Stable sort: types
   * absent from the map keep their existing relative order and trail after every canonical
   * section. Must run before promo banner injection so banner anchor rules
   * (insertAfterSectionType) resolve against the final, correctly-ordered content list.
   */
  static List<HomeSectionDto<?>> sortHomeSections(List<HomeSectionDto<?>> sections) {
    if (sections == null || sections.size() < 2) {
      return sections;
    }

    List<HomeSectionDto<?>> ordered = new ArrayList<>(sections);
    ordered.sort(Comparator.comparingInt(HomeFeedServiceImpl::homeSectionRank));
    return ordered;
  }

  private static int homeSectionRank(HomeSectionDto<?> section) {
    if (section == null || section.getType() == null) {
      return Integer.MAX_VALUE;
    }

    return HOME_SECTION_ORDER.getOrDefault(section.getType().name(), Integer.MAX_VALUE);
  }

  private boolean isGlobalPromoBannerConfig(
      Long homeCategoryId,
      HomeSectionType sectionType
  ) {
    return GLOBAL_HOME_CATEGORY_ID.equals(homeCategoryId)
        && sectionType == HomeSectionType.PROMO_BANNERS;
  }

  private boolean shouldIncludeSection(HomeSectionDto<?> section) {
    if (section == null) {
      return false;
    }

    if (section.getType() == HomeSectionType.NEARBY_LISTINGS) {
      return section.getItems() != null;
    }

    return section.getItems() != null && !section.getItems().isEmpty();
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

  private Double normalizeLatitude(Double latitude) {
    if (latitude == null || latitude < -90.0 || latitude > 90.0) {
      return null;
    }

    return latitude;
  }

  private Double normalizeLongitude(Double longitude) {
    if (longitude == null || longitude < -180.0 || longitude > 180.0) {
      return null;
    }

    return longitude;
  }

  private String normalizePromoSlotKey(String slotKey) {
    return hasText(slotKey) ? slotKey.trim() : "HERO";
  }

  private Integer normalizePromoMaxItems(Integer maxItems) {
    if (maxItems == null || maxItems <= 0) {
      return DEFAULT_PROMO_MAX_ITEMS;
    }

    return maxItems;
  }

  private static boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }

  private String clean(String value) {
    return hasText(value) ? value.trim() : null;
  }
}