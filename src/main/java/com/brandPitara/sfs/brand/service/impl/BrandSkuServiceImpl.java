package com.brandPitara.sfs.brand.service.impl;

import com.brandPitara.sfs.brand.dto.BrandSkuResponse;
import com.brandPitara.sfs.brand.dto.BrandSkuUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.entity.BrandProductCategoryEntity;
import com.brandPitara.sfs.brand.entity.BrandSkuEntity;
import com.brandPitara.sfs.brand.repository.BrandProductCategoryRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.repository.BrandSkuRepository;
import com.brandPitara.sfs.brand.service.BrandSkuService;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.repository.CategoryRepository;
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
public class BrandSkuServiceImpl implements BrandSkuService {

  private final BrandRepository brandRepository;
  private final BrandSkuRepository brandSkuRepository;
  private final CategoryRepository categoryRepository;
  private final BrandProductCategoryRepository brandProductCategoryRepository;
  private final ContentVersionService contentVersionService;

  private static final String KEY_BRANDS = "BRANDS";

  @Override
  @Transactional
  public BrandSkuResponse create(Long brandId, BrandSkuUpsertRequest request) {
    BrandEntity brand = brandRepository.findByIdAndDeletedFalse(brandId)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + brandId));

    CategoryEntity category = resolveCategory(request.getCategoryId());
    BrandProductCategoryEntity productCategory = resolveProductCategory(brandId, request.getProductCategoryId());
    String name = clean(request.getName());

    BrandSkuEntity entity = BrandSkuEntity.builder()
        .brand(brand)
        .category(category)
        .productCategory(productCategory)
        .name(name)
        .slug(resolveSlugForCreate(brandId, clean(request.getSlug()), name))
        .skuCode(clean(request.getSkuCode()))
        .shortDescription(clean(request.getShortDescription()))
        .description(clean(request.getDescription()))
        .imageUrl(clean(request.getImageUrl()))
        .priceLabel(clean(request.getPriceLabel()))
        .externalUrl(clean(request.getExternalUrl()))
        .featured(request.getFeatured() != null ? request.getFeatured() : false)
        .latest(request.getLatest() != null ? request.getLatest() : false)
        .sortOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
        .published(request.getPublished() != null ? request.getPublished() : false)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    BrandSkuEntity saved = brandSkuRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);

    return toResponse(saved);
  }

  @Override
  @Transactional
  public BrandSkuResponse update(Long brandId, Long skuId, BrandSkuUpsertRequest request) {
    BrandSkuEntity entity = brandSkuRepository.findByIdAndBrand_IdAndDeletedFalse(skuId, brandId)
        .orElseThrow(() -> new EntityNotFoundException("SKU not found: " + skuId));

    if (StringUtils.hasText(request.getName())) entity.setName(clean(request.getName()));
    if (request.getSlug() != null) entity.setSlug(resolveSlugForUpdate(brandId, clean(request.getSlug()), skuId));
    if (request.getCategoryId() != null) entity.setCategory(resolveCategory(request.getCategoryId()));
    if (request.getProductCategoryId() != null) {
      entity.setProductCategory(resolveProductCategory(brandId, request.getProductCategoryId()));
    }
    if (request.getSkuCode() != null) entity.setSkuCode(clean(request.getSkuCode()));
    if (request.getShortDescription() != null) entity.setShortDescription(clean(request.getShortDescription()));
    if (request.getDescription() != null) entity.setDescription(clean(request.getDescription()));
    if (request.getImageUrl() != null) entity.setImageUrl(clean(request.getImageUrl()));
    if (request.getPriceLabel() != null) entity.setPriceLabel(clean(request.getPriceLabel()));
    if (request.getExternalUrl() != null) entity.setExternalUrl(clean(request.getExternalUrl()));
    if (request.getFeatured() != null) entity.setFeatured(request.getFeatured());
    if (request.getLatest() != null) entity.setLatest(request.getLatest());
    if (request.getDisplayOrder() != null) entity.setSortOrder(request.getDisplayOrder());
    if (request.getPublished() != null) entity.setPublished(request.getPublished());
    if (request.getActive() != null) entity.setActive(request.getActive());

    BrandSkuEntity saved = brandSkuRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);

    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BrandSkuResponse> adminList(Long brandId, Pageable pageable) {
    return brandSkuRepository.findByBrand_IdAndDeletedFalse(brandId, pageable)
        .map(this::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public BrandSkuResponse adminGetById(Long brandId, Long skuId) {
    BrandSkuEntity entity = brandSkuRepository.findByIdAndBrand_IdAndDeletedFalse(skuId, brandId)
        .orElseThrow(() -> new EntityNotFoundException("SKU not found: " + skuId));
    return toResponse(entity);
  }

  @Override
  @Transactional
  public void softDelete(Long brandId, Long skuId) {
    BrandSkuEntity entity = brandSkuRepository.findByIdAndBrand_IdAndDeletedFalse(skuId, brandId)
        .orElseThrow(() -> new EntityNotFoundException("SKU not found: " + skuId));

    entity.setDeleted(true);
    entity.setPublished(false);
    brandSkuRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);
  }

  // ---------- helpers ----------

  private CategoryEntity resolveCategory(Long categoryId) {
    if (categoryId == null) return null;
    return categoryRepository.findByIdAndActiveTrue(categoryId)
        .orElseThrow(() -> new EntityNotFoundException("Category not found or inactive: " + categoryId));
  }

  // Must belong to the same brand as the SKU - a product category is brand-scoped, so
  // silently accepting another brand's category id would let a SKU appear grouped under
  // a category it has no business relationship to.
  private BrandProductCategoryEntity resolveProductCategory(Long brandId, Long productCategoryId) {
    if (productCategoryId == null) return null;
    return brandProductCategoryRepository.findByIdAndBrand_IdAndDeletedFalse(productCategoryId, brandId)
        .orElseThrow(() -> new EntityNotFoundException(
            "Product category not found for this brand: " + productCategoryId));
  }

  private String resolveSlugForCreate(Long brandId, String requestedSlug, String name) {
    if (StringUtils.hasText(requestedSlug)) {
      brandSkuRepository.findByBrand_IdAndSlugAndDeletedFalse(brandId, requestedSlug).ifPresent(existing -> {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU slug already exists for this brand: " + requestedSlug);
      });
      return requestedSlug;
    }
    return generateUniqueSlug(brandId, name);
  }

  private String resolveSlugForUpdate(Long brandId, String requestedSlug, Long skuId) {
    if (!StringUtils.hasText(requestedSlug)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slug cannot be blank");
    }
    brandSkuRepository.findByBrand_IdAndSlugAndIdNotAndDeletedFalse(brandId, requestedSlug, skuId).ifPresent(existing -> {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU slug already exists for this brand: " + requestedSlug);
    });
    return requestedSlug;
  }

  private String generateUniqueSlug(Long brandId, String name) {
    String base = slugify(name);
    String candidate = base;
    int suffix = 2;
    while (brandSkuRepository.findByBrand_IdAndSlugAndDeletedFalse(brandId, candidate).isPresent()) {
      candidate = base + "-" + suffix++;
    }
    return candidate;
  }

  private String slugify(String input) {
    String base = input == null ? "" : input.toLowerCase().trim()
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-+|-+$)", "");
    return base.isEmpty() ? "sku" : base;
  }

  private BrandSkuResponse toResponse(BrandSkuEntity e) {
    CategoryEntity category = e.getCategory();
    BrandProductCategoryEntity productCategory = e.getProductCategory();
    return BrandSkuResponse.builder()
        .id(e.getId())
        .brandId(e.getBrand().getId())
        .name(e.getName())
        .slug(e.getSlug())
        .skuCode(e.getSkuCode())
        .categoryId(category != null ? category.getId() : null)
        .categoryName(category != null ? category.getName() : null)
        .productCategoryId(productCategory != null ? productCategory.getId() : null)
        .productCategoryName(productCategory != null ? productCategory.getName() : null)
        .shortDescription(e.getShortDescription())
        .description(e.getDescription())
        .imageUrl(e.getImageUrl())
        .priceLabel(e.getPriceLabel())
        .externalUrl(e.getExternalUrl())
        .featured(Boolean.TRUE.equals(e.getFeatured()))
        .latest(Boolean.TRUE.equals(e.getLatest()))
        .displayOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .published(Boolean.TRUE.equals(e.getPublished()))
        .active(Boolean.TRUE.equals(e.getActive()))
        .build();
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
