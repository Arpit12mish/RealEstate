package com.brandPitara.sfs.cms.media.service;

import com.brandPitara.sfs.cms.media.config.CmsMediaProperties;
import com.brandPitara.sfs.cms.media.domain.*;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.media.service.MediaObjectStorageService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

class CmsMediaCleanupServiceTest {
    @Test
    void schedulerUsesBoundedConfiguredBatch() {
        CmsMediaPersistenceService persistence = mock(CmsMediaPersistenceService.class);
        CmsMediaProperties properties = new CmsMediaProperties();
        properties.setCleanupBatchSize(1000);
        when(persistence.cleanupCandidates(any(), any(), eq(500))).thenReturn(java.util.List.of());
        new CmsMediaCleanupService(persistence, mock(MediaObjectStorageService.class), properties,
                mock(CmsMediaMetrics.class)).cleanup();
        verify(persistence).cleanupCandidates(any(), any(), eq(500));
    }

    @Test
    void claimedExpiredAssetIsDeletedOutsideClaimTransactionAndFinished() {
        CmsMediaPersistenceService persistence = mock(CmsMediaPersistenceService.class);
        MediaObjectStorageService storage = mock(MediaObjectStorageService.class);
        CmsMediaMetrics metrics = mock(CmsMediaMetrics.class);
        CmsMediaAssetEntity asset = CmsMediaAssetEntity.builder().id(7L).mediaType(CmsMediaType.IMAGE)
                .status(CmsMediaStatus.DELETING).storageBucket("private").storageKey("cms/images/x").build();
        when(persistence.claimCleanup(7L)).thenReturn(Optional.of(asset));

        new CmsMediaCleanupService(persistence, storage, new CmsMediaProperties(), metrics).cleanupOne(7L);

        var order = inOrder(persistence, storage);
        order.verify(persistence).claimCleanup(7L);
        order.verify(storage).delete("private", "cms/images/x");
        order.verify(persistence).finishCleanup(7L);
    }

    @Test
    void storageFailureReleasesClaimAndReadyAssetCannotBeClaimed() {
        CmsMediaPersistenceService persistence = mock(CmsMediaPersistenceService.class);
        MediaObjectStorageService storage = mock(MediaObjectStorageService.class);
        CmsMediaMetrics metrics = mock(CmsMediaMetrics.class);
        CmsMediaAssetEntity asset = CmsMediaAssetEntity.builder().id(8L).mediaType(CmsMediaType.VIDEO)
                .storageBucket("private").storageKey("cms/videos/x").build();
        when(persistence.claimCleanup(8L)).thenReturn(Optional.of(asset));
        doThrow(new RuntimeException("S3 unavailable")).when(storage).delete(anyString(), anyString());
        new CmsMediaCleanupService(persistence, storage, new CmsMediaProperties(), metrics).cleanupOne(8L);
        verify(persistence).releaseCleanup(8L);
        verify(persistence, never()).finishCleanup(8L);

        when(persistence.claimCleanup(9L)).thenReturn(Optional.empty());
        new CmsMediaCleanupService(persistence, storage, new CmsMediaProperties(), metrics).cleanupOne(9L);
        verify(storage, never()).delete(anyString(), eq("ready"));
    }
}
