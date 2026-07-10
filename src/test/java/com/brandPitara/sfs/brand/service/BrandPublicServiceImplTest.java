package com.brandPitara.sfs.brand.service;

import com.brandPitara.sfs.brand.dto.PublicBrandCardResponse;
import com.brandPitara.sfs.brand.dto.PublicBrandDetailResponse;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.entity.BrandProductCategoryEntity;
import com.brandPitara.sfs.brand.entity.BrandSkuEntity;
import com.brandPitara.sfs.brand.enums.BrandCollaborationTargetType;
import com.brandPitara.sfs.brand.repository.BrandCategoryLinkRepository;
import com.brandPitara.sfs.brand.repository.BrandCertificateRepository;
import com.brandPitara.sfs.brand.repository.BrandCollaborationRepository;
import com.brandPitara.sfs.brand.repository.BrandFaqRepository;
import com.brandPitara.sfs.brand.repository.BrandProductCategoryRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.repository.BrandSkuRepository;
import com.brandPitara.sfs.brand.service.impl.BrandPublicServiceImpl;
import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.enums.BrandRelationType;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.company.entity.CompanyProjectEntity;
import com.brandPitara.sfs.company.entity.CompanyStatEntity;
import com.brandPitara.sfs.company.repository.CompanyProjectRepository;
import com.brandPitara.sfs.company.repository.CompanyStatRepository;
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

