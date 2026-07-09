package com.brandPitara.sfs.instagram.service.impl;

import com.brandPitara.sfs.instagram.config.InstagramMetaProperties;
import com.brandPitara.sfs.instagram.dto.DashboardInstagramReelResponse;
import com.brandPitara.sfs.instagram.dto.DashboardInstagramReelUpsertRequest;
import com.brandPitara.sfs.instagram.dto.PublicInstagramReelItemResponse;
import com.brandPitara.sfs.instagram.entity.InstagramReelEntity;
import com.brandPitara.sfs.instagram.enums.InstagramReelCategory;
import com.brandPitara.sfs.instagram.repository.InstagramReelRepository;
import com.brandPitara.sfs.instagram.service.InstagramReelMapper;
import com.brandPitara.sfs.instagram.service.InstagramReelService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstagramReelServiceImpl implements InstagramReelService {

    private final InstagramReelRepository instagramReelRepository;
    private final InstagramReelMapper instagramReelMapper;
    private final InstagramMetaProperties properties;

    @Override
    @Transactional(readOnly = true)
    public Page<DashboardInstagramReelResponse> dashboardList(
        Boolean active,
        InstagramReelCategory category,
        String query,
        Pageable pageable
    ) {
        String cleanedQuery = clean(query);
        Page<InstagramReelEntity> reels = cleanedQuery == null
            ? instagramReelRepository.dashboardList(
                active,
                category,
                pageable
            )
            : instagramReelRepository.dashboardSearch(
                active,
                category,
                "%" + cleanedQuery.toLowerCase() + "%",
                pageable
            );

        return reels.map(instagramReelMapper::toDashboardResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardInstagramReelResponse get(Long id) {
        return instagramReelMapper.toDashboardResponse(findActiveRow(id));
    }

    @Override
    @Transactional
    public DashboardInstagramReelResponse createManual(DashboardInstagramReelUpsertRequest request) {
        String instagramUrl = cleanRequired(request.getInstagramUrl(), "instagramUrl is required");
        if (instagramReelRepository.existsByInstagramUrlAndDeletedFalse(instagramUrl)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instagram reel URL already exists");
        }
        if (!StringUtils.hasText(request.getThumbnailUrl())) {
            throw new IllegalArgumentException("thumbnailUrl is required for manual Instagram reels");
        }
        String thumbnailUrl = clean(request.getThumbnailUrl());

        InstagramReelEntity entity = InstagramReelEntity.builder()
            .title(clean(request.getTitle()))
            .caption(clean(request.getCaption()))
            .instagramUrl(instagramUrl)
            .thumbnailUrl(thumbnailUrl)
            .cachedThumbnailUrl(thumbnailUrl)
            .previewVideoUrl(clean(request.getPreviewVideoUrl()))
            .categoryOverride(request.getCategoryOverride() != null ? request.getCategoryOverride() : InstagramReelCategory.MANUAL)
            .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
            .active(request.getActive() != null ? request.getActive() : true)
            .syncedFromMeta(false)
            .deleted(false)
            .build();

        return instagramReelMapper.toDashboardResponse(instagramReelRepository.save(entity));
    }

    @Override
    @Transactional
    public DashboardInstagramReelResponse update(Long id, DashboardInstagramReelUpsertRequest request) {
        InstagramReelEntity entity = findActiveRow(id);
        String instagramUrl = cleanRequired(request.getInstagramUrl(), "instagramUrl is required");
        if (instagramReelRepository.existsByInstagramUrlAndDeletedFalseAndIdNot(instagramUrl, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instagram reel URL already exists");
        }

        entity.setTitle(clean(request.getTitle()));
        entity.setCaption(clean(request.getCaption()));
        entity.setInstagramUrl(instagramUrl);
        String thumbnailUrl = clean(request.getThumbnailUrl());
        entity.setThumbnailUrl(thumbnailUrl);
        if (StringUtils.hasText(thumbnailUrl)) {
            entity.setCachedThumbnailUrl(thumbnailUrl);
        }
        entity.setPreviewVideoUrl(clean(request.getPreviewVideoUrl()));
        entity.setCategoryOverride(request.getCategoryOverride());
        entity.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }

        return instagramReelMapper.toDashboardResponse(instagramReelRepository.save(entity));
    }

    @Override
    @Transactional
    public DashboardInstagramReelResponse setActive(Long id, boolean active) {
        InstagramReelEntity entity = findActiveRow(id);
        entity.setActive(active);
        return instagramReelMapper.toDashboardResponse(instagramReelRepository.save(entity));
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        InstagramReelEntity entity = findActiveRow(id);
        entity.setDeleted(true);
        entity.setActive(false);
        instagramReelRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicInstagramReelItemResponse> publicList(InstagramReelCategory category, int page, int limit) {
        InstagramReelCategory safeCategory = category != null ? category : InstagramReelCategory.LATEST;
        int safeLimit = capLimit(limit);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeLimit);

        Page<InstagramReelEntity> reelPage = switch (safeCategory) {
            case LATEST -> instagramReelRepository.findByActiveTrueAndDeletedFalseOrderByPublishedAtDescIdDesc(pageable);
            case TRENDING -> instagramReelRepository.findByActiveTrueAndDeletedFalseOrderByTrendingScoreDescPublishedAtDescIdDesc(pageable);
            case MOST_VIEWED -> instagramReelRepository.findByActiveTrueAndDeletedFalseOrderByViewCountDescPublishedAtDescIdDesc(pageable);
            case INTERVIEWS -> instagramReelRepository
                .findByActiveTrueAndDeletedFalseAndCategoryOverrideOrderByDisplayOrderAscPublishedAtDescIdDesc(
                    InstagramReelCategory.INTERVIEWS,
                    pageable
                );
            case MANUAL -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MANUAL is not a public Instagram reel category");
        };

        return reelPage.getContent().stream()
            .map(entity -> instagramReelMapper.toPublicResponse(entity, safeCategory))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicInstagramReelItemResponse> publicHomeItems(Integer requestedLimit) {
        int limit = requestedLimit != null
            ? Math.min(capLimit(requestedLimit), properties.effectivePublicLimit())
            : properties.effectivePublicLimit();
        return publicList(InstagramReelCategory.LATEST, 0, limit);
    }

    private InstagramReelEntity findActiveRow(Long id) {
        return instagramReelRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new EntityNotFoundException("Instagram reel not found: " + id));
    }

    private int capLimit(int limit) {
        return Math.min(Math.max(limit, 1), 20);
    }

    private String cleanRequired(String value, String message) {
        String cleaned = clean(value);
        if (!StringUtils.hasText(cleaned)) {
            throw new IllegalArgumentException(message);
        }
        return cleaned;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
