package com.brandPitara.sfs.instagram.service.impl;

import com.brandPitara.sfs.instagram.client.InstagramMetaClient;
import com.brandPitara.sfs.instagram.client.InstagramMetaException;
import com.brandPitara.sfs.instagram.client.InstagramMetaInsights;
import com.brandPitara.sfs.instagram.client.InstagramMetaMedia;
import com.brandPitara.sfs.instagram.config.AppInstagramProperties;
import com.brandPitara.sfs.instagram.config.InstagramMetaProperties;
import com.brandPitara.sfs.instagram.dto.InstagramCachedAsset;
import com.brandPitara.sfs.instagram.dto.InstagramReelSyncResult;
import com.brandPitara.sfs.instagram.entity.InstagramReelEntity;
import com.brandPitara.sfs.instagram.repository.InstagramReelRepository;
import com.brandPitara.sfs.instagram.service.InstagramAssetCacheService;
import com.brandPitara.sfs.instagram.service.InstagramReelMapper;
import com.brandPitara.sfs.instagram.service.InstagramReelSyncService;
import com.brandPitara.sfs.integration.ExternalProviderTransactions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstagramReelSyncServiceImpl implements InstagramReelSyncService {

    private final InstagramMetaProperties properties;
    private final AppInstagramProperties appInstagramProperties;
    private final InstagramMetaClient instagramMetaClient;
    private final InstagramAssetCacheService instagramAssetCacheService;
    private final InstagramReelRepository instagramReelRepository;
    private final InstagramReelMapper instagramReelMapper;
    private final InstagramTrendingScoreCalculator trendingScoreCalculator;
    // A real, separate bean - not a self-invoked internal method - so that
    // recalculateTrendingScores() below is actually called through Spring's
    // transactional proxy. See InstagramTrendingScoreRecalculator's Javadoc
    // for why the previous `this.recalculateTrendingScores()` self-call made
    // its own @Transactional silently do nothing (a well-known Spring AOP
    // proxy pitfall), and why a self-injected @Lazy proxy field turned out not
    // to be a safe fix either: it produced a real, Spring-boot-refusing
    // circular dependency (dashboardInstagramReelController ->
    // instagramReelSyncServiceImpl -> itself), confirmed by actually booting
    // the app, not just by reasoning about it.
    private final InstagramTrendingScoreRecalculator trendingScoreRecalculator;
    // Makes "this DB write is a short phase around, never containing, the external Meta Graph
    // API calls above/below it" explicit and consistent with Twilio/PublicReview/
    // ProjectConnectivity, instead of relying on Spring Data's own implicit per-call
    // @Transactional on save(). Each save is still only atomic with itself, not across the
    // whole sync loop - the loop's per-item external calls (fetchInsights, cacheThumbnail)
    // make batch-wide atomicity impossible without holding a connection across external I/O,
    // and the sync is idempotent/resumable per Instagram media ID, so per-item atomicity is
    // the correct, deliberate tradeoff here (a failure mid-loop leaves already-processed items
    // correctly saved and picked up again, not corrupted, on the next scheduled run).
    private final ExternalProviderTransactions externalProviderTransactions;

    @Override
    public InstagramReelSyncResult syncLatestReels() {
        return sync(null);
    }

    @Override
    public InstagramReelSyncResult syncOneMedia(String mediaId) {
        if (!StringUtils.hasText(mediaId)) {
            throw new IllegalArgumentException("mediaId is required");
        }
        return sync(mediaId.trim());
    }

    @Override
    public void recalculateTrendingScores() {
        trendingScoreRecalculator.recalculateTrendingScores();
    }

    @Override
    public InstagramReelSyncResult recacheMissingThumbnails() {
        OffsetDateTime startedAt = OffsetDateTime.now();
        int cached = 0;
        int failed = 0;
        int skipped = 0;

        List<InstagramReelEntity> reels = instagramReelRepository.findByActiveTrueAndDeletedFalseAndCachedThumbnailUrlIsNull();
        for (InstagramReelEntity entity : reels) {
            String sourceThumbnailUrl = firstText(entity.getSourceThumbnailUrl(), entity.getThumbnailUrl());
            if (!StringUtils.hasText(entity.getInstagramMediaId()) || !StringUtils.hasText(sourceThumbnailUrl)) {
                skipped++;
                continue;
            }
            if (cacheThumbnail(entity, sourceThumbnailUrl, OffsetDateTime.now())) {
                cached++;
            } else {
                failed++;
            }
            externalProviderTransactions.write(() -> instagramReelRepository.save(entity));
        }

        OffsetDateTime completedAt = OffsetDateTime.now();
        log.info(
            "Instagram thumbnail recache completed: fetched={}, cached={}, failed={}, skipped={}",
            reels.size(),
            cached,
            failed,
            skipped
        );
        return InstagramReelSyncResult.builder()
            .fetchedCount(reels.size())
            .createdCount(0)
            .updatedCount(0)
            .skippedCount(skipped)
            .failedInsightCount(0)
            .thumbnailCachedCount(cached)
            .thumbnailCacheFailedCount(failed)
            .startedAt(startedAt)
            .completedAt(completedAt)
            .build();
    }

    private InstagramReelSyncResult sync(String onlyMediaId) {
        validateConfig();

        OffsetDateTime startedAt = OffsetDateTime.now();
        List<InstagramMetaMedia> mediaItems = instagramMetaClient.fetchMedia();

        int created = 0;
        int updated = 0;
        int skipped = 0;
        int failedInsights = 0;
        int thumbnailCached = 0;
        int thumbnailCacheFailed = 0;

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

            CacheOutcome cacheOutcome = applyMeta(media, insights, entity);
            externalProviderTransactions.write(() -> instagramReelRepository.save(entity));

            if (cacheOutcome == CacheOutcome.CACHED) {
                thumbnailCached++;
            } else if (cacheOutcome == CacheOutcome.FAILED) {
                thumbnailCacheFailed++;
            }

            if (wasNew) {
                created++;
            } else {
                updated++;
            }
        }

        trendingScoreRecalculator.recalculateTrendingScores();
        OffsetDateTime completedAt = OffsetDateTime.now();
        InstagramReelSyncResult result = InstagramReelSyncResult.builder()
            .fetchedCount(mediaItems.size())
            .createdCount(created)
            .updatedCount(updated)
            .skippedCount(skipped)
            .failedInsightCount(failedInsights)
            .thumbnailCachedCount(thumbnailCached)
            .thumbnailCacheFailedCount(thumbnailCacheFailed)
            .startedAt(startedAt)
            .completedAt(completedAt)
            .build();

        log.info(
            "Instagram reel sync completed: fetched={}, created={}, updated={}, skipped={}, failedInsights={}, thumbnailsCached={}, thumbnailCacheFailed={}",
            result.getFetchedCount(),
            result.getCreatedCount(),
            result.getUpdatedCount(),
            result.getSkippedCount(),
            result.getFailedInsightCount(),
            result.getThumbnailCachedCount(),
            result.getThumbnailCacheFailedCount()
        );
        return result;
    }

    private CacheOutcome applyMeta(
        InstagramMetaMedia media,
        InstagramMetaInsights insights,
        InstagramReelEntity entity
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        String previousSourceThumbnailUrl = clean(entity.getSourceThumbnailUrl());
        String sourceThumbnailUrl = clean(media.thumbnailUrl());

        entity.setInstagramMediaId(media.id());
        entity.setCaption(media.caption());
        if (!StringUtils.hasText(entity.getTitle())) {
            entity.setTitle(instagramReelMapper.deriveTitle(media.caption()));
        }
        entity.setInstagramUrl(media.permalink().trim());
        entity.setSourceThumbnailUrl(sourceThumbnailUrl);
        entity.setMetaFetchedAt(now);
        entity.setMediaType(media.mediaType());
        entity.setMediaProductType(media.mediaProductType());
        entity.setPublishedAt(media.timestamp());
        entity.setLikeCount(Math.max(nonNull(insights.getLikeCount()), nonNull(media.likeCount())));
        entity.setCommentCount(Math.max(nonNull(insights.getCommentCount()), nonNull(media.commentsCount())));
        entity.setViewCount(nonNull(insights.getViewCount()));
        entity.setShareCount(nonNull(insights.getShareCount()));
        entity.setSaveCount(nonNull(insights.getSaveCount()));
        entity.setSyncedFromMeta(true);
        entity.setLastSyncedAt(now);
        entity.setTrendingScore(trendingScoreCalculator.calculate(entity));

        CacheOutcome cacheOutcome = CacheOutcome.SKIPPED;
        if (StringUtils.hasText(sourceThumbnailUrl) && shouldCacheThumbnail(entity, previousSourceThumbnailUrl, sourceThumbnailUrl, now)) {
            cacheOutcome = cacheThumbnail(entity, sourceThumbnailUrl, now) ? CacheOutcome.CACHED : CacheOutcome.FAILED;
        }

        if (cacheOutcome != CacheOutcome.FAILED) {
            entity.setLastSyncStatus("SUCCESS");
            entity.setLastSyncError(null);
        }
        return cacheOutcome;
    }

    private boolean shouldCacheThumbnail(
        InstagramReelEntity entity,
        String previousSourceThumbnailUrl,
        String sourceThumbnailUrl,
        OffsetDateTime now
    ) {
        if (!StringUtils.hasText(entity.getCachedThumbnailUrl())) {
            return true;
        }
        if (!StringUtils.hasText(entity.getCachedThumbnailStorageKey())) {
            return true;
        }
        if (!Objects.equals(previousSourceThumbnailUrl, sourceThumbnailUrl)) {
            return true;
        }
        OffsetDateTime cachedAt = entity.getThumbnailCachedAt();
        if (cachedAt == null) {
            return true;
        }
        return cachedAt.isBefore(now.minusHours(appInstagramProperties.effectiveThumbnailCacheTtlHours()));
    }

    private boolean cacheThumbnail(InstagramReelEntity entity, String sourceThumbnailUrl, OffsetDateTime now) {
        try {
            InstagramCachedAsset cached = instagramAssetCacheService.cacheThumbnail(
                entity.getInstagramMediaId(),
                sourceThumbnailUrl
            );
            if (cached == null) {
                throw new IllegalStateException("thumbnail cache returned no asset");
            }
            entity.setCachedThumbnailUrl(cached.publicUrl());
            entity.setCachedThumbnailStorageKey(cached.storageKey());
            entity.setThumbnailCachedAt(now);
            entity.setThumbnailUrl(cached.publicUrl());
            entity.setLastSyncStatus("SUCCESS");
            entity.setLastSyncError(null);
            return true;
        } catch (RuntimeException ex) {
            entity.setLastSyncStatus("THUMBNAIL_CACHE_FAILED");
            entity.setLastSyncError(limitError(ex.getMessage()));
            log.warn(
                "Instagram thumbnail cache failed mediaId={} reason={}",
                entity.getInstagramMediaId(),
                ex.getMessage()
            );
            return false;
        }
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

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstText(String first, String second) {
        String cleanedFirst = clean(first);
        return cleanedFirst != null ? cleanedFirst : clean(second);
    }

    private String limitError(String message) {
        String cleaned = clean(message);
        if (cleaned == null) {
            return null;
        }
        return cleaned.length() <= 500 ? cleaned : cleaned.substring(0, 500);
    }

    private enum CacheOutcome {
        CACHED,
        FAILED,
        SKIPPED
    }
}
