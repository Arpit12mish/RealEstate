package com.brandPitara.sfs.cms.media.service;

import com.brandPitara.sfs.cms.media.config.CmsMediaProperties;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.media.service.MediaObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CmsMediaCleanupService {
    private final CmsMediaPersistenceService persistence;
    private final MediaObjectStorageService storage;
    private final CmsMediaProperties properties;
    private final CmsMediaMetrics metrics;

    @Scheduled(cron = "${app.cms.media.cleanup-cron:0 17 * * * *}")
    public void cleanup() {
        var ids = persistence.cleanupCandidates(
                OffsetDateTime.now().minusHours(Math.max(properties.getPendingRetentionHours(), 1)),
                OffsetDateTime.now().minusDays(Math.max(properties.getFailedRetentionDays(), 1)),
                Math.min(Math.max(properties.getCleanupBatchSize(), 1), 500)
        );
        for (Long id : ids) cleanupOne(id);
    }

    void cleanupOne(Long id) {
        var claimed = persistence.claimCleanup(id);
        if (claimed.isEmpty()) return;
        CmsMediaAssetEntity asset = claimed.get();
        try {
            storage.delete(asset.getStorageBucket(), asset.getStorageKey());
            persistence.finishCleanup(id);
            metrics.cleanupDeleted(asset.getMediaType(), "success");
        } catch (RuntimeException failure) {
            persistence.releaseCleanup(id);
            metrics.cleanupDeleted(asset.getMediaType(), "failed");
        }
    }
}
