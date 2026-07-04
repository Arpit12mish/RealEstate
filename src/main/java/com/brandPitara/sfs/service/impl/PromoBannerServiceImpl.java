package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dto.PromoBannerResponse;
import com.brandPitara.sfs.entity.PromoBannerEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.repository.CategoryRepository;
import com.brandPitara.sfs.repository.PromoBannerRepository;
import com.brandPitara.sfs.service.PromoBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromoBannerServiceImpl implements PromoBannerService {

    private final PromoBannerRepository promoBannerRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public List<PromoBannerResponse> getBannersForCategory(Long categoryId) {

        var category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found: " + categoryId));

        return promoBannerRepository
                .findByCategory_IdAndActiveTrueOrderByPriorityAsc(categoryId)
                .stream()
                .map(b -> toResponse(b, category.getName(), category.getSlug()))
                .toList();
    }

    @Override
    public List<PromoBannerResponse> getBannersForCategoryAndSlot(Long categoryId, String slotKey, Integer maxItems) {

        var category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found: " + categoryId));

        String safeSlot = (slotKey == null || slotKey.isBlank()) ? "HERO" : slotKey.trim();
        int limit = (maxItems == null || maxItems < 1) ? 10 : Math.min(maxItems, 25);

        List<PromoBannerEntity> list = promoBannerRepository
                .findByCategory_IdAndSlotKeyAndActiveTrueOrderByPriorityAsc(categoryId, safeSlot);

        if (list.size() > limit) list = list.subList(0, limit);

        return list.stream()
                .map(b -> toResponse(b, category.getName(), category.getSlug()))
                .toList();
    }

    private PromoBannerResponse toResponse(PromoBannerEntity b,
                                          String categoryName,
                                          String categorySlug) {
        String mediaType = b.getMediaType() != null ? b.getMediaType() : "IMAGE";
        String mediaUrl  = "IMAGE".equals(mediaType) ? b.getImageUrl() : b.getMediaUrl();
        return PromoBannerResponse.builder()
                .id(b.getId())
                .categoryId(b.getCategory().getId())
                .categoryName(categoryName)
                .categorySlug(categorySlug)
                .title(b.getTitle())
                .subtitle(b.getSubtitle())
                .imageUrl(b.getImageUrl())
                .mediaType(mediaType)
                .mediaUrl(mediaUrl)
                .displayDurationMs(b.getDisplayDurationMs())
                .targetUrl(b.getTargetUrl())
                .priority(b.getPriority())
                .active(b.getActive())
                .startAt(b.getStartAt())
                .endAt(b.getEndAt())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
