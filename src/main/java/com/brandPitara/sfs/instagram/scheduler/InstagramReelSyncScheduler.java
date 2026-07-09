package com.brandPitara.sfs.instagram.scheduler;

import com.brandPitara.sfs.instagram.config.InstagramMetaProperties;
import com.brandPitara.sfs.instagram.dto.InstagramReelSyncResult;
import com.brandPitara.sfs.instagram.service.InstagramReelSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class InstagramReelSyncScheduler {

    private final InstagramMetaProperties properties;
    private final InstagramReelSyncService instagramReelSyncService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "${sfs.instagram.meta.sync-cron:0 0 * * * *}")
    public void sync() {
        if (!properties.isSyncEnabled()) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.info("Instagram reel sync skipped because a previous run is still active");
            return;
        }

        try {
            InstagramReelSyncResult result = instagramReelSyncService.syncLatestReels();
            log.info(
                "Instagram scheduled sync completed: fetched={}, created={}, updated={}, skipped={}, failedInsights={}, thumbnailsCached={}, thumbnailCacheFailed={}",
                result.getFetchedCount(),
                result.getCreatedCount(),
                result.getUpdatedCount(),
                result.getSkippedCount(),
                result.getFailedInsightCount(),
                result.getThumbnailCachedCount(),
                result.getThumbnailCacheFailedCount()
            );
        } catch (RuntimeException ex) {
            log.warn("Instagram scheduled sync failed: {}", ex.getMessage());
        } finally {
            running.set(false);
        }
    }
}
