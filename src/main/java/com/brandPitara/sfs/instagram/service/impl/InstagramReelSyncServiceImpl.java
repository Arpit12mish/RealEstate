package com.brandPitara.sfs.instagram.service.impl;

import com.brandPitara.sfs.instagram.client.InstagramMetaClient;
import com.brandPitara.sfs.instagram.client.InstagramMetaException;
import com.brandPitara.sfs.instagram.client.InstagramMetaInsights;
import com.brandPitara.sfs.instagram.client.InstagramMetaMedia;
import com.brandPitara.sfs.instagram.config.InstagramMetaProperties;
import com.brandPitara.sfs.instagram.dto.InstagramReelSyncResult;
import com.brandPitara.sfs.instagram.entity.InstagramReelEntity;
import com.brandPitara.sfs.instagram.repository.InstagramReelRepository;
import com.brandPitara.sfs.instagram.service.InstagramReelMapper;
import com.brandPitara.sfs.instagram.service.InstagramReelSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstagramReelSyncServiceImpl implements InstagramReelSyncService {

    private final InstagramMetaProperties properties;
    private final InstagramMetaClient instagramMetaClient;
    private final InstagramReelRepository instagramReelRepository;
    private final InstagramReelMapper instagramReelMapper;

    @Override
    @Transactional
    public InstagramReelSyncResult syncLatestReels() {
        return sync(null);
    }

    @Override
    @Transactional
    public InstagramReelSyncResult syncOneMedia(String mediaId) {
        if (!StringUtils.hasText(mediaId)) {
            throw new IllegalArgumentException("mediaId is required");
        }
        return sync(mediaId.trim());
    }

    @Override
    @Transactional
    public void recalculateTrendingScores() {
        instagramReelRepository.findByDeletedFalse()
            .forEach(entity -> entity.setTrendingScore(calculateTrendingScore(entity)));
    }

    private InstagramReelSyncResult sync(String onlyMediaId) {
        validateConfig();

        OffsetDateTime startedAt = OffsetDateTime.now();
        List<InstagramMetaMedia> mediaItems = instagramMetaClient.fetchMedia();

        int created = 0;
        int updated = 0;
        int skipped = 0;
        int failedInsights = 0;

        for (InstagramMetaMedia media : mediaItems) {
            if (media == null) {
                skipped++;
                continue;
            }
            if (StringUtils.hasText(onlyMediaId) && !onlyMediaId.equals(media.id())) {
                skipped++;
                continue;
            }
            if (!isReel(media)) {
                skipped++;
                continue;
            }
            if (!StringUtils.hasText(media.permalink())) {
                skipped++;
                continue;
            }

            InstagramMetaInsights insights;
            try {
                insights = instagramMetaClient.fetchInsights(media.id());
            } catch (InstagramMetaException ex) {
                failedInsights++;
                log.warn("Instagram insights unavailable for mediaId={}", media.id());
                insights = InstagramMetaInsights.builder().build();
            }

            boolean wasNew = false;
            InstagramReelEntity entity = instagramReelRepository
                .findByInstagramMediaIdAndDeletedFalse(media.id())
                .orElseGet(() -> {
                    InstagramReelEntity fresh = InstagramReelEntity.builder()
                        .instagramMediaId(media.id())
                        .active(true)
                        .syncedFromMeta(true)
                        .deleted(false)
                        .build();
                    fresh.setTitle(instagramReelMapper.deriveTitle(media.caption()));
                    return fresh;
                });

            if (entity.getId() == null) {
                wasNew = true;
            }

            applyMeta(media, insights, entity);
            instagramReelRepository.save(entity);

            if (wasNew) {
                created++;
            } else {
                updated++;
            }
        }

        recalculateTrendingScores();
        OffsetDateTime completedAt = OffsetDateTime.now();
        InstagramReelSyncResult result = InstagramReelSyncResult.builder()
            .fetchedCount(mediaItems.size())
            .createdCount(created)
            .updatedCount(updated)
            .skippedCount(skipped)
            .failedInsightCount(failedInsights)
            .startedAt(startedAt)
            .completedAt(completedAt)
            .build();

        log.info(
            "Instagram reel sync completed: fetched={}, created={}, updated={}, skipped={}, failedInsights={}",
            result.getFetchedCount(),
            result.getCreatedCount(),
            result.getUpdatedCount(),
            result.getSkippedCount(),
            result.getFailedInsightCount()
        );
        return result;
    }

    private void applyMeta(
        InstagramMetaMedia media,
        InstagramMetaInsights insights,
        InstagramReelEntity entity
    ) {
        entity.setInstagramMediaId(media.id());
        entity.setCaption(media.caption());
        if (!StringUtils.hasText(entity.getTitle())) {
            entity.setTitle(instagramReelMapper.deriveTitle(media.caption()));
        }
        entity.setInstagramUrl(media.permalink().trim());
        if (StringUtils.hasText(media.thumbnailUrl())) {
            entity.setThumbnailUrl(media.thumbnailUrl().trim());
        }
        entity.setMediaType(media.mediaType());
        entity.setMediaProductType(media.mediaProductType());
        entity.setPublishedAt(media.timestamp());
        entity.setLikeCount(Math.max(nonNull(insights.getLikeCount()), nonNull(media.likeCount())));
        entity.setCommentCount(Math.max(nonNull(insights.getCommentCount()), nonNull(media.commentsCount())));
        entity.setViewCount(nonNull(insights.getViewCount()));
        entity.setShareCount(nonNull(insights.getShareCount()));
        entity.setSaveCount(nonNull(insights.getSaveCount()));
        entity.setSyncedFromMeta(true);
        entity.setLastSyncedAt(OffsetDateTime.now());
        entity.setTrendingScore(calculateTrendingScore(entity));
    }

    private boolean isReel(InstagramMetaMedia media) {
        String productType = lower(media.mediaProductType());
        String mediaType = lower(media.mediaType());
        String permalink = lower(media.permalink());

        if ("reels".equals(productType) || "reel".equals(productType)) {
            return true;
        }
        return "video".equals(mediaType) && permalink != null && permalink.contains("/reel/");
    }

    private BigDecimal calculateTrendingScore(InstagramReelEntity entity) {
        double score =
            nonNull(entity.getViewCount()) * 0.50
                + nonNull(entity.getLikeCount()) * 0.20
                + nonNull(entity.getCommentCount()) * 0.20
                + nonNull(entity.getShareCount()) * 0.10
                + nonNull(entity.getSaveCount()) * 0.10;

        OffsetDateTime publishedAt = entity.getPublishedAt();
        if (publishedAt != null) {
            long ageHours = ChronoUnit.HOURS.between(publishedAt, OffsetDateTime.now());
            if (ageHours <= 24) {
                score += 500;
            } else if (ageHours <= 24 * 7) {
                score += 250;
            } else if (ageHours <= 24 * 30) {
                score += 100;
            }
        }

        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateConfig() {
        if (!properties.hasRequiredSyncConfig()) {
            throw new IllegalStateException(
                "Instagram Meta sync is not configured. Set META_ACCESS_TOKEN, META_INSTAGRAM_BUSINESS_ACCOUNT_ID, and META_GRAPH_API_VERSION."
            );
        }
    }

    private Long nonNull(Long value) {
        return value != null ? value : 0L;
    }

    private String lower(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }
}
