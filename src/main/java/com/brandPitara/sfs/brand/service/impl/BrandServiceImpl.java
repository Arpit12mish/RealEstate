package com.brandPitara.sfs.brand.service.impl;

import com.brandPitara.sfs.brand.dto.BrandResponse;
import com.brandPitara.sfs.brand.dto.BrandUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.BrandService;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

  private final BrandRepository brandRepository;
  private final ContentVersionService contentVersionService;

  // Keys for version bumps
  private static final String KEY_BRANDS = "BRANDS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional
  public BrandResponse create(BrandUpsertRequest request) {
    BrandEntity entity = BrandEntity.builder()
        .name(clean(request.getName()))
        .logoUrl(clean(request.getLogoUrl()))
        .description(clean(request.getDescription()))
        .priority(request.getPriority() != null ? request.getPriority() : 0)
        .active(request.getActive() != null ? request.getActive() : true)
        .published(false) // default - admin will publish
        .deleted(false)
        .build();

    BrandEntity saved = brandRepository.save(entity);

    // content changed (admin added), but not visible to customers until published.
    // Still bump BRANDS for admin screens; HOME only after publish/unpublish.
    contentVersionService.bump(KEY_BRANDS);

    return toResponse(saved);
  }

  @Override
  @Transactional
  public BrandResponse update(Long id, BrandUpsertRequest request) {
    BrandEntity entity = brandRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + id));

    if (StringUtils.hasText(request.getName())) {
      entity.setName(clean(request.getName()));
    }
    if (request.getLogoUrl() != null) {
      entity.setLogoUrl(clean(request.getLogoUrl()));
    }
    if (request.getDescription() != null) {
      entity.setDescription(clean(request.getDescription()));
    }
    if (request.getPriority() != null) {
      entity.setPriority(request.getPriority());
    }
    if (request.getActive() != null) {
      entity.setActive(request.getActive());
    }

    BrandEntity saved = brandRepository.save(entity);

    // If brand is already published, customers should see update quickly
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

    // Publishing impacts customer surfaces
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
    entity.setPublished(false); // safety
    brandRepository.save(entity);

    // Removing impacts customer surfaces
    contentVersionService.bump(KEY_BRANDS);
    contentVersionService.bump(KEY_HOME);
  }

  @Override
  @Transactional(readOnly = true)
  public BrandResponse getById(Long id) {
    BrandEntity entity = brandRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + id));
    return toResponse(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BrandResponse> adminList(Boolean published, Boolean active, Pageable pageable) {
    // Flexible filters, not required for v1. Keeping it clean.
    Page<BrandEntity> page;

    if (published == null && active == null) {
      page = brandRepository.findByDeletedFalse(pageable);
    } else if (published != null && active == null) {
      page = brandRepository.findByPublishedAndDeletedFalse(published, pageable);
    } else if (published == null) { // active != null
      page = brandRepository.findByActiveAndDeletedFalse(active, pageable);
    } else {
      page = brandRepository.findByPublishedAndActiveAndDeletedFalse(published, active, pageable);
    }

    return page.map(this::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BrandResponse> listPublished(Pageable pageable) {
    return brandRepository
        .findByPublishedTrueAndActiveTrueAndDeletedFalse(pageable)
        .map(this::toResponse);
  }

  // ---------- helpers ----------

  private BrandResponse toResponse(BrandEntity e) {
    return BrandResponse.builder()
        .id(e.getId())
        .name(e.getName())
        .logoUrl(e.getLogoUrl())
        .description(e.getDescription())
        .active(Boolean.TRUE.equals(e.getActive()))
        .published(Boolean.TRUE.equals(e.getPublished()))
        .priority(e.getPriority() != null ? e.getPriority() : 0)
        .build();
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
