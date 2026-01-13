package com.brandPitara.sfs.brand.service.impl;

import com.brandPitara.sfs.brand.dto.BrandDistributorResponse;
import com.brandPitara.sfs.brand.dto.BrandDistributorUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandDistributorEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandDistributorRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.BrandDistributorService;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.distributor.entity.DistributorEntity;
import com.brandPitara.sfs.distributor.repository.DistributorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BrandDistributorServiceImpl implements BrandDistributorService {

  private final BrandRepository brandRepository;
  private final DistributorRepository distributorRepository;
  private final BrandDistributorRepository brandDistributorRepository;
  private final ContentVersionService contentVersionService;

  private static final String KEY_HOME = "HOME";
  private static final String KEY_BRANDS = "BRANDS";

  @Override
  @Transactional
  public BrandDistributorResponse upsert(Long brandId, BrandDistributorUpsertRequest request) {

    BrandEntity brand = brandRepository.findByIdAndDeletedFalse(brandId)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + brandId));

    DistributorEntity distributor = distributorRepository.findByIdAndDeletedFalse(request.getDistributorId())
        .orElseThrow(() -> new EntityNotFoundException("Distributor not found: " + request.getDistributorId()));

    BrandDistributorEntity entity = brandDistributorRepository
        .findByBrandIdAndDistributorIdAndDeletedFalse(brandId, distributor.getId())
        .orElse(null);

    if (entity == null) {
      entity = BrandDistributorEntity.builder()
          .brand(brand)
          .distributor(distributor)
          .deleted(false)
          .build();
    }

    if (request.getStatus() != null) entity.setStatus(request.getStatus());
    if (request.getPriority() != null) entity.setPriority(request.getPriority());
    if (request.getOfferTitle() != null) entity.setOfferTitle(clean(request.getOfferTitle()));
    if (request.getOfferDescription() != null) entity.setOfferDescription(clean(request.getOfferDescription()));
    if (request.getOfferBannerUrl() != null) entity.setOfferBannerUrl(clean(request.getOfferBannerUrl()));
    if (request.getValidTill() != null) entity.setValidTill(request.getValidTill());
    if (request.getActive() != null) entity.setActive(request.getActive());

    BrandDistributorEntity saved = brandDistributorRepository.save(entity);

    // This affects customer surfaces (brand pages, distributor page, home offers etc.)
    contentVersionService.bump(KEY_BRANDS);
    contentVersionService.bump(KEY_HOME);

    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BrandDistributorResponse> adminList(Long brandId, Pageable pageable) {
    return brandDistributorRepository.findByBrandIdAndDeletedFalse(brandId, pageable)
        .map(this::toResponse);
  }

  @Override
  @Transactional
  public void softDelete(Long brandId, Long distributorId) {
    BrandDistributorEntity entity = brandDistributorRepository
        .findByBrandIdAndDistributorIdAndDeletedFalse(brandId, distributorId)
        .orElseThrow(() -> new EntityNotFoundException("Mapping not found"));

    entity.setDeleted(true);
    entity.setActive(false);
    brandDistributorRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);
    contentVersionService.bump(KEY_HOME);
  }

  private BrandDistributorResponse toResponse(BrandDistributorEntity e) {
    var d = e.getDistributor();
    return BrandDistributorResponse.builder()
        .id(e.getId())
        .brandId(e.getBrand().getId())
        .distributorId(d.getId())
        .status(e.getStatus())
        .priority(e.getPriority() != null ? e.getPriority() : 0)
        .offerTitle(e.getOfferTitle())
        .offerDescription(e.getOfferDescription())
        .offerBannerUrl(e.getOfferBannerUrl())
        .validTill(e.getValidTill())
        .active(Boolean.TRUE.equals(e.getActive()))
        // handy fields for admin list UI
        .distributorName(d.getName())
        .distributorPhone(d.getPhone())
        .cityId(d.getCityId())
        .build();
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }
}