import java.math.BigDecimal;
import java.time.Year;
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
  @Mock private BrandProductCategoryRepository brandProductCategoryRepository;
  @Mock private CompanyStatRepository companyStatRepository;
  @Mock private CompanyProjectRepository companyProjectRepository;

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
    when(brandProductCategoryRepository.findByBrand_IdAndActiveTrueAndPublicVisibleTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
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
    assertThat(response.getFoundedYear()).isNull();
    assertThat(response.getYearsInIndustry()).isNull();
    assertThat(response.getStats().getYearsInIndustry()).isNull();
    // brand has no linked categories -> related brands lookup must be skipped entirely
    verify(brandRepository, never()).findRelatedBrands(any(), any(), any());
  }

  @Test
  void getPublicBrandBySlug_mapsPublicStatsFields() {
    BrandEntity brand = brand(1L, "Ikea", "ikea");
    brand.setFoundedYear(1943);
    brand.setCustomerRating(new BigDecimal("4.7"));
    brand.setCustomerRatingCount(350);

    when(brandRepository.findBySlugAndPublishedTrueAndActiveTrueAndDeletedFalse("ikea"))
        .thenReturn(Optional.of(brand));
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandSkuRepository.findByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(eq(1L), any(Pageable.class)))
        .thenReturn(Page.empty());
    when(brandProductCategoryRepository.findByBrand_IdAndActiveTrueAndPublicVisibleTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandSkuRepository.countByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(1L)).thenReturn(2L);
    when(brandCertificateRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandFaqRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandCollaborationRepository.findPublicProjectCollaborationsByBrandId(1L)).thenReturn(List.of());
    when(brandCollaborationRepository.findPublicBuilderCollaborationsByBrandId(1L)).thenReturn(List.of());
    when(brandCollaborationRepository.findPublicCompanyCollaborationsByBrandId(1L)).thenReturn(List.of());

    PublicBrandDetailResponse response = brandPublicService.getPublicBrandBySlug("ikea");

    int expectedYears = Year.now().getValue() - 1943;
    assertThat(response.getFoundedYear()).isEqualTo(1943);
    assertThat(response.getYearsInIndustry()).isEqualTo(expectedYears);
    assertThat(response.getCustomerRating()).isEqualByComparingTo("4.7");
    assertThat(response.getCustomerRatingCount()).isEqualTo(350);
    assertThat(response.getStats().getYearsInIndustry()).isEqualTo(expectedYears);
    assertThat(response.getStats().getCustomerRating()).isEqualByComparingTo("4.7");
    assertThat(response.getStats().getCustomerRatingCount()).isEqualTo(350);
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

  @Test
  void listPublicBrands_doesNotSurfaceCompanyProjectRowsInPublicStats() {
    Pageable pageable = PageRequest.of(0, 20);
    BrandEntity brand = brand(1L, "Berger", "berger");
    Page<BrandEntity> page = new PageImpl<>(List.of(brand), pageable, 1);

    when(brandRepository.findPublicBrands(isNull(), eq(pageable))).thenReturn(page);
    when(brandCategoryLinkRepository.findByBrand_IdInAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(anyCollection()))
        .thenReturn(List.of());
    when(brandSkuRepository.countPublicByBrandIds(anyCollection())).thenReturn(List.of());
    when(brandCollaborationRepository.findPublicStatRowsByBrandIds(anyCollection()))
        .thenReturn(List.of(
            new Object[] {1L, BrandCollaborationTargetType.PROJECT, null, 3L},
            new Object[] {1L, BrandCollaborationTargetType.COMPANY_PROJECT, null, 9L}
        ));

    Page<PublicBrandCardResponse> result = brandPublicService.listPublicBrands(null, null, pageable);

    PublicBrandCardResponse card = result.getContent().get(0);
    assertThat(card.getProjectsCount()).isEqualTo(3L);
    assertThat(card.getBuildersCount()).isZero();
    assertThat(card.getArchitectsCount()).isZero();
    assertThat(card.getDesignersCount()).isZero();
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
    when(brandProductCategoryRepository.findByBrand_IdAndActiveTrueAndPublicVisibleTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
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

  // ---------- Phase 2B.3: brand-owned product categories + SKU externalUrl ----------

  @Test
  void getPublicBrandBySlug_productCategories_onlyUsesPublicVisibleQuery() {
    BrandEntity brand = brand(1L, "Berger", "berger-paints");
    BrandProductCategoryEntity productCategory = BrandProductCategoryEntity.builder()
        .id(5L).brand(brand).name("Lamps").slug("lamps")
        .description("Decorative lamps").imageUrl("lamps.png").externalUrl("https://berger.com/lamps")
        .active(true).publicVisible(true).sortOrder(0).deleted(false)
        .build();

    when(brandRepository.findBySlugAndPublishedTrueAndActiveTrueAndDeletedFalse("berger-paints"))
        .thenReturn(Optional.of(brand));
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandSkuRepository.findByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(eq(1L), any(Pageable.class)))
        .thenReturn(Page.empty());
    when(brandProductCategoryRepository.findByBrand_IdAndActiveTrueAndPublicVisibleTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of(productCategory));
    when(brandSkuRepository.countByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(1L)).thenReturn(0L);
    when(brandCertificateRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandFaqRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandCollaborationRepository.findPublicProjectCollaborationsByBrandId(1L)).thenReturn(List.of());
    when(brandCollaborationRepository.findPublicBuilderCollaborationsByBrandId(1L)).thenReturn(List.of());
    when(brandCollaborationRepository.findPublicCompanyCollaborationsByBrandId(1L)).thenReturn(List.of());

    PublicBrandDetailResponse response = brandPublicService.getPublicBrandBySlug("berger-paints");

    assertThat(response.getProductCategories()).hasSize(1);
    assertThat(response.getProductCategories().get(0).getName()).isEqualTo("Lamps");
    assertThat(response.getProductCategories().get(0).getExternalUrl()).isEqualTo("https://berger.com/lamps");
    // The active/publicVisible/deleted filtering is the repository method's own contract -
    // asserting this exact method was called is the service-level guarantee that only
    // publicly-visible product categories are ever requested.
    verify(brandProductCategoryRepository)
        .findByBrand_IdAndActiveTrueAndPublicVisibleTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L);
  }

  @Test
  void getPublicBrandBySlug_latestProducts_includesExternalUrlAndCtaLabel() {
    BrandEntity brand = brand(1L, "Berger", "berger-paints");
    BrandSkuEntity skuWithUrl = BrandSkuEntity.builder()
        .id(10L).brand(brand).name("Silk Glamour").slug("silk-glamour")
        .externalUrl("https://berger.com/products/silk-glamour")
        .published(true).active(true).deleted(false).build();
    BrandSkuEntity skuWithoutUrl = BrandSkuEntity.builder()
        .id(11L).brand(brand).name("Weathercoat").slug("weathercoat")
        .published(true).active(true).deleted(false).build();

    when(brandRepository.findBySlugAndPublishedTrueAndActiveTrueAndDeletedFalse("berger-paints"))
        .thenReturn(Optional.of(brand));
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandSkuRepository.findByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(eq(1L), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(skuWithUrl, skuWithoutUrl)));
    when(brandProductCategoryRepository.findByBrand_IdAndActiveTrueAndPublicVisibleTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandSkuRepository.countByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(1L)).thenReturn(2L);
    when(brandCertificateRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandFaqRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(brandCollaborationRepository.findPublicProjectCollaborationsByBrandId(1L)).thenReturn(List.of());
    when(brandCollaborationRepository.findPublicBuilderCollaborationsByBrandId(1L)).thenReturn(List.of());
    when(brandCollaborationRepository.findPublicCompanyCollaborationsByBrandId(1L)).thenReturn(List.of());

    PublicBrandDetailResponse response = brandPublicService.getPublicBrandBySlug("berger-paints");

    var products = response.getLatestProducts();
    var withUrl = products.stream().filter(p -> p.getId().equals(10L)).findFirst().orElseThrow();
    var withoutUrl = products.stream().filter(p -> p.getId().equals(11L)).findFirst().orElseThrow();

    assertThat(withUrl.getExternalUrl()).isEqualTo("https://berger.com/products/silk-glamour");
    assertThat(withUrl.getCtaLabel()).isEqualTo("View Product");
    assertThat(withoutUrl.getExternalUrl()).isNull();
    assertThat(withoutUrl.getCtaLabel()).isNull();
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
    when(brandProductCategoryRepository.findByBrand_IdAndActiveTrueAndPublicVisibleTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
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

  // ---------- 10: topArchitects/interiorDesigners match the Home screen card fields ----------

  private CompanyEntity company(Long id, String name, String companyType) {
    return CompanyEntity.builder()
        .id(id)
        .name(name)
        .companyType(companyType)
        .logoUrl("company-logo-" + id + ".png")
        .coverImageUrl("company-cover-" + id + ".png")
        .specializationText("Architecture | Interiors | Landscape")
        .infoLine1("Offices | New Delhi")
        .infoLine2("Ranked among the Top 100 Design Firms")
        .active(true)
        .published(true)
        .deleted(false)
        .build();
  }

  private void stubBaseBrandDetailDependencies(Long brandId, BrandEntity brand) {
    when(brandRepository.findBySlugAndPublishedTrueAndActiveTrueAndDeletedFalse(brand.getSlug()))
        .thenReturn(Optional.of(brand));
    when(brandCategoryLinkRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(brandId))
        .thenReturn(List.of());
    when(brandSkuRepository.findByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(eq(brandId), any(Pageable.class)))
        .thenReturn(Page.empty());
    when(brandProductCategoryRepository.findByBrand_IdAndActiveTrueAndPublicVisibleTrueAndDeletedFalseOrderBySortOrderAscIdAsc(brandId))
        .thenReturn(List.of());
    when(brandSkuRepository.countByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(brandId)).thenReturn(0L);
    when(brandCertificateRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(brandId))
        .thenReturn(List.of());
    when(brandFaqRepository.findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(brandId))
        .thenReturn(List.of());
    when(brandCollaborationRepository.findPublicProjectCollaborationsByBrandId(brandId)).thenReturn(List.of());
    when(brandCollaborationRepository.findPublicBuilderCollaborationsByBrandId(brandId)).thenReturn(List.of());
  }

  @Test
  void getPublicBrandBySlug_topArchitects_includeCoverLogoDescriptionAndStats() {
    BrandEntity brand = brand(1L, "Berger", "berger-paints");
    stubBaseBrandDetailDependencies(1L, brand);

    CompanyEntity architectCompany = company(50L, "Morphogenesis", "ARCHITECT");
    BrandCollaborationEntity collaboration = BrandCollaborationEntity.builder()
        .company(architectCompany)
        .relationType(BrandRelationType.CERTIFIED_PARTNER)
        .verified(true)
        .featured(false)
        .sortOrder(0)
        .build();
    when(brandCollaborationRepository.findPublicCompanyCollaborationsByBrandId(1L))
        .thenReturn(List.of(collaboration));

    CompanyStatEntity yearsStat = CompanyStatEntity.builder()
        .company(architectCompany).label("Years Experience").value("18+").displayOrder(0).active(true).deleted(false)
        .build();
    CompanyStatEntity citiesStat = CompanyStatEntity.builder()
        .company(architectCompany).label("Cities Served").value("5").displayOrder(1).active(true).deleted(false)
        .build();
    CompanyStatEntity awardsStat = CompanyStatEntity.builder()
        .company(architectCompany).label("Awards").value("12+").displayOrder(2).active(true).deleted(false)
        .build();
    when(companyStatRepository.findByCompany_IdInAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(any()))
        .thenReturn(List.of(yearsStat, citiesStat, awardsStat));
    when(companyProjectRepository.findByCompany_IdInAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(any()))
        .thenReturn(List.of());

    PublicBrandDetailResponse response = brandPublicService.getPublicBrandBySlug("berger-paints");

    assertThat(response.getTopArchitects()).hasSize(1);
    var card = response.getTopArchitects().get(0);
    assertThat(card.getId()).isEqualTo(50L);
    assertThat(card.getName()).isEqualTo("Morphogenesis");
    assertThat(card.getLogoUrl()).isEqualTo("company-logo-50.png");
    assertThat(card.getCoverImageUrl()).isEqualTo("company-cover-50.png");
    assertThat(card.getDescription()).isEqualTo("Offices | New Delhi");
    assertThat(card.getSubtitle()).isEqualTo("Ranked among the Top 100 Design Firms");
    assertThat(card.getSpecializationText()).isEqualTo("Architecture | Interiors | Landscape");
    assertThat(card.getYearsExperience()).isEqualTo("18+");
    assertThat(card.getCitiesServed()).isEqualTo("5");
    assertThat(card.getAwardsCount()).isEqualTo("12+");
    assertThat(response.getInteriorDesigners()).isEmpty();
  }

  @Test
  void getPublicBrandBySlug_companyWithArchitectAndDesignerType_appearsInBothLists() {
    BrandEntity brand = brand(1L, "Berger", "berger-paints");
    stubBaseBrandDetailDependencies(1L, brand);

    CompanyEntity dualCompany = company(51L, "Studio Hybrid", "ARCHITECT&DESIGNERS");
    BrandCollaborationEntity collaboration = BrandCollaborationEntity.builder()
        .company(dualCompany)
        .verified(false)
        .featured(false)
        .sortOrder(0)
        .build();
    when(brandCollaborationRepository.findPublicCompanyCollaborationsByBrandId(1L))
        .thenReturn(List.of(collaboration));
    when(companyStatRepository.findByCompany_IdInAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(any()))
        .thenReturn(List.of());
    when(companyProjectRepository.findByCompany_IdInAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(any()))
        .thenReturn(List.of());

    PublicBrandDetailResponse response = brandPublicService.getPublicBrandBySlug("berger-paints");

    assertThat(response.getTopArchitects()).extracting("id").containsExactly(51L);
    assertThat(response.getInteriorDesigners()).extracting("id").containsExactly(51L);
  }

  @Test
  void getPublicBrandBySlug_companyCoverImage_fallsBackToTopCompanyProjectCover_whenCompanyCoverMissing() {
    BrandEntity brand = brand(1L, "Berger", "berger-paints");
    stubBaseBrandDetailDependencies(1L, brand);

    CompanyEntity architectCompany = company(52L, "NoCover Studio", "ARCHITECT");
    architectCompany.setCoverImageUrl(null);
    BrandCollaborationEntity collaboration = BrandCollaborationEntity.builder()
        .company(architectCompany)
        .verified(false)
        .featured(false)
        .sortOrder(0)
        .build();
    when(brandCollaborationRepository.findPublicCompanyCollaborationsByBrandId(1L))
        .thenReturn(List.of(collaboration));
    when(companyStatRepository.findByCompany_IdInAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(any()))
        .thenReturn(List.of());

    CompanyProjectEntity topProject = CompanyProjectEntity.builder()
        .company(architectCompany)
        .coverMediaUrl("project-cover.png")
        .build();
    when(companyProjectRepository.findByCompany_IdInAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(any()))
        .thenReturn(List.of(topProject));

    PublicBrandDetailResponse response = brandPublicService.getPublicBrandBySlug("berger-paints");

    assertThat(response.getTopArchitects()).hasSize(1);
    assertThat(response.getTopArchitects().get(0).getCoverImageUrl()).isEqualTo("project-cover.png");
  }

  @Test
  void getPublicBrandBySlug_companyStatsAndProjects_areBatchedOncePerRequest_notPerCompany() {
    BrandEntity brand = brand(1L, "Berger", "berger-paints");
    stubBaseBrandDetailDependencies(1L, brand);

    CompanyEntity companyA = company(60L, "Studio A", "ARCHITECT");
    CompanyEntity companyB = company(61L, "Studio B", "DESIGNER");
    BrandCollaborationEntity collabA = BrandCollaborationEntity.builder().company(companyA).sortOrder(0).build();
    BrandCollaborationEntity collabB = BrandCollaborationEntity.builder().company(companyB).sortOrder(1).build();
    when(brandCollaborationRepository.findPublicCompanyCollaborationsByBrandId(1L))
        .thenReturn(List.of(collabA, collabB));
    when(companyStatRepository.findByCompany_IdInAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(any()))
        .thenReturn(List.of());
    when(companyProjectRepository.findByCompany_IdInAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(any()))
        .thenReturn(List.of());

    brandPublicService.getPublicBrandBySlug("berger-paints");

    verify(companyStatRepository, times(1))
        .findByCompany_IdInAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(any());
    verify(companyProjectRepository, times(1))
        .findByCompany_IdInAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(any());
  }
}
