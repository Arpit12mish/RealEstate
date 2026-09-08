package com.brandPitara.sfs.dashboard.promobanner.service.impl;

import com.brandPitara.sfs.dashboard.promobanner.dto.DashboardPromoBannerResponse;
import com.brandPitara.sfs.dashboard.promobanner.dto.DashboardPromoBannerUpsertRequest;
import com.brandPitara.sfs.dashboard.promobanner.service.DashboardPromoBannerService;
import com.brandPitara.sfs.entity.CategoryEntity;
import com.brandPitara.sfs.entity.PromoBannerEntity;
import com.brandPitara.sfs.enums.PromoBannerMediaType;
import com.brandPitara.sfs.repository.CategoryRepository;
import com.brandPitara.sfs.repository.PromoBannerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DashboardPromoBannerServiceImpl implements DashboardPromoBannerService {

    private final PromoBannerRepository promoBannerRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<DashboardPromoBannerResponse> list(
            Long categoryId,
            String slotKey,
            Boolean active,
            PromoBannerMediaType mediaType,
            Pageable pageable
    ) {
        Specification<PromoBannerEntity> spec = (root, query, cb) -> cb.isFalse(root.get("deleted"));
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (StringUtils.hasText(slotKey)) {
            String normalizedSlot = normalizeSlotKey(slotKey);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("slotKey"), normalizedSlot));
        }
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        if (mediaType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("mediaType"), mediaType));
        }
        return promoBannerRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardPromoBannerResponse get(Long bannerId) {
        return toResponse(findActiveRecord(bannerId));
    }

    @Override
    @Transactional
    public DashboardPromoBannerResponse create(DashboardPromoBannerUpsertRequest request) {
        PromoBannerEntity entity = PromoBannerEntity.builder().build();
        apply(entity, request);
        return toResponse(promoBannerRepository.save(entity));
    }

    @Override
    @Transactional
    public DashboardPromoBannerResponse update(Long bannerId, DashboardPromoBannerUpsertRequest request) {
        PromoBannerEntity entity = findActiveRecord(bannerId);
        apply(entity, request);
        return toResponse(promoBannerRepository.save(entity));
    }

    @Override
    @Transactional
    public DashboardPromoBannerResponse setActive(Long bannerId, boolean active) {
        PromoBannerEntity entity = findActiveRecord(bannerId);
        entity.setActive(active);
        return toResponse(promoBannerRepository.save(entity));
    }

    @Override
    @Transactional
    public void softDelete(Long bannerId) {
        PromoBannerEntity entity = findActiveRecord(bannerId);
        entity.setActive(false);
        entity.setDeleted(true);
        promoBannerRepository.save(entity);
    }

    private PromoBannerEntity findActiveRecord(Long bannerId) {
        return promoBannerRepository.findByIdAndDeletedFalse(bannerId)
                .orElseThrow(() -> new EntityNotFoundException("Promo banner not found: " + bannerId));
    }

    private void apply(PromoBannerEntity entity, DashboardPromoBannerUpsertRequest request) {
        validate(request);
        CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + request.getCategoryId()));

        String mediaUrl = request.getMediaUrl().trim();
        entity.setCategory(category);
        entity.setSlotKey(normalizeSlotKey(request.getSlotKey()));
        entity.setTitle(request.getTitle().trim());
        entity.setSubtitle(clean(request.getSubtitle()));
        entity.setMediaType(request.getMediaType());
        entity.setMediaUrl(mediaUrl);
        entity.setImageUrl(request.getMediaType() == PromoBannerMediaType.IMAGE ? mediaUrl : null);
        entity.setTargetUrl(clean(request.getTargetUrl()));
        entity.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        entity.setActive(request.getActive() != null ? request.getActive() : true);
        entity.setDisplayDurationMs(request.getDisplayDurationMs());
        entity.setStartAt(request.getStartAt());
        entity.setEndAt(request.getEndAt());
        if (entity.getDeleted() == null) {
            entity.setDeleted(false);
        }
    }

    private void validate(DashboardPromoBannerUpsertRequest request) {
        if (request.getStartAt() != null && request.getEndAt() != null
                && !request.getEndAt().isAfter(request.getStartAt())) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }

        URI uri;
        try {
            uri = new URI(request.getMediaUrl().trim());
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("mediaUrl must be a valid http(s) URL");
        }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || !StringUtils.hasText(uri.getHost())) {
            throw new IllegalArgumentException("mediaUrl must be a valid http(s) URL");
        }
        String query = uri.getRawQuery();
        if (query != null) {
            String lowerQuery = query.toLowerCase(Locale.ROOT);
            if (lowerQuery.contains("x-amz-signature=")
                    || lowerQuery.contains("x-amz-credential=")
                    || lowerQuery.contains("x-amz-expires=")) {
                throw new IllegalArgumentException("mediaUrl must be the permanent public URL, not a presigned upload URL");
            }
        }

        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        if (request.getMediaType() == PromoBannerMediaType.LOTTIE_JSON && !path.endsWith(".json")) {
            throw new IllegalArgumentException("LOTTIE_JSON mediaUrl must point to a .json file");
        }
        if (request.getMediaType() == PromoBannerMediaType.VIDEO && !path.endsWith(".mp4")) {
            throw new IllegalArgumentException("VIDEO mediaUrl must point to an .mp4 file");
        }
    }

    private String normalizeSlotKey(String slotKey) {
        return slotKey.trim().toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private DashboardPromoBannerResponse toResponse(PromoBannerEntity entity) {
        return DashboardPromoBannerResponse.builder()
                .id(entity.getId())
                .categoryId(entity.getCategory().getId())
                .categoryName(entity.getCategory().getName())
                .categorySlug(entity.getCategory().getSlug())
                .slotKey(entity.getSlotKey())
                .title(entity.getTitle())
                .subtitle(entity.getSubtitle())
                .mediaType(entity.getMediaType())
                .mediaUrl(entity.getMediaType() == PromoBannerMediaType.IMAGE
                        ? entity.getImageUrl() : entity.getMediaUrl())
                .imageUrl(entity.getImageUrl())
                .targetUrl(entity.getTargetUrl())
                .priority(entity.getPriority())
                .active(entity.getActive())
                .displayDurationMs(entity.getDisplayDurationMs())
                .startAt(entity.getStartAt())
                .endAt(entity.getEndAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
