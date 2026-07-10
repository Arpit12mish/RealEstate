package com.brandPitara.sfs.company.service.impl;

import com.brandPitara.sfs.company.dto.*;
import com.brandPitara.sfs.company.entity.*;
import com.brandPitara.sfs.company.mapper.CompanyProjectTagMapper;
import com.brandPitara.sfs.company.repository.*;
import com.brandPitara.sfs.company.service.ArchitectDesignerPublicService;
import com.brandPitara.sfs.company.service.CompanyConnectedBrandPublicService;
import com.brandPitara.sfs.publicreview.dto.PublicReviewSampleResponse;
import com.brandPitara.sfs.publicreview.dto.PublicReviewSignalResponse;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import com.brandPitara.sfs.publicreview.service.PublicReviewService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ArchitectDesignerPublicServiceImpl implements ArchitectDesignerPublicService {

  private static final String HERO_USAGE_TYPE = "HERO";
  private static final int MAX_SAMPLE_REVIEWS = 5;

  private final CompanyRepository companyRepository;
  private final CompanyProjectRepository companyProjectRepository;
  private final CompanyStatRepository companyStatRepository;
  private final CompanyAwardRepository companyAwardRepository;
  private final CompanyCertificateRepository companyCertificateRepository;
  private final CompanyMediaRepository companyMediaRepository;
  private final CompanyPricingPlanRepository companyPricingPlanRepository;
  private final CompanyConnectedBrandPublicService companyConnectedBrandPublicService;
  private final PublicReviewService publicReviewService;

  @Override
  public ArchitectDesignerDetailResponse getDetail(Long companyId) {
    CompanyEntity company = companyRepository.findByIdAndActiveTrueAndPublishedTrueAndDeletedFalse(companyId)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Architect/Designer not found"));

    List<CompanyProjectCardDto> topProjects = companyProjectRepository
        .findTop10ByCompany_IdAndPublishedTrueAndActiveTrueAndDeletedFalseOrderByPriorityAscIdDesc(companyId)
        .stream()
        .map(this::toProjectCard)
        .toList();

    List<CompanyStatDto> stats = companyStatRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(companyId)
        .stream()
        .map(s -> CompanyStatDto.builder()
            .id(s.getId())
            .label(s.getLabel())
            .value(s.getValue())
            .iconKey(s.getIconKey())
            .sortOrder(s.getDisplayOrder())
            .build())
        .toList();

    List<CompanyAwardDto> awards = companyAwardRepository
        .findByCompany_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(companyId)
        .stream()
        .map(a -> CompanyAwardDto.builder()
            .id(a.getId())
            .title(a.getTitle())
            .subtitle(a.getSubtitle())
            .description(a.getDescription())
            .displayOrder(a.getDisplayOrder())
            .build())
        .toList();

    List<CompanyCertificateDto> certificates = companyCertificateRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscIdAsc(companyId)
        .stream()
        .map(c -> CompanyCertificateDto.builder()
            .id(c.getId())
            .title(c.getTitle())
            .issuer(c.getIssuer())
            .certificateUrl(c.getCertificateUrl())
            .displayOrder(c.getDisplayOrder())
            .description(c.getDescription())
            .fileUrl(c.getCertificateFileUrl())
            .year(c.getYear())
            .verified(c.getVerified())
            .build())
        .toList();

    List<CompanyHeroImageDto> heroImages = companyMediaRepository
        .findByCompany_IdAndUsageTypeAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(
            companyId, HERO_USAGE_TYPE
        )
        .stream()
        .map(m -> CompanyHeroImageDto.builder()
            .id(m.getId())
            .mediaUrl(m.getMediaUrl())
            .mediaType(m.getMediaType())
            .title(m.getTitle())
            .altText(m.getAltText())
            .sortOrder(m.getSortOrder())
            .build())
        .toList();

    List<CompanyPricingPlanPublicDto> pricingPlans = companyPricingPlanRepository
        .findByCompany_IdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(companyId)
        .stream()
        .map(p -> CompanyPricingPlanPublicDto.builder()
            .id(p.getId())
            .pricingType(p.getPricingType())
            .planName(p.getPlanName())
            .priceAmount(p.getPriceAmount())
            .currency(p.getCurrency())
            .billingUnit(p.getBillingUnit())
            .description(p.getDescription())
            .features(p.getFeatures() != null ? p.getFeatures() : List.of())
            .sortOrder(p.getSortOrder())
            .build())
        .toList();

    PublicReviewSignalResponse signal = publicReviewService.getPublicSignal(PublicReviewTargetType.COMPANY, companyId);
    CompanyReviewSummaryDto reviewSummary = CompanyReviewSummaryDto.builder()
        .rating(signal.getRating())
        .reviewCount(signal.getUserRatingCount())
        .source(mapReviewSource(signal))
        .displayMode(signal.getPlaces().isEmpty() ? null : signal.getPlaces().get(0).getDisplayMode().name())
        .build();

    // Public profile shows a short highlight reel, not the full moderation list -
    // best-rated first, capped at MAX_SAMPLE_REVIEWS. The underlying signal is
    // already filtered to APPROVED_PUBLIC samples only (PublicReviewServiceImpl).
    List<PublicReviewSampleResponse> sampleReviews = signal.getSamples().stream()
        .sorted(Comparator.comparing(
            PublicReviewSampleResponse::getRating,
            Comparator.nullsLast(Comparator.reverseOrder())
        ))
        .limit(MAX_SAMPLE_REVIEWS)
        .toList();

    List<ConnectedBrandDto> connectedBrands = companyConnectedBrandPublicService.getConnectedBrands(companyId);

    return ArchitectDesignerDetailResponse.builder()
        .companyId(company.getId())
        .name(company.getName())
        .slug(company.getSlug())
        .companyType(company.getCompanyType())
        .logoUrl(company.getLogoUrl())
        .thumbnailImageUrl(company.getCoverImageUrl())
        .description(company.getDescription())
        .topProjects(topProjects)
        .stats(stats)
        .awardsAndPublications(awards)
        .certificates(certificates)
        .heroImages(heroImages)
        .pricingPlans(pricingPlans)
        .reviewSummary(reviewSummary)
        .sampleReviews(sampleReviews)
        .connectedBrands(connectedBrands)
        .build();
  }

  private CompanyProjectCardDto toProjectCard(CompanyProjectEntity p) {
    CompanyEntity c = p.getCompany();
    return CompanyProjectCardDto.builder()
        .id(p.getId())
        .name(p.getName())
        .companyId(c != null ? c.getId() : null)
        .companyName(c != null ? c.getName() : null)
        .companyLogoUrl(c != null ? c.getLogoUrl() : null)
        .cityId(p.getCity() != null ? p.getCity().getId() : null)
        .cityName(p.getCity() != null ? p.getCity().getName() : null)
        .addressLine(p.getAddressLine())
        .projectCityLatitude(p.getCity() != null ? p.getCity().getLatitude() : null)
        .projectCityLongitude(p.getCity() != null ? p.getCity().getLongitude() : null)
        .clientName(p.getClientName())
        .projectArea(p.getProjectArea())
        .detail3(p.getDetail3())
        .tags(CompanyProjectTagMapper.toTags(p.getTags()))
        .coverMediaUrl(p.getCoverMediaUrl())
        .coverMediaType(p.getCoverMediaType())
        .build();
  }

  // No place attached yet -> no source to report at all (null, not a guessed
  // default) rather than "GOOGLE" every time, since buildSignal's empty-state
  // response always defaults sourceType to GOOGLE_PLACES regardless of whether
  // a place was ever configured.
  private String mapReviewSource(PublicReviewSignalResponse signal) {
    if (signal.getPlaces().isEmpty()) return null;
    if (signal.getSourceType() == null) return null;
    return switch (signal.getSourceType()) {
      case GOOGLE_PLACES -> "GOOGLE";
      case SFS_REVIEW -> "SFS";
    };
  }
}
