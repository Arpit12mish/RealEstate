package com.brandPitara.sfs.brand.service.impl;

import com.brandPitara.sfs.brand.dto.BrandFaqResponse;
import com.brandPitara.sfs.brand.dto.BrandFaqUpsertRequest;
import com.brandPitara.sfs.brand.entity.BrandEntity;
import com.brandPitara.sfs.brand.entity.BrandFaqEntity;
import com.brandPitara.sfs.brand.repository.BrandFaqRepository;
import com.brandPitara.sfs.brand.repository.BrandRepository;
import com.brandPitara.sfs.brand.service.BrandFaqService;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandFaqServiceImpl implements BrandFaqService {

  private final BrandRepository brandRepository;
  private final BrandFaqRepository brandFaqRepository;
  private final ContentVersionService contentVersionService;

  private static final String KEY_BRANDS = "BRANDS";

  @Override
  @Transactional
  public BrandFaqResponse create(Long brandId, BrandFaqUpsertRequest request) {
    BrandEntity brand = brandRepository.findByIdAndDeletedFalse(brandId)
        .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + brandId));

    BrandFaqEntity entity = BrandFaqEntity.builder()
        .brand(brand)
        .question(cleanRequired(request.getQuestion()))
        .answer(cleanRequired(request.getAnswer()))
        .sortOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
        .active(request.getActive() != null ? request.getActive() : true)
        .deleted(false)
        .build();

    BrandFaqEntity saved = brandFaqRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);

    return toResponse(saved);
  }

  @Override
  @Transactional
  public BrandFaqResponse update(Long brandId, Long faqId, BrandFaqUpsertRequest request) {
    BrandFaqEntity entity = brandFaqRepository.findByIdAndBrand_IdAndDeletedFalse(faqId, brandId)
        .orElseThrow(() -> new EntityNotFoundException("FAQ not found: " + faqId));

    if (StringUtils.hasText(request.getQuestion())) entity.setQuestion(cleanRequired(request.getQuestion()));
    if (StringUtils.hasText(request.getAnswer())) entity.setAnswer(cleanRequired(request.getAnswer()));
    if (request.getDisplayOrder() != null) entity.setSortOrder(request.getDisplayOrder());
    if (request.getActive() != null) entity.setActive(request.getActive());

    BrandFaqEntity saved = brandFaqRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);

    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BrandFaqResponse> adminList(Long brandId) {
    return brandFaqRepository.findByBrand_IdAndDeletedFalseOrderBySortOrderAscIdAsc(brandId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void softDelete(Long brandId, Long faqId) {
    BrandFaqEntity entity = brandFaqRepository.findByIdAndBrand_IdAndDeletedFalse(faqId, brandId)
        .orElseThrow(() -> new EntityNotFoundException("FAQ not found: " + faqId));

    entity.setDeleted(true);
    entity.setActive(false);
    brandFaqRepository.save(entity);

    contentVersionService.bump(KEY_BRANDS);
  }

  private BrandFaqResponse toResponse(BrandFaqEntity e) {
    return BrandFaqResponse.builder()
        .id(e.getId())
        .brandId(e.getBrand().getId())
        .question(e.getQuestion())
        .answer(e.getAnswer())
        .displayOrder(e.getSortOrder() != null ? e.getSortOrder() : 0)
        .active(Boolean.TRUE.equals(e.getActive()))
        .build();
  }

  private String cleanRequired(String s) {
    return s.trim();
  }
}
