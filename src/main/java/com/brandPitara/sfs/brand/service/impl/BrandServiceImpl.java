package com.brandPitara.sfs.brand.service.impl;

import com.brandPitara.sfs.brand.dto.*;
import com.brandPitara.sfs.brand.entity.BrandCategoryLinkEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.entity.BrandMediaEntity;
import com.brandPitara.sfs.brand.entity.BrandMediaEntity.Placement;
import com.brandPitara.sfs.brand.repository.BrandCategoryLinkRepository;
import com.brandPitara.sfs.brand.repository.BrandDistributorRepository;
import com.brandPitara.sfs.brand.repository.BrandMediaRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.BrandService;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.distributor.dto.DistributorCardResponse;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Year;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

  private final BrandRepository brandRepository;
  private final BrandMediaRepository brandMediaRepository;
  private final BrandDistributorRepository brandDistributorRepository;
  private final BrandCategoryLinkRepository brandCategoryLinkRepository;
  private final CategoryRepository categoryRepository;
  private final ContentVersionService contentVersionService;

  // Keys for version bumps
  private static final String KEY_BRANDS = "BRANDS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional
  public BrandResponse create(BrandUpsertRequest request) {
    String name = clean(request.getName());
    BrandEntity entity = BrandEntity.builder()
        .name(name)
        .logoUrl(clean(request.getLogoUrl()))
        .description(clean(request.getDescription()))
        .slug(resolveSlugForCreate(clean(request.getSlug()), name))
        .heroImageUrl(clean(request.getHeroImageUrl()))
        .shortDescription(clean(request.getShortDescription()))
        .foundedYear(request.getFoundedYear())
        .customerRating(request.getCustomerRating())
        .customerRatingCount(request.getCustomerRatingCount())
        .priority(request.getPriority() != null ? request.getPriority() : 0)
        .active(request.getActive() != null ? request.getActive() : true)
        .published(false)
        .deleted(false)
        .promoEnabled(request.getPromoEnabled() != null ? request.getPromoEnabled() : false)
        .promoMediaType(request.getPromoMediaType())
        .promoMediaUrl(clean(request.getPromoMediaUrl()))
        .build();

    BrandEntity saved = brandRepository.save(entity);

    replaceCategoryLinks(saved, request.getCategoryIds());

    // Admin-side change; customer visibility after publish
    contentVersionService.bump(KEY_BRANDS);

    return toResponse(saved);
  }

  @Override
  @Transactional
  public BrandResponse update(Long id, BrandUpsertRequest request) {
    BrandEntity entity = brandRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + id));

    if (StringUtils.hasText(request.getName())) entity.setName(clean(request.getName()));
    if (request.getLogoUrl() != null) entity.setLogoUrl(clean(request.getLogoUrl()));
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));
    if (request.getSlug() != null) entity.setSlug(resolveSlugForUpdate(clean(request.getSlug()), id));
    if (request.getHeroImageUrl() != null) entity.setHeroImageUrl(clean(request.getHeroImageUrl()));
    if (request.getShortDescription() != null) entity.setShortDescription(clean(request.getShortDescription()));
    if (request.isFoundedYearPresent()) entity.setFoundedYear(request.getFoundedYear());
    if (request.isCustomerRatingPresent()) entity.setCustomerRating(request.getCustomerRating());
    if (request.isCustomerRatingCountPresent()) entity.setCustomerRatingCount(request.getCustomerRatingCount());
    if (request.getPriority() != null) entity.setPriority(request.getPriority());
    if (request.getActive() != null) entity.setActive(request.getActive());

    if (request.getPromoEnabled() != null) entity.setPromoEnabled(request.getPromoEnabled());
    if (request.getPromoMediaType() != null) entity.setPromoMediaType(request.getPromoMediaType());
    if (request.getPromoMediaUrl() != null) entity.setPromoMediaUrl(clean(request.getPromoMediaUrl()));

    BrandEntity saved = brandRepository.save(entity);

    replaceCategoryLinks(saved, request.getCategoryIds());

    contentVersionService.bump(KEY_BRANDS);
    if (Boolean.TRUE.equals(saved.getPublished()) && Boolean.TRUE.equals(saved.getActive())) {
      contentVersionService.bump(KEY_HOME);
    }

    return toResponse(saved);
  }

  @Override
  @Transactional
  public BrandResponse setPublished(Long id, boolean published) {
    BrandEntity entity = brandRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + id));

    entity.setPublished(published);
    BrandEntity saved = brandRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);
    contentVersionService.bump(KEY_HOME);

    return toResponse(saved);
  }

  @Override
  @Transactional
  public void softDelete(Long id) {
    BrandEntity entity = brandRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + id));

    entity.setDeleted(true);
    entity.setPublished(false);
    brandRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);
    contentVersionService.bump(KEY_HOME);
  }

  @Override
  @Transactional(readOnly = true)
  public BrandResponse adminGetById(Long id) {
    BrandEntity entity = brandRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + id));
    return toResponse(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public BrandPublicResponse getById(Long id) {
    BrandEntity entity = brandRepository.findByIdAndPublishedTrueAndActiveTrueAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + id));
    return toPublicResponse(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BrandResponse> adminList(Boolean published, Boolean active, Pageable pageable) {
    Page<BrandEntity> page;

    if (published == null && active == null) {
      page = brandRepository.findByDeletedFalse(pageable);
    } else if (published != null && active == null) {
      page = brandRepository.findByPublishedAndDeletedFalse(published, pageable);
    } else if (published == null) {
      page = brandRepository.findByActiveAndDeletedFalse(active, pageable);
    } else {
      page = brandRepository.findByPublishedAndActiveAndDeletedFalse(published, active, pageable);
    }

    // Batch-load category links for every brand on this page in one query instead of
    // one query per row (toResponse's single-brand overload is fine for create/update/get).
    List<Long> brandIds = page.getContent().stream().map(BrandEntity::getId).toList();
    Map<Long, List<Long>> categoryIdsByBrandId = brandCategoryLinkRepository
        .findByBrand_IdInAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(brandIds)
        .stream()
        .collect(Collectors.groupingBy(
            link -> link.getBrand().getId(),
            LinkedHashMap::new,
            Collectors.mapping(link -> link.getCategory().getId(), Collectors.toList())
        ));

    return page.map(brand -> toResponse(brand, categoryIdsByBrandId.getOrDefault(brand.getId(), List.of())));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BrandPublicResponse> listPublished(Pageable pageable) {
    return brandRepository
        .findByPublishedTrueAndActiveTrueAndDeletedFalse(pageable)
        .map(this::toPublicResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public BrandPromoResponse getPromo(Long id) {
    BrandEntity e = brandRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + id));

    boolean visible = Boolean.TRUE.equals(e.getPublished())
        && Boolean.TRUE.equals(e.getActive())
        && !Boolean.TRUE.equals(e.getDeleted());

    if (!visible) {
      throw new EntityNotFoundException("Brand not found: " + id);
    }

    return BrandPromoResponse.builder()
        .id(e.getId())
        .promoEnabled(Boolean.TRUE.equals(e.getPromoEnabled()))
        .promoMediaType(e.getPromoMediaType())
        .promoMediaUrl(e.getPromoMediaUrl())
        .build();
  }

  // ✅ NEW: Brand detail page aggregate
  @Override
  @Transactional(readOnly = true)
  public BrandDetailPageResponse getBrandPage(Long brandId, Long cityId) {
    BrandEntity brand = brandRepository.findByIdAndDeletedFalse(brandId)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + brandId));

    // public visibility check
    boolean visible = Boolean.TRUE.equals(brand.getPublished())
        && Boolean.TRUE.equals(brand.getActive())
        && !Boolean.TRUE.equals(brand.getDeleted());

    if (!visible) {
      throw new EntityNotFoundException("Brand not found: " + brandId);
    }

    BrandPublicResponse brandDto = toPublicResponse(brand);

    List<BrandMediaResponse> banners = brandMediaRepository
        .findByBrandIdAndPlacementAndDeletedFalseAndActiveTrueOrderBySortOrderAsc(brandId, Placement.BANNER)
        .stream()
        .map(this::toMediaResponse)
        .toList();

    Page<DistributorCardResponse> topPage = brandDistributorRepository.findPublicDistributorCardsByBrand(
        brandId,
        cityId,
        PageRequest.of(0, 10) // top distributors in first paint
    );

    return BrandDetailPageResponse.builder()
        .brand(brandDto)
        .offerLine(deriveOfferLine(brand, topPage.getContent()))
        .banners(banners)
        .topDistributors(topPage.getContent())
        .distributorCount(topPage.getTotalElements())
        .build();
  }

  // ---------- helpers ----------

  private BrandPublicResponse toPublicResponse(BrandEntity e) {
    return BrandPublicResponse.builder()
        .id(e.getId())
        .name(e.getName())
        .logoUrl(e.getLogoUrl())
        .description(e.getDescription())
        .build();
  }

  private BrandResponse toResponse(BrandEntity e) {
    List<Long> categoryIds = brandCategoryLinkRepository
        .findByBrand_IdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(e.getId())
        .stream()
        .map(link -> link.getCategory().getId())
        .toList();
    return toResponse(e, categoryIds);
  }

  private BrandResponse toResponse(BrandEntity e, List<Long> categoryIds) {
    return BrandResponse.builder()
        .id(e.getId())
        .name(e.getName())
        .logoUrl(e.getLogoUrl())
        .description(e.getDescription())
        .slug(e.getSlug())
        .heroImageUrl(e.getHeroImageUrl())
        .shortDescription(e.getShortDescription())
        .foundedYear(e.getFoundedYear())
        .yearsInIndustry(calculateYearsInIndustry(e.getFoundedYear()))
        .customerRating(e.getCustomerRating())
        .customerRatingCount(e.getCustomerRatingCount())
        .categoryIds(categoryIds)
        .active(Boolean.TRUE.equals(e.getActive()))
        .published(Boolean.TRUE.equals(e.getPublished()))
        .priority(e.getPriority() != null ? e.getPriority() : 0)
        .promoEnabled(Boolean.TRUE.equals(e.getPromoEnabled()))
        .promoMediaType(e.getPromoMediaType())
        .promoMediaUrl(e.getPromoMediaUrl())
        .build();
  }

  private Integer calculateYearsInIndustry(Integer foundedYear) {
    if (foundedYear == null) return null;
    return Math.max(0, Year.now().getValue() - foundedYear);
  }

  // ---------- slug helpers ----------

  private String resolveSlugForCreate(String requestedSlug, String name) {
    if (StringUtils.hasText(requestedSlug)) {
      brandRepository.findBySlug(requestedSlug).ifPresent(existing -> {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Brand slug already exists: " + requestedSlug);
      });
      return requestedSlug;
    }
    return generateUniqueSlug(name);
  }

  private String resolveSlugForUpdate(String requestedSlug, Long brandId) {
    if (!StringUtils.hasText(requestedSlug)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slug cannot be blank");
    }
    brandRepository.findBySlugAndIdNot(requestedSlug, brandId).ifPresent(existing -> {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Brand slug already exists: " + requestedSlug);
    });
    return requestedSlug;
  }

  private String generateUniqueSlug(String name) {
    String base = slugify(name);
    String candidate = base;
    int suffix = 2;
    while (brandRepository.findBySlug(candidate).isPresent()) {
      candidate = base + "-" + suffix++;
    }
    return candidate;
  }

  private String slugify(String input) {
    String base = input == null ? "" : input.toLowerCase().trim()
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-+|-+$)", "");
    return base.isEmpty() ? "brand" : base;
  }

  // ---------- category link helpers ----------
  /**
 * Full replace of this brand's category tags when categoryIds is non-null.
 *
 * Contract:
 * - categoryIds == null means "leave existing tags untouched".
 * - empty categoryIds means "remove all category tags".
 * - existing links are reactivated instead of reinserted, because the DB unique
 *   constraint is on (brand_id, category_id) and includes soft-deleted rows.
 */
