package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.PublicBrandCardResponse;
import com.brandPitara.sfs.brand.dto.PublicBrandDetailResponse;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.entity.BrandSkuEntity;
import com.brandPitara.sfs.brand.repository.BrandCategoryLinkRepository;
import com.brandPitara.sfs.brand.repository.BrandCertificateRepository;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.brand.repository.BrandFaqRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.repository.BrandSkuRepository;
import com.brandPitara.sfs.brand.service.impl.BrandPublicServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandPublicServiceImplTest {

  @Mock private BrandRepository brandRepository;
  @Mock private BrandCategoryLinkRepository brandCategoryLinkRepository;
  @Mock private BrandSkuRepository brandSkuRepository;
  @Mock private BrandCertificateRepository brandCertificateRepository;
  @Mock private BrandFaqRepository brandFaqRepository;
  @Mock private BrandCollaborationRepository brandCollaborationRepository;

  @InjectMocks private BrandPublicServiceImpl brandPublicService;

  private BrandEntity brand(Long id, String name, String slug) {
    return BrandEntity.builder()
        .id(id).name(name).slug(slug)
        .logoUrl("logo.png").heroImageUrl("hero.png").shortDescription("short")
        .description("long description")
        .published(true).active(true).deleted(false).priority(0)
        .promoEnabled(false)
        .build();
  }

  // ---------- 1 & 2: brand detail visibility ----------

  @Test
  void getPublicBrandBySlug_returnsDetail_forPublicVisibleBrand() {
    BrandEntity brand = brand(1L, "Berger", "berger-paints");
    when(brandRepository.findBySlugAndPublishedTrueAndActiveTrueAndDeletedFalse("berger-paints"))
        .thenReturn(Optional.of(brand));
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandSkuRepository.findByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(eq(1L), any(Pageable.class)))
        .thenReturn(Page.empty());
    when(brandSkuRepository.findDistinctPublicCategoriesByBrandId(1L)).thenReturn(List.of());
    when(brandSkuRepository.countByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(1L)).thenReturn(0L);
    when(brandCertificateRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandFaqRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandCollaborationRepository.findPublicProjectCollaborationsByBrandId(1L)).thenReturn(List.of());
    when(brandCollaborationRepository.findPublicBuilderCollaborationsByBrandId(1L)).thenReturn(List.of());
    when(brandCollaborationRepository.findPublicCompanyCollaborationsByBrandId(1L)).thenReturn(List.of());

    PublicBrandDetailResponse response = brandPublicService.getPublicBrandBySlug("berger-paints");

    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getName()).isEqualTo("Berger");
    assertThat(response.getSlug()).isEqualTo("berger-paints");
    assertThat(response.getStats().getProductsCount()).isEqualTo(0L);
    // brand has no linked categories -> related brands lookup must be skipped entirely
    verify(brandRepository, never()).findRelatedBrands(any(), any(), any());
  }

  @Test
  void getPublicBrandBySlug_throwsNotFound_whenBrandMissingOrUnpublished() {
    when(brandRepository.findBySlugAndPublishedTrueAndActiveTrueAndDeletedFalse("not-live"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> brandPublicService.getPublicBrandBySlug("not-live"))
        .isInstanceOf(EntityNotFoundException.class);
  }

  // ---------- 3 & 4: listing filters + batching ----------

  @Test
  void listPublicBrands_passesCategoryIdThroughToRepository() {
    Pageable pageable = PageRequest.of(0, 20);
    when(brandRepository.findPublicBrands(eq(9L), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    brandPublicService.listPublicBrands(9L, null, pageable);

    ArgumentCaptor<Long> categoryIdCaptor = ArgumentCaptor.forClass(Long.class);
    verify(brandRepository).findPublicBrands(categoryIdCaptor.capture(), eq(pageable));
    assertThat(categoryIdCaptor.getValue()).isEqualTo(9L);
  }

  @Test
  void listPublicBrands_batchesCategoryAndStatLookups_insteadOfPerRow() {
    Pageable pageable = PageRequest.of(0, 20);
    BrandEntity brand1 = brand(1L, "Berger", "berger");
    BrandEntity brand2 = brand(2L, "Asian Paints", "asian-paints");
    Page<BrandEntity> page = new PageImpl<>(List.of(brand1, brand2), pageable, 2);

    when(brandRepository.findPublicBrands(isNull(), eq(pageable))).thenReturn(page);
    when(brandCategoryLinkRepository.findByBrand_IdInAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(anyCollection()))
        .thenReturn(List.of());
    when(brandSkuRepository.countPublicByBrandIds(anyCollection())).thenReturn(List.of());
    when(brandCollaborationRepository.findPublicStatRowsByBrandIds(anyCollection())).thenReturn(List.of());

    Page<PublicBrandCardResponse> result = brandPublicService.listPublicBrands(null, null, pageable);

    assertThat(result.getContent()).hasSize(2);
    // exactly one batched call each, never once-per-brand-row
    verify(brandCategoryLinkRepository, times(1))
        .findByBrand_IdInAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(anyCollection());
    verify(brandSkuRepository, times(1)).countPublicByBrandIds(anyCollection());
    verify(brandCollaborationRepository, times(1)).findPublicStatRowsByBrandIds(anyCollection());
    verify(brandCategoryLinkRepository, never())
        .findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(any());
  }

  // ---------- Phase 1C.7 Bug B regression coverage: never pass null/blank q to lower(:q) ----------

  @Test
  void listPublicBrands_withNullQ_usesNoSearchRepositoryPath() {
    Pageable pageable = PageRequest.of(0, 20);
    when(brandRepository.findPublicBrands(isNull(), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    brandPublicService.listPublicBrands(null, null, pageable);

    verify(brandRepository).findPublicBrands(isNull(), eq(pageable));
    verify(brandRepository, never()).searchPublicBrands(any(), any(), any());
  }

  @Test
  void listPublicBrands_withBlankQ_usesNoSearchRepositoryPath() {
    Pageable pageable = PageRequest.of(0, 20);
    when(brandRepository.findPublicBrands(isNull(), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    brandPublicService.listPublicBrands(null, "   ", pageable);

    verify(brandRepository).findPublicBrands(isNull(), eq(pageable));
    verify(brandRepository, never()).searchPublicBrands(any(), any(), any());
  }

  @Test
  void listPublicBrands_withQText_usesSearchRepositoryPathWithNormalizedLowercaseQ() {
    Pageable pageable = PageRequest.of(0, 20);
    when(brandRepository.searchPublicBrands(isNull(), eq("berger"), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    brandPublicService.listPublicBrands(null, "  Berger  ", pageable);

    verify(brandRepository).searchPublicBrands(isNull(), eq("berger"), eq(pageable));
    verify(brandRepository, never()).findPublicBrands(any(), any());
  }

  // ---------- 8: latest products only published/active/not-deleted ----------

  @Test
  void getPublicBrandBySlug_latestProducts_onlyUsesPublicVisibleSkuQuery() {
    BrandEntity brand = brand(1L, "Berger", "berger-paints");
    BrandSkuEntity sku = BrandSkuEntity.builder()
        .id(10L).brand(brand).name("Silk Glamour").slug("silk-glamour")
        .published(true).active(true).deleted(false).featured(true).latest(true)
        .build();

    when(brandRepository.findBySlugAndPublishedTrueAndActiveTrueAndDeletedFalse("berger-paints"))
        .thenReturn(Optional.of(brand));
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandSkuRepository.findByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(eq(1L), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(sku)));
    when(brandSkuRepository.findDistinctPublicCategoriesByBrandId(1L)).thenReturn(List.of());
    when(brandSkuRepository.countByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(1L)).thenReturn(1L);
    when(brandCertificateRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandFaqRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandCollaborationRepository.findPublicProjectCollaborationsByBrandId(1L)).thenReturn(List.of());
    when(brandCollaborationRepository.findPublicBuilderCollaborationsByBrandId(1L)).thenReturn(List.of());
    when(brandCollaborationRepository.findPublicCompanyCollaborationsByBrandId(1L)).thenReturn(List.of());

    PublicBrandDetailResponse response = brandPublicService.getPublicBrandBySlug("berger-paints");

    assertThat(response.getLatestProducts()).hasSize(1);
    assertThat(response.getLatestProducts().get(0).getName()).isEqualTo("Silk Glamour");
    // The published/active/deleted filtering is the repository method's own contract -
    // asserting this exact method (not a generic findByBrandId) was called is the
    // service-level guarantee that only public-visible SKUs are ever requested.
    verify(brandSkuRepository).findByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(eq(1L), any(Pageable.class));
  }

  // ---------- 9: related brands exclude current brand ----------

  @Test
  void getPublicBrandBySlug_relatedBrands_excludesCurrentBrand() {
    BrandEntity brand = brand(1L, "Berger", "berger-paints");

    var categoryLink = com.brandPitara.sfs.brand.entity.BrandCategoryLinkEntity.builder()
        .id(100L).brand(brand)
        .category(com.brandPitara.sfs.entity.CategoryEntity.builder().id(9L).name("Paints").slug("paints").priority(1).active(true).build())
        .sortOrder(0).active(true).deleted(false)
        .build();

    when(brandRepository.findBySlugAndPublishedTrueAndActiveTrueAndDeletedFalse("berger-paints"))
        .thenReturn(Optional.of(brand));
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of(categoryLink));
    when(brandSkuRepository.findByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(eq(1L), any(Pageable.class)))
        .thenReturn(Page.empty());
    when(brandSkuRepository.findDistinctPublicCategoriesByBrandId(1L)).thenReturn(List.of());
    when(brandSkuRepository.countByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(1L)).thenReturn(0L);
    when(brandCertificateRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandFaqRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandCollaborationRepository.findPublicProjectCollaborationsByBrandId(1L)).thenReturn(List.of());
    when(brandCollaborationRepository.findPublicBuilderCollaborationsByBrandId(1L)).thenReturn(List.of());
    when(brandCollaborationRepository.findPublicCompanyCollaborationsByBrandId(1L)).thenReturn(List.of());
    when(brandRepository.findRelatedBrands(anyCollection(), eq(1L), any())).thenReturn(List.of());

    brandPublicService.getPublicBrandBySlug("berger-paints");

    ArgumentCaptor<Long> excludeIdCaptor = ArgumentCaptor.forClass(Long.class);
    verify(brandRepository).findRelatedBrands(anyCollection(), excludeIdCaptor.capture(), any());
    assertThat(excludeIdCaptor.getValue()).isEqualTo(1L);
  }
}
