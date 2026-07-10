package com.brandPitara.sfs.company.service.impl;

import com.brandPitara.sfs.company.dto.*;
import com.brandPitara.sfs.company.entity.*;
import com.brandPitara.sfs.company.repository.*;
import com.brandPitara.sfs.company.service.CompanyConnectedBrandPublicService;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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
}
