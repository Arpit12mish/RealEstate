package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.brand.dto.BrandCardDto;
import com.brandPitara.sfs.brand.dto.PublicBrandCategoryResponse;
import com.brandPitara.sfs.brand.dto.PublicBrandCardResponse;
import com.brandPitara.sfs.brand.service.BrandPublicService;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.SectionContext;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class TopBrandsSectionLoaderTest {

  private PublicBrandCardResponse brandCard(Long id, String name, String slug) {
    return PublicBrandCardResponse.builder()
        .id(id)
        .name(name)
        .slug(slug)
        .logoUrl("logo.png")
        .heroImageUrl("hero.png")
        .shortDescription("short")
        .categories(List.of(PublicBrandCategoryResponse.builder().id(9L).name("Paints").slug("paints").displayOrder(1).build()))
        .productsCount(5)
        .projectsCount(3)
        .buildersCount(2)
        .designersCount(1)
        .architectsCount(1)
        .promoEnabled(true)
        .build();
  }

  // 1 & 2 & 3: public-visible brands (trusted from BrandPublicService), slug + categories/stats present
  @Test
  void load_mapsBrandPublicServiceCards_includingSlugCategoriesAndStats() {
    BrandPublicService brandPublicService = mock(BrandPublicService.class);
    TopBrandsSectionLoader loader = new TopBrandsSectionLoader(brandPublicService);

    HomeSectionConfigEntity config = HomeSectionConfigEntity.builder()
        .id(1L)
        .sectionType(HomeSectionType.TOP_BRANDS)
        .title("Top Brands")
        .maxItems(20)
        .build();

    PublicBrandCardResponse berger = brandCard(1L, "Berger", "berger-paints");
    when(brandPublicService.listPublicBrands(isNull(), isNull(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(berger)));

    HomeSectionDto<?> section = loader.load(config, SectionContext.builder().build());

    assertThat(section.getType()).isEqualTo(HomeSectionType.TOP_BRANDS);
    assertThat(section.getItems()).hasSize(1);
    BrandCardDto card = (BrandCardDto) section.getItems().get(0);
    assertThat(card.getSlug()).isEqualTo("berger-paints");
    assertThat(card.getCategories()).extracting("name").containsExactly("Paints");
    assertThat(card.getProductsCount()).isEqualTo(5);
    assertThat(card.getProjectsCount()).isEqualTo(3);
    assertThat(card.getArchitectsCount()).isEqualTo(1);
    assertThat(card.getAction().getPath()).isEqualTo("/brands/berger-paints");
  }

  @Test
  void load_usesConfiguredMaxItemsAndPrioritySort() {
    BrandPublicService brandPublicService = mock(BrandPublicService.class);
    TopBrandsSectionLoader loader = new TopBrandsSectionLoader(brandPublicService);

    HomeSectionConfigEntity config = HomeSectionConfigEntity.builder()
        .id(1L)
        .sectionType(HomeSectionType.TOP_BRANDS)
        .maxItems(5)
        .build();

    when(brandPublicService.listPublicBrands(isNull(), isNull(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    loader.load(config, SectionContext.builder().build());

    org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
    verify(brandPublicService).listPublicBrands(isNull(), isNull(), captor.capture());
    Pageable used = captor.getValue();
    assertThat(used.getPageSize()).isEqualTo(5);
    assertThat(used.getSort().getOrderFor("priority").isAscending()).isTrue();
    assertThat(used.getSort().getOrderFor("id").isDescending()).isTrue();
  }

  // 5: no per-brand N+1 - the loader calls the (already-batched) service exactly once
  // for the whole section, regardless of how many brands come back.
  @Test
  void load_callsBrandPublicServiceExactlyOnce_regardlessOfResultSize() {
    BrandPublicService brandPublicService = mock(BrandPublicService.class);
    TopBrandsSectionLoader loader = new TopBrandsSectionLoader(brandPublicService);

    HomeSectionConfigEntity config = HomeSectionConfigEntity.builder()
        .id(1L)
        .sectionType(HomeSectionType.TOP_BRANDS)
        .maxItems(20)
        .build();

    when(brandPublicService.listPublicBrands(isNull(), isNull(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(
            brandCard(1L, "Berger", "berger-paints"),
            brandCard(2L, "Asian Paints", "asian-paints"),
            brandCard(3L, "Nerolac", "nerolac")
        )));

    HomeSectionDto<?> section = loader.load(config, SectionContext.builder().build());

    assertThat(section.getItems()).hasSize(3);
    verify(brandPublicService, times(1)).listPublicBrands(isNull(), isNull(), any(Pageable.class));
    verifyNoMoreInteractions(brandPublicService);
  }
}
