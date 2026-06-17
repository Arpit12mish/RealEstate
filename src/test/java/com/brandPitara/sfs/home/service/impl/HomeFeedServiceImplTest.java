package com.brandPitara.sfs.home.service.impl;

import com.brandPitara.sfs.common.contentVersion.repository.ContentVersionRepository;
import com.brandPitara.sfs.home.dto.HomeFeedResponse;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.repository.HomeSectionConfigRepository;
import com.brandPitara.sfs.home.repository.PromoBannerSlotConfigRepository;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.repository.CityRepository;
import com.brandPitara.sfs.service.PromoBannerService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeFeedServiceImplTest {

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

    assertThat(sectionTypes).containsExactly(
        HomeSectionType.PROJECT_ANALYTICS,
        HomeSectionType.TRENDING_CITIES,
        HomeSectionType.TOP_PROJECTS,
        HomeSectionType.ARCHITECTS,
        HomeSectionType.DESIGNERS,
        HomeSectionType.TOP_BUILDERS
    );
    assertThat(sectionTypes).contains(HomeSectionType.PROJECT_ANALYTICS, HomeSectionType.TOP_PROJECTS);
    assertThat(sectionTypes).filteredOn(type -> type == HomeSectionType.TRENDING_CITIES).hasSize(1);
    assertThat(response.getSections()).allSatisfy(section -> assertThat(section.getItems()).isNotEmpty());
  }

  private static HomeSectionConfigEntity config(HomeSectionType type) {
    return HomeSectionConfigEntity.builder()
        .sectionType(type)
        .title(type.name())
        .maxItems(1)
        .build();
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
