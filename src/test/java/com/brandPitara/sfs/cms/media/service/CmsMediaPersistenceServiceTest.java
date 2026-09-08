package com.brandPitara.sfs.cms.media.service;

import com.brandPitara.sfs.cms.media.domain.*;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.cms.media.validation.CmsMediaValidationResult;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CmsMediaPersistenceServiceTest {
    @Test
    void strandedDeletingClaimCanBeRecoveredAfterProcessRestart() {
        CmsMediaAssetRepository repository = mock(CmsMediaAssetRepository.class);
        CmsMediaAssetEntity deleting = CmsMediaAssetEntity.builder().id(90L)
                .status(CmsMediaStatus.DELETING).build();
        when(repository.findByIdForUpdate(90L)).thenReturn(Optional.of(deleting));
        when(repository.saveAndFlush(deleting)).thenReturn(deleting);
        CmsMediaPersistenceService service = new CmsMediaPersistenceService(repository, mock(DashboardUserRepository.class));

        assertThat(service.claimCleanup(90L)).contains(deleting);
    }

    @Test
    void lockedReadyTransitionIsIdempotentForConcurrentFinalizeWinner() {
        CmsMediaAssetRepository repository = mock(CmsMediaAssetRepository.class);
        CmsMediaAssetEntity alreadyReady = CmsMediaAssetEntity.builder().id(91L).mediaType(CmsMediaType.IMAGE)
                .status(CmsMediaStatus.READY).sizeBytes(100L).readyAt(OffsetDateTime.now()).build();
        when(repository.findByIdForUpdate(91L)).thenReturn(Optional.of(alreadyReady));
        CmsMediaPersistenceService service = new CmsMediaPersistenceService(repository, mock(DashboardUserRepository.class));

        CmsMediaAssetEntity result = service.markReady(91L, 200L, "different",
                new CmsMediaValidationResult(10, 10, null));

        assertThat(result).isSameAs(alreadyReady);
        assertThat(result.getSizeBytes()).isEqualTo(100L);
        verify(repository, never()).saveAndFlush(any());
    }
}
