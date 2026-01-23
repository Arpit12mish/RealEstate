package com.brandPitara.sfs.brand.service.impl;

import com.brandPitara.sfs.brand.dto.BrandMediaResponse;
import com.brandPitara.sfs.brand.dto.BrandMediaUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.entity.BrandMediaEntity;
import com.brandPitara.sfs.brand.entity.BrandMediaEntity.Placement;
import com.brandPitara.sfs.brand.repository.BrandMediaRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.BrandMediaService;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandMediaServiceImpl implements BrandMediaService {

  private final BrandRepository brandRepository;
  private final BrandMediaRepository brandMediaRepository;
  private final ContentVersionService contentVersionService;

  private static final String KEY_BRANDS = "BRANDS";
  private static final String KEY_HOME = "HOME";

  @Override
  @Transactional
  public BrandMediaResponse addMedia(Long brandId, BrandMediaUpsertRequest request) {
    BrandEntity brand = brandRepository.findByIdAndDeletedFalse(brandId)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + brandId));

    BrandMediaEntity entity = BrandMediaEntity.builder()
        .brand(brand)
        .mediaType(request.getMediaType())
        .placement(request.getPlacement())
        .url(clean(request.getUrl()))
        .caption(clean(request.getCaption()))
        .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
        .actionType(clean(request.getActionType()))
        .actionValue(clean(request.getActionValue()))
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    BrandMediaEntity saved = brandMediaRepository.save(entity);

    // brand page changes -> bump
    contentVersionService.bump(KEY_BRANDS);
    contentVersionService.bump(KEY_HOME);

    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BrandMediaResponse> listMedia(Long brandId, Placement placement) {
    brandRepository.findByIdAndDeletedFalse(brandId)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + brandId));

    return brandMediaRepository
        .findByBrandIdAndPlacementAndDeletedFalseAndActiveTrueOrderBySortOrderAsc(brandId, placement)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void softDeleteMedia(Long brandId, Long mediaId) {
    brandRepository.findByIdAndDeletedFalse(brandId)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + brandId));

    BrandMediaEntity media = brandMediaRepository.findById(mediaId)
        .orElseThrow(() -> new EntityNotFoundException("Media not found: " + mediaId));

    if (media.getBrand() == null || !media.getBrand().getId().equals(brandId)) {
      throw new IllegalArgumentException("Media does not belong to brand");
    }

    media.setDeleted(true);
    media.setActive(false);
    brandMediaRepository.save(media);

    contentVersionService.bump(KEY_BRANDS);
    contentVersionService.bump(KEY_HOME);
  }

  private BrandMediaResponse toResponse(BrandMediaEntity m) {
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

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
