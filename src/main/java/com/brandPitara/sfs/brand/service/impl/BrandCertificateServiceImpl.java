package com.brandPitara.sfs.brand.service.impl;

import com.brandPitara.sfs.brand.dto.BrandCertificateResponse;
import com.brandPitara.sfs.brand.dto.BrandCertificateUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandCertificateEntity;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.repository.BrandCertificateRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.BrandCertificateService;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandCertificateServiceImpl implements BrandCertificateService {

  private final BrandRepository brandRepository;
  private final BrandCertificateRepository brandCertificateRepository;
  private final ContentVersionService contentVersionService;

  private static final String KEY_BRANDS = "BRANDS";

  @Override
  @Transactional
  public BrandCertificateResponse create(Long brandId, BrandCertificateUpsertRequest request) {
    BrandEntity brand = brandRepository.findByIdAndDeletedFalse(brandId)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + brandId));

    BrandCertificateEntity entity = BrandCertificateEntity.builder()
        .brand(brand)
        .title(cleanRequired(request.getTitle()))
        .issuer(clean(request.getIssuer()))
        .certificateUrl(clean(request.getCertificateUrl()))
        .sortOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    BrandCertificateEntity saved = brandCertificateRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);

    return toResponse(saved);
  }

  @Override
  @Transactional
  public BrandCertificateResponse update(Long brandId, Long certificateId, BrandCertificateUpsertRequest request) {
    BrandCertificateEntity entity = brandCertificateRepository.findByIdAndBrand_IdAndDeletedFalse(certificateId, brandId)
        .orElseThrow(() -> new EntityNotFoundException("Certificate not found: " + certificateId));

    if (StringUtils.hasText(request.getTitle())) entity.setTitle(cleanRequired(request.getTitle()));
    if (request.getIssuer() != null) entity.setIssuer(clean(request.getIssuer()));
    if (request.getCertificateUrl() != null) entity.setCertificateUrl(clean(request.getCertificateUrl()));
    if (request.getDisplayOrder() != null) entity.setSortOrder(request.getDisplayOrder());
    if (request.getActive() != null) entity.setActive(request.getActive());

    BrandCertificateEntity saved = brandCertificateRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);

    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BrandCertificateResponse> adminList(Long brandId) {
    return brandCertificateRepository.findByBrand_IdAndDeletedFalseOrderBySortOrderAscIdAsc(brandId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void softDelete(Long brandId, Long certificateId) {
    BrandCertificateEntity entity = brandCertificateRepository.findByIdAndBrand_IdAndDeletedFalse(certificateId, brandId)
        .orElseThrow(() -> new EntityNotFoundException("Certificate not found: " + certificateId));

    entity.setDeleted(true);
    entity.setActive(false);
    brandCertificateRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);
  }

  private BrandCertificateResponse toResponse(BrandCertificateEntity e) {
    return BrandCertificateResponse.builder()
        .id(e.getId())
        .brandId(e.getBrand().getId())
        .title(e.getTitle())
        .issuer(e.getIssuer())
        .certificateUrl(e.getCertificateUrl())
        .displayOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .active(Boolean.TRUE.equals(e.getActive()))
        .build();
  }

  private String clean(String s) {
    if (!StringUtils.hasText(s)) return null;
    return s.trim();
  }

  private String cleanRequired(String s) {
    return s.trim();
  }
}
