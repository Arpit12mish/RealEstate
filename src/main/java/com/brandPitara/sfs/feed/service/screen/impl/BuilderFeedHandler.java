package com.brandPitara.sfs.feed.service.screen.impl;

import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.dto.PromoBannerResponse;
import com.brandPitara.sfs.feed.entity.FeedSectionConfigEntity;
import com.brandPitara.sfs.feed.enums.FeedScreen;
import com.brandPitara.sfs.feed.repository.FeedSectionConfigRepository;
import com.brandPitara.sfs.feed.service.screen.FeedScreenHandler;
import com.brandPitara.sfs.feed.service.section.FeedContext;
import com.brandPitara.sfs.feed.service.section.FeedSectionLoader;
import com.brandPitara.sfs.home.dto.HomeFeedResponse;
import com.brandPitara.sfs.home.dto.HomeHeaderDto;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.PromoBannerSlotConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.repository.PromoBannerSlotConfigRepository;
import com.brandPitara.sfs.home.service.impl.PromoBannerInjector;
import com.brandPitara.sfs.service.PromoBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class BuilderFeedHandler implements FeedScreenHandler {

  private final BuilderRepository builderRepository;

  private final FeedSectionConfigRepository feedSectionConfigRepository;
  private final List<FeedSectionLoader> loaders;

  private final PromoBannerSlotConfigRepository promoBannerSlotConfigRepository;
  private final PromoBannerService promoBannerService;
  private final PromoBannerInjector promoBannerInjector;

  @Override
  public FeedScreen supports() {
    return FeedScreen.BUILDER;
  }

  @Override
  public HomeFeedResponse build(Long entityId, Long cityId, Long categoryId, Long clientVersion) {
    if (entityId == null) {
      throw new IllegalArgumentException("entityId is required for BUILDER screen");
    }

    // ✅ DEBUG: list injected loaders
    System.out.println("===========================================");
    System.out.println("✅ LOADERS => " + loaders.stream()
        .map(l -> l.supports() + ":" + l.getClass().getSimpleName())
        .toList());
    System.out.println("===========================================");

    var builder = builderRepository.findByIdAndDeletedFalse(entityId)
        .orElseThrow(() -> new IllegalArgumentException("Builder not found: " + entityId));

    List<FeedSectionConfigEntity> configs = feed_section_configs_for_builder(entityId);

    // ✅ DEBUG: configs fetched
    System.out.println("✅ BUILDER CONFIGS => " + (configs == null ? "null" :
        configs.stream().map(c -> c.getSectionType() + "#" + c.getId() + "(sort=" + c.getSortOrder() + ")").toList()
    ));

    // Build loader map by sectionType string
    Map<String, FeedSectionLoader> loaderMap = new HashMap<>();
    for (FeedSectionLoader l : loaders) loaderMap.put(l.supports(), l);

    FeedContext ctx = FeedContext.builder()
        .screen(FeedScreen.BUILDER.name())
        .entityId(entityId)
        .cityId(cityId)
        .categoryId(categoryId)
        .clientVersion(clientVersion)
        .build();

    List<HomeSectionDto<?>> sections = new ArrayList<>();

    if (configs != null) {
      for (var cfg : configs) {
        FeedSectionLoader loader = loaderMap.get(cfg.getSectionType());

        // ✅ DEBUG: which loader matched
        System.out.println("➡️ CFG=" + cfg.getSectionType()
            + " loader=" + (loader == null ? "NULL" : loader.getClass().getSimpleName()));

        if (loader == null) continue;

        HomeSectionDto<?> section = loader.load(cfg, ctx);

        // ✅ DEBUG: section output count
        int count = (section == null || section.getItems() == null) ? -1 : section.getItems().size();
        System.out.println("⬅️ SECTION=" + cfg.getSectionType() + " items=" + count
            + " key=" + (section == null ? "null" : section.getKey())
            + " type=" + (section == null ? "null" : section.getType()));

        if (section != null && section.getItems() != null && !section.getItems().isEmpty()) {
          sections.add(section);
        }
      }
    }

    List<PromoBannerSlotConfigEntity> promoRules = loadPromoRulesForBuilder(entityId, cityId);

    sections = promoBannerInjector.inject(
        sections,
        promoRules,
        rule -> {
          var items = promoBannerService.getBannersForCategoryAndSlot(
              0L,
              rule.getSlotKey(),
              rule.getMaxItems()
          );
          if (items == null || items.isEmpty()) return null;

          return HomeSectionDto.<PromoBannerResponse>builder()
              .key("PROMO_" + rule.getSlotKey())
              .type(HomeSectionType.PROMO_BANNERS)
              .title(null)
              .items(items)
              .build();
        }
    );

    HomeHeaderDto header = HomeHeaderDto.builder()
        .cityId(cityId)
        .cityName(builder.getCity() == null ? null : builder.getCity().getName())
        .sentences(List.of(builder.getName(), "Shaping dreams into masterpieces"))
        .build();

    return HomeFeedResponse.builder()
        .version(1L)
        .header(header)
        .sections(sections)
        .build();
  }

  // keep logic same, just moved into a method for clean logging
  private List<FeedSectionConfigEntity> feed_section_configs_for_builder(Long entityId) {
    List<FeedSectionConfigEntity> configs = feedSectionConfigRepository
        .findByScreenAndEntityIdAndEnabledTrueOrderBySortOrderAscIdAsc(FeedScreen.BUILDER.name(), entityId);

    if (configs == null || configs.isEmpty()) {
      configs = feedSectionConfigRepository
          .findByScreenAndEnabledTrueOrderBySortOrderAscIdAsc(FeedScreen.BUILDER.name());
    }
    return configs;
  }

  private List<PromoBannerSlotConfigEntity> promo_banner_slot_fallback(Long homeCategoryId) {
    return promoBannerSlotConfigRepository
        .findByScreenAndHomeCategory_IdAndActiveTrueOrderByPriorityAscIdAsc(
            com.brandPitara.sfs.feed.enums.FeedScreen.BUILDER,
            homeCategoryId
        );
  }

  private List<PromoBannerSlotConfigEntity> loadPromoRulesForBuilder(Long builderId, Long cityId) {
    Long homeCategoryId = 0L;

    if (cityId != null) {
      var cityRules = promoBannerSlotConfigRepository
          .findByScreenAndHomeCategory_IdAndCity_IdAndActiveTrueOrderByPriorityAscIdAsc(
              com.brandPitara.sfs.feed.enums.FeedScreen.BUILDER,
              homeCategoryId,
              cityId
          );
      if (cityRules != null && !cityRules.isEmpty()) return cityRules;
    }

    return promo_banner_slot_fallback(homeCategoryId);
  }

  
}