package com.brandPitara.sfs.brand.service.impl;

import com.brandPitara.sfs.brand.dto.BrandProductCategoryResponse;
import com.brandPitara.sfs.brand.dto.BrandProductCategoryUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.entity.BrandProductCategoryEntity;
import com.brandPitara.sfs.brand.repository.BrandProductCategoryRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.BrandProductCategoryService;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BrandProductCategoryServiceImpl implements BrandProductCategoryService {

  private final BrandRepository brandRepository;
  private final BrandProductCategoryRepository brandProductCategoryRepository;
  private final ContentVersionService contentVersionService;

  private static final String KEY_BRANDS = "BRANDS";

  @Override
  @Transactional
  public BrandProductCategoryResponse create(Long brandId, BrandProductCategoryUpsertRequest request) {
    BrandEntity brand = brandRepository.findByIdAndDeletedFalse(brandId)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + brandId));

    String name = clean(request.getName());

    BrandProductCategoryEntity entity = BrandProductCategoryEntity.builder()
        .brand(brand)
        .name(name)
        .slug(resolveSlugForCreate(brandId, clean(request.getSlug()), name))
        .description(clean(request.getDescription()))
        .imageUrl(clean(request.getImageUrl()))
        .externalUrl(clean(request.getExternalUrl()))
        .active(request.getActive() != null ? request.getActive() : true)
        .publicVisible(request.getPublicVisible() != null ? request.getPublicVisible() : true)
        .sortOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
        .deleted(false)
        .build();

    BrandProductCategoryEntity saved = brandProductCategoryRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);

    return toResponse(saved);
  }

  @Override
  @Transactional
  public BrandProductCategoryResponse update(Long brandId, Long categoryId, BrandProductCategoryUpsertRequest request) {
    BrandProductCategoryEntity entity = brandProductCategoryRepository.findByIdAndBrand_IdAndDeletedFalse(categoryId, brandId)
        .orElseThrow(() -> new EntityNotFoundException("Product category not found: " + categoryId));

    if (StringUtils.hasText(request.getName())) entity.setName(clean(request.getName()));
    if (request.getSlug() != null) entity.setSlug(resolveSlugForUpdate(brandId, clean(request.getSlug()), categoryId));
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));
    if (request.getImageUrl() != null) entity.setImageUrl(clean(request.getImageUrl()));
    if (request.getExternalUrl() != null) entity.setExternalUrl(clean(request.getExternalUrl()));
    if (request.getActive() != null) entity.setActive(request.getActive());
    if (request.getPublicVisible() != null) entity.setPublicVisible(request.getPublicVisible());
    if (request.getDisplayOrder() != null) entity.setSortOrder(request.getDisplayOrder());

    BrandProductCategoryEntity saved = brandProductCategoryRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);

    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BrandProductCategoryResponse> adminList(Long brandId, Pageable pageable) {
    return brandProductCategoryRepository.findByBrand_IdAndDeletedFalse(brandId, pageable)
        .map(this::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public BrandProductCategoryResponse adminGetById(Long brandId, Long categoryId) {
    BrandProductCategoryEntity entity = brandProductCategoryRepository.findByIdAndBrand_IdAndDeletedFalse(categoryId, brandId)
        .orElseThrow(() -> new EntityNotFoundException("Product category not found: " + categoryId));
    return toResponse(entity);
  }

  @Override
  @Transactional
  public void softDelete(Long brandId, Long categoryId) {
    BrandProductCategoryEntity entity = brandProductCategoryRepository.findByIdAndBrand_IdAndDeletedFalse(categoryId, brandId)
        .orElseThrow(() -> new EntityNotFoundException("Product category not found: " + categoryId));

    entity.setDeleted(true);
    entity.setActive(false);
    brandProductCategoryRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);
  }

  // ---------- helpers ----------

  private String resolveSlugForCreate(Long brandId, String requestedSlug, String name) {
    if (StringUtils.hasText(requestedSlug)) {
      brandProductCategoryRepository.findByBrand_IdAndSlugAndDeletedFalse(brandId, requestedSlug).ifPresent(existing -> {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Product category slug already exists for this brand: " + requestedSlug);
      });
      return requestedSlug;
    }
    return generateUniqueSlug(brandId, name);
  }

  private String resolveSlugForUpdate(Long brandId, String requestedSlug, Long categoryId) {
    if (!StringUtils.hasText(requestedSlug)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slug cannot be blank");
    }
    brandProductCategoryRepository.findByBrand_IdAndSlugAndIdNotAndDeletedFalse(brandId, requestedSlug, categoryId).ifPresent(existing -> {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Product category slug already exists for this brand: " + requestedSlug);
    });
    return requestedSlug;
  }

  private String generateUniqueSlug(Long brandId, String name) {
    String base = slugify(name);
    String candidate = base;
    int suffix = 2;
    while (brandProductCategoryRepository.findByBrand_IdAndSlugAndDeletedFalse(brandId, candidate).isPresent()) {
      candidate = base + "-" + suffix++;
    }
    return candidate;
  }

  private String slugify(String input) {
    String base = input == null ? "" : input.toLowerCase().trim()
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-+|-+$)", "");
    return base.isEmpty() ? "category" : base;
  }

  private BrandProductCategoryResponse toResponse(BrandProductCategoryEntity e) {
    return BrandProductCategoryResponse.builder()
        .id(e.getId())
        .brandId(e.getBrand().getId())
        .name(e.getName())
        .slug(e.getSlug())
        .description(e.getDescription())
        .imageUrl(e.getImageUrl())
        .externalUrl(e.getExternalUrl())
        .active(Boolean.TRUE.equals(e.getActive()))
        .publicVisible(Boolean.TRUE.equals(e.getPublicVisible()))
        .displayOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .build();
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
