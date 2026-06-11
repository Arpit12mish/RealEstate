package com.brandPitara.sfs.brand.service.impl;

import com.brandPitara.sfs.brand.dto.*;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.entity.BrandMediaEntity;
import com.brandPitara.sfs.brand.entity.BrandMediaEntity.Placement;
import com.brandPitara.sfs.brand.repository.BrandDistributorRepository;
import com.brandPitara.sfs.brand.repository.BrandMediaRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.BrandService;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.distributor.dto.DistributorCardResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

  private final BrandRepository brandRepository;
  private final BrandMediaRepository brandMediaRepository;
  private final BrandDistributorRepository brandDistributorRepository;
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
        .published(false)
        .deleted(false)
        .promoEnabled(request.getPromoEnabled() != null ? request.getPromoEnabled() : false)
        .promoMediaType(request.getPromoMediaType())
        .promoMediaUrl(clean(request.getPromoMediaUrl()))
        .build();

    BrandEntity saved = brandRepository.save(entity);

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
    if (request.getPriority() != null) entity.setPriority(request.getPriority());
    if (request.getActive() != null) entity.setActive(request.getActive());

    if (request.getPromoEnabled() != null) entity.setPromoEnabled(request.getPromoEnabled());
    if (request.getPromoMediaType() != null) entity.setPromoMediaType(request.getPromoMediaType());
    if (request.getPromoMediaUrl() != null) entity.setPromoMediaUrl(clean(request.getPromoMediaUrl()));

    BrandEntity saved = brandRepository.save(entity);

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

    return page.map(this::toResponse);
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
    return BrandResponse.builder()
        .id(e.getId())
        .name(e.getName())
        .logoUrl(e.getLogoUrl())
        .description(e.getDescription())
        .active(Boolean.TRUE.equals(e.getActive()))
        .published(Boolean.TRUE.equals(e.getPublished()))
        .priority(e.getPriority() != null ? e.getPriority() : 0)
        .promoEnabled(Boolean.TRUE.equals(e.getPromoEnabled()))
        .promoMediaType(e.getPromoMediaType())
        .promoMediaUrl(e.getPromoMediaUrl())
        .build();
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
