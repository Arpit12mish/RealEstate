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
import com.brandPitara.sfs.service.PromoBannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeFeedServiceImpl implements HomeFeedService {

  private final ContentVersionRepository contentVersionRepository;
  private final HomeSectionConfigRepository homeSectionConfigRepository;
  private final List<HomeSectionLoader> loaders;

  private final PromoBannerSlotConfigRepository promoBannerSlotConfigRepository;
  private final PromoBannerService promoBannerService;
  private final PromoBannerInjector promoBannerInjector;

  @Override
  public HomeFeedResponse getHome(Long cityId, Long categoryId, Long builderId, Long clientVersion) {

    boolean isAll = (categoryId == null || categoryId == 0);
    Long homeCategoryId = isAll ? 0L : categoryId;

    String key = isAll ? "HOME:ALL" : "HOME:" + categoryId;

    long version = contentVersionRepository.findById(key)
        .map(ContentVersionEntity::getVersion)
        .orElse(1L);

    var configs = homeSectionConfigRepository
        .findByHomeCategory_IdAndEnabledTrueOrderBySortOrderAscIdAsc(homeCategoryId);

    Map<HomeSectionType, HomeSectionLoader> loaderMap = new EnumMap<>(HomeSectionType.class);
    for (HomeSectionLoader l : loaders) loaderMap.put(l.supports(), l);

    SectionContext ctx = SectionContext.builder()
        .cityId(cityId)
        .categoryId(homeCategoryId)
        .builderId(builderId)
        .build();

    List<HomeSectionDto<?>> sections = new ArrayList<>();

    for (var cfg : configs) {
      var loader = loaderMap.get(cfg.getSectionType());
      if (loader == null) continue;

      HomeSectionDto<?> section = loader.load(cfg, ctx);
      if (section != null && section.getItems() != null && !section.getItems().isEmpty()) {
        sections.add(section);
      }
    }

    // ✅ Inject promo banners via slot config rules
    List<PromoBannerSlotConfigEntity> rules = loadPromoRules(FeedScreen.HOME, homeCategoryId, cityId);

    sections = promoBannerInjector.inject(
        sections,
        rules,
        rule -> {
          String slotKey = rule.getSlotKey(); // HERO / MID

          var items = promoBannerService.getBannersForCategoryAndSlot(
              homeCategoryId,
              slotKey,
              rule.getMaxItems()
          );

          // ✅ Step 6: HERO is required -> log error if missing
          if (items == null || items.isEmpty()) {
            if ("HERO".equalsIgnoreCase(slotKey)) {
              log.error("HERO banner missing for screen={} category={} city={}",
                  FeedScreen.HOME, homeCategoryId, cityId);
            }
            return null;
          }

          // ✅ Step 4: set section.key = slotKey so frontend can render big vs normal
          return HomeSectionDto.<PromoBannerResponse>builder()
              .type(HomeSectionType.PROMO_BANNERS)
              .key(slotKey) // ✅ HERO / MID
              .title(null)
              .items(items)
              .build();
        }
    );

    return HomeFeedResponse.builder()
        .version(version)
        .header(HomeHeaderDto.builder()
            .cityId(cityId)
            .cityName(null)
            .sentences(List.of("Verified professionals", "Trusted brands near you"))
            .build())
        .sections(sections)
        .build();
  }

  private List<PromoBannerSlotConfigEntity> loadPromoRules(FeedScreen screen, Long homeCategoryId, Long cityId) {
    if (cityId != null) {
      var cityRules = promoBannerSlotConfigRepository
          .findByScreenAndHomeCategory_IdAndCity_IdAndActiveTrueOrderByPriorityAscIdAsc(screen, homeCategoryId, cityId);
      if (cityRules != null && !cityRules.isEmpty()) return cityRules;
    }
    return promoBannerSlotConfigRepository
        .findByScreenAndHomeCategory_IdAndActiveTrueOrderByPriorityAscIdAsc(screen, homeCategoryId);
  }
}