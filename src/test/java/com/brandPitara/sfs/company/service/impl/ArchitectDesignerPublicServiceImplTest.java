package com.brandPitara.sfs.company.service.impl;

import com.brandPitara.sfs.company.dto.*;
import com.brandPitara.sfs.company.entity.*;
import com.brandPitara.sfs.company.enums.ArchitectDesignerType;
import com.brandPitara.sfs.company.repository.*;
import com.brandPitara.sfs.company.service.CompanyConnectedBrandPublicService;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.publicreview.dto.PublicReviewPlaceResponse;
import com.brandPitara.sfs.publicreview.dto.PublicReviewSampleResponse;
import com.brandPitara.sfs.publicreview.dto.PublicReviewSignalResponse;
import com.brandPitara.sfs.publicreview.enums.GoogleReviewDisplayMode;
import com.brandPitara.sfs.publicreview.enums.PublicReviewSourceType;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import com.brandPitara.sfs.publicreview.service.PublicReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchitectDesignerPublicServiceImplTest {

  @Mock private CompanyRepository companyRepository;
  @Mock private CompanyProjectRepository companyProjectRepository;
  @Mock private CompanyStatRepository companyStatRepository;
  @Mock private CompanyAwardRepository companyAwardRepository;
  @Mock private CompanyCertificateRepository companyCertificateRepository;
  @Mock private CompanyMediaRepository companyMediaRepository;
  @Mock private CompanyPricingPlanRepository companyPricingPlanRepository;
  @Mock private CompanyConnectedBrandPublicService companyConnectedBrandPublicService;
  @Mock private PublicReviewService publicReviewService;

  private ArchitectDesignerPublicServiceImpl service() {
    return new ArchitectDesignerPublicServiceImpl(
        companyRepository, companyProjectRepository, companyStatRepository, companyAwardRepository,
        companyCertificateRepository, companyMediaRepository, companyPricingPlanRepository,
        companyConnectedBrandPublicService, publicReviewService
    );
  }

  private CompanyEntity company() {
    return CompanyEntity.builder()
        .id(1L).name("Morphogenesis").slug("morphogenesis").companyType("ARCHITECT&DESIGNERS")
        .active(true).published(true).deleted(false)
        .build();
  }

  private void stubCommonEmptyLists() {
    when(companyProjectRepository
        .findTop10ByCompany_IdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(1L))
        .thenReturn(List.of());
    when(companyAwardRepository.findByCompany_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(companyConnectedBrandPublicService.getConnectedBrands(1L)).thenReturn(List.of());
  }

  private PublicReviewSignalResponse emptySignal() {
    return PublicReviewSignalResponse.builder()
        .targetType(PublicReviewTargetType.COMPANY)
        .targetId(1L)
        .sourceType(PublicReviewSourceType.GOOGLE_PLACES)
        .rating(null)
        .userRatingCount(0)
        .sourceLabel("Google Maps")
        .places(List.of())
        .samples(List.of())
        .build();
  }

  private PublicReviewSampleResponse sample(Long id, Integer rating) {
    return PublicReviewSampleResponse.builder()
        .id(id)
        .reviewerName("Reviewer " + id)
        .rating(rating)
        .reviewText("Great work")
        .relativePublishTime("2 months ago")
        .build();
  }

  @Test
  void getDetail_returnsPublicVisibleStatsAndCertificatesWithNewFields() {
    when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(1L))
        .thenReturn(Optional.of(company()));
    stubCommonEmptyLists();

    CompanyStatEntity stat = CompanyStatEntity.builder()
        .id(10L).label("Projects Completed").value("31+").iconKey("projects")
        .displayOrder(0).publicVisible(true).active(true).deleted(false).build();
    when(companyStatRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of(stat));

    CompanyCertificateEntity cert = CompanyCertificateEntity.builder()
        .id(20L).title("ISO Certificate").issuer("ISO").description("Quality management")
        .certificateFileUrl("https://cdn/iso.pdf").year(2021).verified(true)
        .displayOrder(0).publicVisible(true).active(true).deleted(false).build();
    when(companyCertificateRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of(cert));

    when(companyMediaRepository
        .findByCompany_IdAndUsageTypeAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L, "HERO"))
        .thenReturn(List.of());
    when(companyPricingPlanRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(publicReviewService.getPublicSignal(PublicReviewTargetType.COMPANY, 1L)).thenReturn(emptySignal());

    ArchitectDesignerDetailResponse response = service().getDetail(1L);

    assertThat(response.getStats()).hasSize(1);
    assertThat(response.getStats().get(0).getId()).isEqualTo(10L);
    assertThat(response.getStats().get(0).getIconKey()).isEqualTo("projects");
    assertThat(response.getStats().get(0).getSortOrder()).isEqualTo(0);

    assertThat(response.getCertificates()).hasSize(1);
    assertThat(response.getCertificates().get(0).getDescription()).isEqualTo("Quality management");
    assertThat(response.getCertificates().get(0).getFileUrl()).isEqualTo("https://cdn/iso.pdf");
    assertThat(response.getCertificates().get(0).getYear()).isEqualTo(2021);
    assertThat(response.getCertificates().get(0).getVerified()).isTrue();
  }

  @Test
  void getDetail_returnsHeroImagesAndPricingPlans() {
    when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(1L))
        .thenReturn(Optional.of(company()));
    stubCommonEmptyLists();
    when(companyStatRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(companyCertificateRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of());

    CompanyMediaEntity hero = CompanyMediaEntity.builder()
        .id(30L).mediaUrl("https://cdn/hero1.jpg").mediaType("IMAGE").usageType("HERO")
        .sortOrder(0).publicVisible(true).active(true).deleted(false).build();
    when(companyMediaRepository
        .findByCompany_IdAndUsageTypeAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L, "HERO"))
        .thenReturn(List.of(hero));

    CompanyPricingPlanEntity plan = CompanyPricingPlanEntity.builder()
        .id(40L).pricingType("SUBSCRIPTION").planName("Base")
        .priceAmount(new BigDecimal("49000")).currency("INR").billingUnit("month")
        .features(List.of("3D Design", "Site Visits"))
        .sortOrder(0).publicVisible(true).active(true).deleted(false).build();
    when(companyPricingPlanRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of(plan));

    when(publicReviewService.getPublicSignal(PublicReviewTargetType.COMPANY, 1L)).thenReturn(emptySignal());

    ArchitectDesignerDetailResponse response = service().getDetail(1L);

    assertThat(response.getHeroImages()).hasSize(1);
    assertThat(response.getHeroImages().get(0).getMediaUrl()).isEqualTo("https://cdn/hero1.jpg");

    assertThat(response.getPricingPlans()).hasSize(1);
    assertThat(response.getPricingPlans().get(0).getPlanName()).isEqualTo("Base");
    assertThat(response.getPricingPlans().get(0).getFeatures()).containsExactly("3D Design", "Site Visits");
  }

  @Test
  void getDetail_reviewSummaryGracefullyHandlesNoReviewPlaceAttached() {
    when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(1L))
        .thenReturn(Optional.of(company()));
    stubCommonEmptyLists();
    when(companyStatRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(companyCertificateRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(companyMediaRepository
        .findByCompany_IdAndUsageTypeAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L, "HERO"))
        .thenReturn(List.of());
    when(companyPricingPlanRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(publicReviewService.getPublicSignal(PublicReviewTargetType.COMPANY, 1L)).thenReturn(emptySignal());

    ArchitectDesignerDetailResponse response = service().getDetail(1L);

    assertThat(response.getReviewSummary()).isNotNull();
    assertThat(response.getReviewSummary().getRating()).isNull();
    assertThat(response.getReviewSummary().getReviewCount()).isEqualTo(0);
    assertThat(response.getReviewSummary().getDisplayMode()).isNull();
    assertThat(response.getReviewSummary().getSource()).isNull();
    assertThat(response.getSampleReviews()).isEmpty();
  }

  @Test
  void getDetail_reviewSummaryIncludesDisplayModeWhenPlaceAttached() {
    when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(1L))
        .thenReturn(Optional.of(company()));
    stubCommonEmptyLists();
    when(companyStatRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(companyCertificateRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(companyMediaRepository
        .findByCompany_IdAndUsageTypeAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L, "HERO"))
        .thenReturn(List.of());
    when(companyPricingPlanRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());

    PublicReviewPlaceResponse place = PublicReviewPlaceResponse.builder()
        .displayMode(GoogleReviewDisplayMode.RATING_AND_REVIEWS)
        .build();
    PublicReviewSignalResponse signal = PublicReviewSignalResponse.builder()
        .targetType(PublicReviewTargetType.COMPANY).targetId(1L)
        .sourceType(PublicReviewSourceType.GOOGLE_PLACES)
        .rating(new BigDecimal("4.5")).userRatingCount(120).sourceLabel("Google Maps")
        .places(List.of(place)).samples(List.of())
        .build();
    when(publicReviewService.getPublicSignal(PublicReviewTargetType.COMPANY, 1L)).thenReturn(signal);

    ArchitectDesignerDetailResponse response = service().getDetail(1L);

    assertThat(response.getReviewSummary().getRating()).isEqualByComparingTo("4.5");
    assertThat(response.getReviewSummary().getReviewCount()).isEqualTo(120);
    assertThat(response.getReviewSummary().getDisplayMode()).isEqualTo("RATING_AND_REVIEWS");
    assertThat(response.getReviewSummary().getSource()).isEqualTo("GOOGLE");
  }

  @Test
  void getDetail_capsSampleReviewsAtFiveSortedByRatingDescending() {
    when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(1L))
        .thenReturn(Optional.of(company()));
    stubCommonEmptyLists();
    when(companyStatRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(companyCertificateRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(companyMediaRepository
        .findByCompany_IdAndUsageTypeAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L, "HERO"))
        .thenReturn(List.of());
    when(companyPricingPlanRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());

    PublicReviewPlaceResponse place = PublicReviewPlaceResponse.builder()
        .displayMode(GoogleReviewDisplayMode.RATING_AND_REVIEWS)
        .build();
    // 7 approved samples with mixed ratings (and one null rating) - only the
    // top 5 by rating should survive, in descending order, nulls last.
    PublicReviewSignalResponse signal = PublicReviewSignalResponse.builder()
        .targetType(PublicReviewTargetType.COMPANY).targetId(1L)
        .sourceType(PublicReviewSourceType.GOOGLE_PLACES)
        .rating(new BigDecimal("4.2")).userRatingCount(200)
        .places(List.of(place))
        .samples(List.of(
            sample(1L, 3), sample(2L, 5), sample(3L, 1), sample(4L, 4),
            sample(5L, null), sample(6L, 5), sample(7L, 2)
        ))
        .build();
    when(publicReviewService.getPublicSignal(PublicReviewTargetType.COMPANY, 1L)).thenReturn(signal);

    ArchitectDesignerDetailResponse response = service().getDetail(1L);

    assertThat(response.getSampleReviews()).hasSize(5);
    assertThat(response.getSampleReviews())
        .extracting(PublicReviewSampleResponse::getRating)
        .containsExactly(5, 5, 4, 3, 2);
  }

  @Test
  void getDetail_throwsNotFound_whenCompanyNotPublic() {
    when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(999L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getDetail(999L)).isInstanceOf(ResponseStatusException.class);
  }

  // ── Phase 8A-G: remaining aggregation groups this file never exercised with
  // real content - stats/certificates/hero-images/pricing-plans/review-summary
  // /sample-reviews were already covered above; topProjects and connectedBrands
  // were only ever stubbed empty via stubCommonEmptyLists(), never asserted on
  // with real data (GAP-037 reconnaissance finding) ─────────────────────────

  private void stubRemainingAggregationEmpty() {
    when(companyStatRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(companyCertificateRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(companyMediaRepository
        .findByCompany_IdAndUsageTypeAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L, "HERO"))
        .thenReturn(List.of());
    when(companyPricingPlanRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(publicReviewService.getPublicSignal(PublicReviewTargetType.COMPANY, 1L)).thenReturn(emptySignal());
  }

  @Test
  void getDetail_returnsTopProjectsMappedFromCompanyProjectEntities() {
    when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(1L))
        .thenReturn(Optional.of(company()));
    when(companyAwardRepository.findByCompany_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(companyConnectedBrandPublicService.getConnectedBrands(1L)).thenReturn(List.of());
    stubRemainingAggregationEmpty();

    CityEntity city = CityEntity.builder().id(11L).name("Gurugram").latitude(28.45).longitude(77.02).build();
    CompanyProjectEntity project = CompanyProjectEntity.builder()
        .id(501L).name("Skyline Residence").company(company()).city(city)
        .addressLine("Sector 50").clientName("Private Client").projectArea("4200 sq ft")
        .detail3("Completed 2025").tags("modern,minimal")
        .coverMediaUrl("https://cdn/project-501.jpg").coverMediaType("IMAGE")
        .build();
    when(companyProjectRepository
        .findTop10ByCompany_IdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(1L))
        .thenReturn(List.of(project));

    ArchitectDesignerDetailResponse response = service().getDetail(1L);

    assertThat(response.getTopProjects()).hasSize(1);
    CompanyProjectCardDto card = response.getTopProjects().get(0);
    assertThat(card.getId()).isEqualTo(501L);
    assertThat(card.getName()).isEqualTo("Skyline Residence");
    assertThat(card.getCompanyId()).isEqualTo(1L);
    assertThat(card.getCompanyName()).isEqualTo("Morphogenesis");
    assertThat(card.getCityId()).isEqualTo(11L);
    assertThat(card.getCityName()).isEqualTo("Gurugram");
    assertThat(card.getClientName()).isEqualTo("Private Client");
    assertThat(card.getCoverMediaUrl()).isEqualTo("https://cdn/project-501.jpg");
    assertThat(card.getTags()).contains("modern", "minimal");
  }

  @Test
  void getDetail_returnsConnectedBrandsFromTheConnectedBrandService() {
    when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(1L))
        .thenReturn(Optional.of(company()));
    when(companyProjectRepository
        .findTop10ByCompany_IdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(1L))
        .thenReturn(List.of());
    when(companyAwardRepository.findByCompany_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of());
    stubRemainingAggregationEmpty();

    ConnectedBrandDto brand = ConnectedBrandDto.builder()
        .brandId(9001L).name("Kohler").slug("kohler").logoUrl("https://cdn/kohler.png")
        .verified(true).featured(true).displayOrder(0)
        .build();
    when(companyConnectedBrandPublicService.getConnectedBrands(1L)).thenReturn(List.of(brand));

    ArchitectDesignerDetailResponse response = service().getDetail(1L);

    assertThat(response.getConnectedBrands()).hasSize(1);
    assertThat(response.getConnectedBrands().get(0).getBrandId()).isEqualTo(9001L);
    assertThat(response.getConnectedBrands().get(0).getName()).isEqualTo("Kohler");
    assertThat(response.getConnectedBrands().get(0).isVerified()).isTrue();
  }

  // ── Phase 8A-G: getDetailBySlug (GAP-003B) ─────────────────────────────────

  private void stubFullDetailAggregationEmpty() {
    when(companyProjectRepository
        .findTop10ByCompany_IdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(1L))
        .thenReturn(List.of());
    when(companyAwardRepository.findByCompany_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(1L))
        .thenReturn(List.of());
    when(companyConnectedBrandPublicService.getConnectedBrands(1L)).thenReturn(List.of());
    stubRemainingAggregationEmpty();
  }

  @Test
  void getDetailBySlug_returnsSameRichResponseAsNumericDetail_whenSlugAndTypeMatch() {
    when(companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(1L))
        .thenReturn(Optional.of(company()));
    when(companyRepository.findBySlugAndCompanyTypeInAndActiveTrueAndPublishedTrueAndDeletedFalse(
        "morphogenesis", ArchitectDesignerType.ARCHITECT.storageValues()))
        .thenReturn(Optional.of(company()));
    stubFullDetailAggregationEmpty();

    ArchitectDesignerDetailResponse bySlug = service().getDetailBySlug("morphogenesis", ArchitectDesignerType.ARCHITECT);
    ArchitectDesignerDetailResponse byId = service().getDetail(1L);

    assertThat(bySlug.getCompanyId()).isEqualTo(byId.getCompanyId());
    assertThat(bySlug.getName()).isEqualTo(byId.getName());
    assertThat(bySlug.getSlug()).isEqualTo(byId.getSlug());
    assertThat(bySlug.getDescription()).isEqualTo(byId.getDescription());
    assertThat(bySlug.getTopProjects()).isEqualTo(byId.getTopProjects());
    assertThat(bySlug.getStats()).isEqualTo(byId.getStats());
  }

  @Test
  void getDetailBySlug_throwsNotFound_whenSlugUnknown() {
    when(companyRepository.findBySlugAndCompanyTypeInAndActiveTrueAndPublishedTrueAndDeletedFalse(
        "unknown-studio", ArchitectDesignerType.ARCHITECT.storageValues()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getDetailBySlug("unknown-studio", ArchitectDesignerType.ARCHITECT))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void getDetailBySlug_throwsNotFound_whenSlugBelongsToTheOtherType() {
    // A real Interior Designer's slug requested through the Architect type -
    // the repository query itself scopes by companyType, so it simply finds
    // nothing for ARCHITECT's storage values, exactly like an unknown slug.
    when(companyRepository.findBySlugAndCompanyTypeInAndActiveTrueAndPublishedTrueAndDeletedFalse(
        "interior-studio", ArchitectDesignerType.ARCHITECT.storageValues()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getDetailBySlug("interior-studio", ArchitectDesignerType.ARCHITECT))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void getDetailBySlug_neverFetchesByNumericId() {
    // Proves the slug path uses its own dedicated repository method, never
    // falling back to (or additionally calling) the numeric id lookup.
    when(companyRepository.findBySlugAndCompanyTypeInAndActiveTrueAndPublishedTrueAndDeletedFalse(
        "morphogenesis", ArchitectDesignerType.ARCHITECT.storageValues()))
        .thenReturn(Optional.of(company()));
    stubFullDetailAggregationEmpty();

    service().getDetailBySlug("morphogenesis", ArchitectDesignerType.ARCHITECT);

    verify(companyRepository, never()).findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(any());
  }

  // ── Phase 8A-G: list (GAP-037) ──────────────────────────────────────────────

  private CompanyEntity company(Long id, String name, String slug, String companyType) {
    return CompanyEntity.builder()
        .id(id).name(name).slug(slug).companyType(companyType)
        .active(true).published(true).deleted(false)
        .build();
  }

  private Pageable defaultPageable() {
    return PageRequest.of(0, 20, Sort.by("priority").ascending().and(Sort.by("id").descending()));
  }

  @Test
  void list_architectType_queriesTheArchitectStorageValueSet() {
    Pageable pageable = defaultPageable();
    Page<CompanyEntity> page = new PageImpl<>(
        List.of(company(1L, "Morphogenesis", "morphogenesis", "ARCHITECT&DESIGNERS")), pageable, 1);
    when(companyRepository.findByCompanyTypeInAndActiveTrueAndPublishedTrueAndDeletedFalse(
        eq(ArchitectDesignerType.ARCHITECT.storageValues()), eq(pageable)))
        .thenReturn(page);

    Page<ArchitectDesignerListItemResponse> result = service().list(ArchitectDesignerType.ARCHITECT, pageable);

    assertThat(result.getContent()).hasSize(1);
    ArchitectDesignerListItemResponse item = result.getContent().get(0);
    assertThat(item.getCompanyId()).isEqualTo(1L);
    assertThat(item.getSlug()).isEqualTo("morphogenesis");
    assertThat(item.getName()).isEqualTo("Morphogenesis");
    // Normalized type, not the raw "ARCHITECT&DESIGNERS" storage value.
    assertThat(item.getType()).isEqualTo("ARCHITECT");
    verify(companyRepository).findByCompanyTypeInAndActiveTrueAndPublishedTrueAndDeletedFalse(
        eq(ArchitectDesignerType.ARCHITECT.storageValues()), eq(pageable));
  }

  @Test
  void list_interiorDesignerType_queriesTheInteriorDesignerStorageValueSet() {
    Pageable pageable = defaultPageable();
    Page<CompanyEntity> page = new PageImpl<>(
        List.of(company(2L, "Studio Interiors", "studio-interiors", "INTERIOR_DESIGNER")), pageable, 1);
    when(companyRepository.findByCompanyTypeInAndActiveTrueAndPublishedTrueAndDeletedFalse(
        eq(ArchitectDesignerType.INTERIOR_DESIGNER.storageValues()), eq(pageable)))
        .thenReturn(page);

    Page<ArchitectDesignerListItemResponse> result = service().list(ArchitectDesignerType.INTERIOR_DESIGNER, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getType()).isEqualTo("INTERIOR_DESIGNER");
    verify(companyRepository).findByCompanyTypeInAndActiveTrueAndPublishedTrueAndDeletedFalse(
        eq(ArchitectDesignerType.INTERIOR_DESIGNER.storageValues()), eq(pageable));
  }

  @Test
  void list_architectAndInteriorDesignerQueriesUseDisjointStorageValueSets() {
    // Structural proof that the two normalized types can never both match
    // the same query - each call is scoped to its own storage-value set.
    assertThat(ArchitectDesignerType.ARCHITECT.storageValues())
        .doesNotContainAnyElementsOf(ArchitectDesignerType.INTERIOR_DESIGNER.storageValues());
  }

  @Test
  void list_returnsAnEmptyPageWithoutError() {
    Pageable pageable = defaultPageable();
    Page<CompanyEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
    when(companyRepository.findByCompanyTypeInAndActiveTrueAndPublishedTrueAndDeletedFalse(
        eq(ArchitectDesignerType.ARCHITECT.storageValues()), eq(pageable)))
        .thenReturn(emptyPage);

    Page<ArchitectDesignerListItemResponse> result = service().list(ArchitectDesignerType.ARCHITECT, pageable);

    assertThat(result.getContent()).isEmpty();
    assertThat(result.getTotalElements()).isZero();
  }

  @Test
  void list_forAPageBeyondAvailableDataReturnsAnEmptyPageNotAnError() {
    Pageable pageable = PageRequest.of(5, 20, Sort.by("priority").ascending().and(Sort.by("id").descending()));
    Page<CompanyEntity> emptyPage = new PageImpl<>(List.of(), pageable, 3);
    when(companyRepository.findByCompanyTypeInAndActiveTrueAndPublishedTrueAndDeletedFalse(
        eq(ArchitectDesignerType.ARCHITECT.storageValues()), eq(pageable)))
        .thenReturn(emptyPage);

    Page<ArchitectDesignerListItemResponse> result = service().list(ArchitectDesignerType.ARCHITECT, pageable);

    assertThat(result.getContent()).isEmpty();
    assertThat(result.getTotalElements()).isEqualTo(3);
  }

  @Test
  void list_preservesPageMetadataFromTheRepositoryPage() {
    Pageable pageable = PageRequest.of(1, 20, Sort.by("priority").ascending().and(Sort.by("id").descending()));
    Page<CompanyEntity> page = new PageImpl<>(
        List.of(company(3L, "A", "a", "ARCHITECT")), pageable, 45);
    when(companyRepository.findByCompanyTypeInAndActiveTrueAndPublishedTrueAndDeletedFalse(
        eq(ArchitectDesignerType.ARCHITECT.storageValues()), eq(pageable)))
        .thenReturn(page);

    Page<ArchitectDesignerListItemResponse> result = service().list(ArchitectDesignerType.ARCHITECT, pageable);

    assertThat(result.getTotalElements()).isEqualTo(45);
    assertThat(result.getTotalPages()).isEqualTo(3);
    assertThat(result.getNumber()).isEqualTo(1);
    assertThat(result.hasNext()).isTrue();
  }

  @Test
  void list_neverIssuesAnyChildAggregationQuery() {
    // GAP-037's own concern: the listing DTO must never trigger per-row
    // stats/awards/certificates/media/pricing/review/project queries.
    Pageable pageable = defaultPageable();
    when(companyRepository.findByCompanyTypeInAndActiveTrueAndPublishedTrueAndDeletedFalse(
        eq(ArchitectDesignerType.ARCHITECT.storageValues()), eq(pageable)))
        .thenReturn(new PageImpl<>(
            List.of(company(1L, "Morphogenesis", "morphogenesis", "ARCHITECT")), pageable, 1));

    service().list(ArchitectDesignerType.ARCHITECT, pageable);

    verifyNoInteractions(
        companyProjectRepository, companyStatRepository, companyAwardRepository,
        companyCertificateRepository, companyMediaRepository, companyPricingPlanRepository,
        companyConnectedBrandPublicService, publicReviewService
    );
  }

  @Test
  void list_itemContractNeverIncludesContactOrInternalFields() {
    // Structural proof, not a leak test: ArchitectDesignerListItemResponse
    // has exactly these 7 fields - no phone/whatsapp/email/active/published
    // /deleted/priority field exists on the class at all to leak.
    List<String> fieldNames = java.util.Arrays.stream(ArchitectDesignerListItemResponse.class.getDeclaredFields())
        .map(java.lang.reflect.Field::getName)
        .toList();
    assertThat(fieldNames).containsExactlyInAnyOrder(
        "companyId", "slug", "name", "type", "logoUrl", "coverImageUrl", "description");
  }
}