private void replaceCategoryLinks(BrandEntity brand, List<Long> categoryIds) {
  if (categoryIds == null) return;

  Map<Long, CategoryEntity> requestedCategories = new LinkedHashMap<>();
  for (Long categoryId : categoryIds) {
    if (categoryId == null || requestedCategories.containsKey(categoryId)) continue;

    CategoryEntity category = categoryRepository.findByIdAndActiveTrue(categoryId)
        .orElseThrow(() -> new EntityNotFoundException("Category not found or inactive: " + categoryId));

    requestedCategories.put(categoryId, category);
  }

  List<BrandCategoryLinkEntity> existingLinks = brandCategoryLinkRepository
      .findByBrand_IdOrderBySortOrderAscIdAsc(brand.getId());

  Map<Long, BrandCategoryLinkEntity> existingByCategoryId = existingLinks.stream()
      .collect(Collectors.toMap(
          link -> link.getCategory().getId(),
          link -> link,
          (first, second) -> first,
          LinkedHashMap::new
      ));

  for (BrandCategoryLinkEntity existing : existingLinks) {
    Long existingCategoryId = existing.getCategory().getId();

    if (!requestedCategories.containsKey(existingCategoryId)) {
      existing.setDeleted(true);
      existing.setActive(false);
      brandCategoryLinkRepository.save(existing);
    }
  }

  int order = 0;
  for (Map.Entry<Long, CategoryEntity> entry : requestedCategories.entrySet()) {
    Long categoryId = entry.getKey();
    CategoryEntity category = entry.getValue();

    BrandCategoryLinkEntity link = existingByCategoryId.get(categoryId);

    if (link == null) {
      link = BrandCategoryLinkEntity.builder()
          .brand(brand)
          .category(category)
          .build();
    } else {
      link.setDeleted(false);
      link.setActive(true);
    }

    link.setSortOrder(order++);
    brandCategoryLinkRepository.save(link);
  }
}



  private BrandMediaResponse toMediaResponse(BrandMediaEntity m) {
    return BrandMediaResponse.builder()
        .id(m.getId())
        .mediaType(m.getMediaType())
        .placement(m.getPlacement())
        .url(m.getUrl())
        .caption(m.getCaption())
        .sortOrder(m.getSortOrder() != null ? m.getSortOrder() : 0)
        .actionType(m.getActionType())
        .actionValue(m.getActionValue())
        .build();
  }

  private String deriveOfferLine(BrandEntity brand, List<DistributorCardResponse> topDistributors) {
    // Simple & safe default: use brand description snippet OR first distributor offer title.
    if (StringUtils.hasText(brand.getDescription())) {
      String d = brand.getDescription().trim();
      return d.length() <= 80 ? d : d.substring(0, 80) + "...";
    }
    if (topDistributors != null) {
      for (var d : topDistributors) {
        if (StringUtils.hasText(d.getOfferTitle())) return d.getOfferTitle().trim();
      }
    }
    return null;
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
