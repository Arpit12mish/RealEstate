package com.brandPitara.sfs.brand.service.impl;

import com.brandPitara.sfs.brand.dto.*;
import com.brandPitara.sfs.brand.entity.BrandCertificateEntity;
import com.brandPitara.sfs.brand.entity.BrandCollaborationEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.entity.BrandFaqEntity;
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
import com.brandPitara.sfs.brand.service.BrandPublicService;
import com.brandPitara.sfs.brand.util.BrandCompanyClassifier;
import com.brandPitara.sfs.company.entity.CompanyEntity;
import com.brandPitara.sfs.entity.CategoryEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandPublicServiceImpl implements BrandPublicService {

  private static final int LATEST_PRODUCTS_LIMIT = 10;
  private static final int RELATED_BRANDS_LIMIT = 10;

  private final BrandRepository brandRepository;
  private final BrandCategoryLinkRepository brandCategoryLinkRepository;
  private final BrandSkuRepository brandSkuRepository;
  private final BrandCertificateRepository brandCertificateRepository;
  private final BrandFaqRepository brandFaqRepository;
  private final BrandCollaborationRepository brandCollaborationRepository;
  private final BrandProductCategoryRepository brandProductCategoryRepository;

  @Override
  @Transactional(readOnly = true)
  public List<PublicBrandCategoryResponse> listPublicCategories() {
    return brandCategoryLinkRepository.findDistinctPublicCategories()
        .stream()
        .map(this::toCategoryResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PublicBrandCardResponse> listPublicBrands(Long categoryId, String q, Pageable pageable) {
    // Never pass a null/blank q into the lower(:q)-based search query - Postgres can't infer
    // a type for a null bound only inside lower(...)/concat(...) (see BrandRepository).
    Page<BrandEntity> page = StringUtils.hasText(q)
        ? brandRepository.searchPublicBrands(categoryId, q.trim().toLowerCase(), pageable)
        : brandRepository.findPublicBrands(categoryId, pageable);

    List<Long> brandIds = page.getContent().stream().map(BrandEntity::getId).toList();

    Map<Long, List<PublicBrandCategoryResponse>> categoriesByBrandId = batchCategoriesByBrandIds(brandIds);
    Map<Long, Long> productCountsByBrandId = batchProductCounts(brandIds);
    Map<Long, StatsAccumulator> statsByBrandId = batchCollaborationStats(brandIds);

    return page.map(brand -> toCardResponse(
        brand,
        categoriesByBrandId.getOrDefault(brand.getId(), List.of()),
        productCountsByBrandId.getOrDefault(brand.getId(), 0L),
        statsByBrandId.getOrDefault(brand.getId(), new StatsAccumulator())
    ));
  }

  @Override
  @Transactional(readOnly = true)
  public PublicBrandDetailResponse getPublicBrandBySlug(String slug) {
    BrandEntity brand = brandRepository.findBySlugAndPublishedTrueAndActiveTrueAndDeletedFalse(slug)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + slug));

    Long brandId = brand.getId();

    List<PublicBrandCategoryResponse> categories = brandCategoryLinkRepository
        .findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(brandId)
        .stream()
        .map(link -> toCategoryResponse(link.getCategory()))
        .toList();

    List<PublicBrandSkuResponse> latestProducts = brandSkuRepository
        .findByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(brandId, latestProductsSort())
        .getContent()
        .stream()
        .map(this::toSkuResponse)
        .toList();

    List<PublicBrandProductCategoryResponse> productCategories = brandProductCategoryRepository
        .findByBrand_IdAndActiveTrueAndPublicVisibleTrueAndDeletedFalseOrderBySortOrderAscIdAsc(brandId)
        .stream()
        .map(this::toProductCategoryResponse)
        .toList();

    long productsCount = brandSkuRepository.countByBrand_IdAndPublishedTrueAndActiveTrueAndDeletedFalse(brandId);

    List<PublicBrandCertificateResponse> certificates = brandCertificateRepository
        .findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(brandId)
        .stream()
        .map(this::toCertificateResponse)
        .toList();

    List<PublicBrandFaqResponse> faqs = brandFaqRepository
        .findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(brandId)
        .stream()
        .map(this::toFaqResponse)
        .toList();

    List<BrandCollaborationEntity> projectCollaborations = brandCollaborationRepository
        .findPublicProjectCollaborationsByBrandId(brandId);
    List<PublicCollaborationTargetSummary> topProjects = projectCollaborations.stream()
        .map(c -> toTargetSummary(c.getProject().getId(), c.getProject().getName(), null, c))
        .toList();

    List<BrandCollaborationEntity> builderCollaborations = brandCollaborationRepository
        .findPublicBuilderCollaborationsByBrandId(brandId);
    List<PublicCollaborationTargetSummary> topBuilders = builderCollaborations.stream()
        .map(c -> toTargetSummary(c.getBuilder().getId(), c.getBuilder().getName(), c.getBuilder().getLogoUrl(), c))
        .toList();

    List<BrandCollaborationEntity> companyCollaborations = brandCollaborationRepository
        .findPublicCompanyCollaborationsByBrandId(brandId);
    List<PublicCollaborationTargetSummary> topArchitects = new ArrayList<>();
    List<PublicCollaborationTargetSummary> interiorDesigners = new ArrayList<>();
    for (BrandCollaborationEntity c : companyCollaborations) {
      CompanyEntity company = c.getCompany();
      PublicCollaborationTargetSummary summary = toTargetSummary(company.getId(), company.getName(), company.getLogoUrl(), c);
      String companyType = company.getCompanyType();
      // Independent checks, not mutually exclusive - see BrandCompanyClassifier.
      if (BrandCompanyClassifier.isArchitect(companyType)) topArchitects.add(summary);
      if (BrandCompanyClassifier.isDesigner(companyType)) interiorDesigners.add(summary);
    }

    PublicBrandStatsResponse stats = PublicBrandStatsResponse.builder()
        .yearsInIndustry(calculateYearsInIndustry(brand.getFoundedYear()))
        .customerRating(brand.getCustomerRating())
        .customerRatingCount(brand.getCustomerRatingCount())
        .projectsCount(projectCollaborations.size())
        .buildersCount(builderCollaborations.size())
        .productsCount(productsCount)
        .architectsCount(topArchitects.size())
        .designersCount(interiorDesigners.size())
        .build();

    List<Long> categoryIds = categories.stream().map(PublicBrandCategoryResponse::getId).toList();
    List<PublicRelatedBrandResponse> relatedBrands = categoryIds.isEmpty()
        ? List.of()
        : brandRepository.findRelatedBrands(categoryIds, brandId, PageRequest.of(0, RELATED_BRANDS_LIMIT))
            .stream()
            .map(this::toRelatedBrandResponse)
            .toList();

    return PublicBrandDetailResponse.builder()
        .id(brand.getId())
        .name(brand.getName())
        .slug(brand.getSlug())
        .logoUrl(brand.getLogoUrl())
        .heroImageUrl(brand.getHeroImageUrl())
        .shortDescription(brand.getShortDescription())
        .description(brand.getDescription())
        .foundedYear(brand.getFoundedYear())
        .yearsInIndustry(calculateYearsInIndustry(brand.getFoundedYear()))
        .customerRating(brand.getCustomerRating())
        .customerRatingCount(brand.getCustomerRatingCount())
        .categories(categories)
        .stats(stats)
        .productCategories(productCategories)
        .latestProducts(latestProducts)
        .topProjects(topProjects)
        .topBuilders(topBuilders)
        .topArchitects(topArchitects)
        .interiorDesigners(interiorDesigners)
        .certificates(certificates)
        .faqs(faqs)
        .relatedBrands(relatedBrands)
        .build();
  }

  private Integer calculateYearsInIndustry(Integer foundedYear) {
    if (foundedYear == null) return null;
    return Math.max(0, Year.now().getValue() - foundedYear);
  }

  // ---------- batch helpers (listPublicBrands) ----------

  private Map<Long, List<PublicBrandCategoryResponse>> batchCategoriesByBrandIds(Collection<Long> brandIds) {
    if (brandIds.isEmpty()) return Map.of();
    return brandCategoryLinkRepository
        .findByBrand_IdInAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(brandIds)
        .stream()
        .collect(Collectors.groupingBy(
            link -> link.getBrand().getId(),
            LinkedHashMap::new,
            Collectors.mapping(link -> toCategoryResponse(link.getCategory()), Collectors.toList())
        ));
  }

  private Map<Long, Long> batchProductCounts(Collection<Long> brandIds) {
    if (brandIds.isEmpty()) return Map.of();
    Map<Long, Long> result = new HashMap<>();
    for (Object[] row : brandSkuRepository.countPublicByBrandIds(brandIds)) {
      result.put((Long) row[0], (Long) row[1]);
    }
    return result;
  }

  private Map<Long, StatsAccumulator> batchCollaborationStats(Collection<Long> brandIds) {
    Map<Long, StatsAccumulator> result = new HashMap<>();
    if (brandIds.isEmpty()) return result;

    for (Object[] row : brandCollaborationRepository.findPublicStatRowsByBrandIds(brandIds)) {
      Long brandId = (Long) row[0];
      BrandCollaborationTargetType targetType = (BrandCollaborationTargetType) row[1];
      String companyType = (String) row[2];
      long count = ((Number) row[3]).longValue();

      StatsAccumulator acc = result.computeIfAbsent(brandId, id -> new StatsAccumulator());
      switch (targetType) {
        case PROJECT -> acc.projects += count;
        case BUILDER -> acc.builders += count;
        case COMPANY -> {
          if (BrandCompanyClassifier.isArchitect(companyType)) acc.architects += count;
          if (BrandCompanyClassifier.isDesigner(companyType)) acc.designers += count;
        }
        case BUSINESS -> {
          // not surfaced as a stat field on the card/detail response
        }
      }
    }
    return result;
  }

  private static final class StatsAccumulator {
    long projects;
    long builders;
    long architects;
    long designers;
  }

  // ---------- mapping helpers ----------

  private Pageable latestProductsSort() {
    return PageRequest.of(0, LATEST_PRODUCTS_LIMIT, Sort.by(
        Sort.Order.desc("latest"),
        Sort.Order.desc("featured"),
        Sort.Order.asc("sortOrder"),
        Sort.Order.desc("id")
    ));
  }

  private PublicBrandCategoryResponse toCategoryResponse(CategoryEntity c) {
    return PublicBrandCategoryResponse.builder()
        .id(c.getId())
        .name(c.getName())
        .slug(c.getSlug())
        .iconUrl(null) // CategoryEntity has no icon field today
        .displayOrder(c.getPriority())
        .build();
  }

  private PublicBrandCardResponse toCardResponse(
      BrandEntity b,
      List<PublicBrandCategoryResponse> categories,
      long productsCount,
      StatsAccumulator stats
  ) {
    return PublicBrandCardResponse.builder()
        .id(b.getId())
        .name(b.getName())
        .slug(b.getSlug())
        .logoUrl(b.getLogoUrl())
        .heroImageUrl(b.getHeroImageUrl())
        .shortDescription(b.getShortDescription())
        .categories(categories)
        .productsCount(productsCount)
        .projectsCount(stats.projects)
        .buildersCount(stats.builders)
        .designersCount(stats.designers)
        .architectsCount(stats.architects)
        .promoEnabled(Boolean.TRUE.equals(b.getPromoEnabled()))
        .promoMediaType(b.getPromoMediaType())
        .promoMediaUrl(b.getPromoMediaUrl())
        .build();
  }

  private PublicBrandSkuResponse toSkuResponse(BrandSkuEntity s) {
    CategoryEntity category = s.getCategory();
    String externalUrl = s.getExternalUrl();
    return PublicBrandSkuResponse.builder()
        .id(s.getId())
        .name(s.getName())
        .slug(s.getSlug())
        .skuCode(s.getSkuCode())
        .categoryId(category != null ? category.getId() : null)
        .categoryName(category != null ? category.getName() : null)
        .shortDescription(s.getShortDescription())
        .imageUrl(s.getImageUrl())
        .priceLabel(s.getPriceLabel())
        .featured(Boolean.TRUE.equals(s.getFeatured()))
        .latest(Boolean.TRUE.equals(s.getLatest()))
        .externalUrl(externalUrl)
        .ctaLabel(StringUtils.hasText(externalUrl) ? "View Product" : null)
        .build();
  }

  private PublicBrandProductCategoryResponse toProductCategoryResponse(BrandProductCategoryEntity c) {
    return PublicBrandProductCategoryResponse.builder()
        .id(c.getId())
        .name(c.getName())
        .slug(c.getSlug())
        .description(c.getDescription())
        .imageUrl(c.getImageUrl())
        .externalUrl(c.getExternalUrl())
        .sortOrder(c.getSortOrder() != null ? c.getSortOrder() : 0)
        .build();
  }

  private PublicBrandCertificateResponse toCertificateResponse(BrandCertificateEntity e) {
    return PublicBrandCertificateResponse.builder()
        .id(e.getId())
        .title(e.getTitle())
        .issuer(e.getIssuer())
        .certificateUrl(e.getCertificateUrl())
        .displayOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .build();
  }

  private PublicBrandFaqResponse toFaqResponse(BrandFaqEntity e) {
    return PublicBrandFaqResponse.builder()
        .id(e.getId())
        .question(e.getQuestion())
        .answer(e.getAnswer())
        .displayOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .build();
  }

  private PublicCollaborationTargetSummary toTargetSummary(Long id, String name, String logoUrl, BrandCollaborationEntity c) {
    return PublicCollaborationTargetSummary.builder()
        .id(id)
        .name(name)
        .logoUrl(logoUrl)
        .relationType(c.getRelationType())
        .verified(Boolean.TRUE.equals(c.getVerified()))
        .featured(Boolean.TRUE.equals(c.getFeatured()))
        .build();
  }

  private PublicRelatedBrandResponse toRelatedBrandResponse(BrandEntity b) {
    return PublicRelatedBrandResponse.builder()
        .id(b.getId())
        .name(b.getName())
        .slug(b.getSlug())
        .logoUrl(b.getLogoUrl())
        .shortDescription(b.getShortDescription())
        .build();
  }
}
