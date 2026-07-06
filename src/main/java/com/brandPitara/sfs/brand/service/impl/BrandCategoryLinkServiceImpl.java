package com.brandPitara.sfs.brand.service.impl;

import com.brandPitara.sfs.brand.dto.BrandCategoryLinkResponse;
import com.brandPitara.sfs.brand.dto.BrandCategoryLinkUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandCategoryLinkEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandCategoryLinkRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.BrandCategoryLinkService;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandCategoryLinkServiceImpl implements BrandCategoryLinkService {

  private final BrandRepository brandRepository;
  private final CategoryRepository categoryRepository;
  private final BrandCategoryLinkRepository brandCategoryLinkRepository;
  private final ContentVersionService contentVersionService;

  private static final String KEY_BRANDS = "BRANDS";

  @Override
  @Transactional
  public BrandCategoryLinkResponse upsert(Long brandId, BrandCategoryLinkUpsertRequest request) {
    BrandEntity brand = brandRepository.findByIdAndDeletedFalse(brandId)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + brandId));

    CategoryEntity category = categoryRepository.findByIdAndActiveTrue(request.getCategoryId())
        .orElseThrow(() -> new EntityNotFoundException("Category not found or inactive: " + request.getCategoryId()));

    BrandCategoryLinkEntity entity = brandCategoryLinkRepository
        .findByBrand_IdAndCategory_Id(brandId, category.getId())
        .orElse(null);

    if (entity == null) {
      entity = BrandCategoryLinkEntity.builder()
          .brand(brand)
          .category(category)
          .active(true)
          .deleted(false)
          .build();
    } else {
      // reactivate if this link was previously soft-deleted
      entity.setDeleted(false);
    }

    if (request.getDisplayOrder() != null) entity.setSortOrder(request.getDisplayOrder());
    if (request.getActive() != null) entity.setActive(request.getActive());

    BrandCategoryLinkEntity saved = brandCategoryLinkRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);

    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BrandCategoryLinkResponse> adminList(Long brandId) {
    return brandCategoryLinkRepository.findByBrand_IdAndDeletedFalseOrderBySortOrderAscIdAsc(brandId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void softDelete(Long brandId, Long categoryId) {
    BrandCategoryLinkEntity entity = brandCategoryLinkRepository
        .findByBrand_IdAndCategory_IdAndDeletedFalse(brandId, categoryId)
        .orElseThrow(() -> new EntityNotFoundException("Category link not found"));

    entity.setDeleted(true);
    entity.setActive(false);
    brandCategoryLinkRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);
  }

  private BrandCategoryLinkResponse toResponse(BrandCategoryLinkEntity e) {
    CategoryEntity category = e.getCategory();
    return BrandCategoryLinkResponse.builder()
        .id(e.getId())
        .brandId(e.getBrand().getId())
        .categoryId(category.getId())
        .categoryName(category.getName())
        .categorySlug(category.getSlug())
        .displayOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .active(Boolean.TRUE.equals(e.getActive()))
        .build();
  }
}
