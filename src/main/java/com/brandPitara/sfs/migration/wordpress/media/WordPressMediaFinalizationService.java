package com.brandPitara.sfs.migration.wordpress.media;

import com.brandPitara.sfs.cms.media.domain.CmsMediaStatus;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.cms.media.service.CmsMediaPersistenceService;
import com.brandPitara.sfs.cms.media.validation.CmsMediaValidationResult;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Every method here is a short, independent database transaction (Spring Data / {@code @Service}
 * default propagation) - none of them ever run while external storage I/O is in flight. This is
 * a separate bean (not methods on the orchestrating {@link WordPressMediaImportService}) so that
 * {@code @Transactional} actually applies: a method calling another {@code @Transactional} method
 * on {@code this} would bypass the Spring AOP proxy and silently run without a transaction.
 * All CMS-media-lifecycle mutations delegate to the existing, already-tested
 * {@link CmsMediaPersistenceService} - this class never mutates a {@code CmsMediaAssetEntity}
 * field directly, so the legitimate {@code PENDING_UPLOAD -> READY/FAILED} transition rules are
 * never bypassed.
 */
@Service
@RequiredArgsConstructor
public class WordPressMediaFinalizationService {

    private final CmsMediaAssetRepository mediaAssetRepository;
    private final CmsMediaPersistenceService mediaPersistence;
    private final WordPressMediaMappingRepository mappingRepository;

    @Transactional(readOnly = true)
    public Optional<WordPressMediaMappingEntity> findMapping(String sourceSystem, long attachmentId) {
        return mappingRepository.findBySourceSystemAndWordPressAttachmentId(sourceSystem, attachmentId);
    }

    @Transactional(readOnly = true)
    public Optional<CmsMediaAssetEntity> findReadyAssetByStorageKey(String storageKey) {
        return mediaAssetRepository.findByStorageKey(storageKey)
                .filter(asset -> asset.getStatus() == CmsMediaStatus.READY);
    }

    @Transactional(readOnly = true)
    public CmsMediaAssetEntity requireById(Long id) {
        return mediaAssetRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("CmsMediaAssetEntity " + id + " disappeared."));
    }

    /**
     * Creates the {@code PENDING_UPLOAD} row and immediately transitions it to {@code READY} -
     * both steps only ever run after the object has already been uploaded and verified by the
     * caller. If a concurrent import already inserted the same content-addressed storage key
     * (unique constraint violation) between this call's caller checking
     * {@link #findReadyAssetByStorageKey} and calling this method, that winner's row is reused
     * instead of failing the whole import - see requirement "concurrent mapping race". Deliberately
     * NOT itself {@code @Transactional}: {@code create} and {@code markReady} each already commit
     * in their own short transaction on {@link CmsMediaPersistenceService} (the same two-step
     * pattern {@code CmsMediaServiceImpl} uses for a human upload). Wrapping both in one shared
     * transaction here would mean a constraint violation from {@code create} poisons that
     * transaction for the rest of this method, breaking the fallback lookup below.
     */
    public CmsMediaAssetEntity createAndMarkReady(
            CmsMediaType type, String bucket, String key, String filename, String contentType,
            long size, DashboardUserEntity actor, String etag, CmsMediaValidationResult validation
    ) {
        CmsMediaAssetEntity created;
        try {
            created = mediaPersistence.create(type, bucket, key, filename, contentType, size, actor.getId());
        } catch (DataIntegrityViolationException raceLost) {
            return mediaAssetRepository.findByStorageKey(key)
                    .orElseThrow(() -> raceLost);
        }
        return mediaPersistence.markReady(created.getId(), size, etag, validation);
    }

    @Transactional
    public void upsertCompletedMapping(
            String sourceSystem, long attachmentId, Long cmsMediaAssetId, String sourceUploadPath,
            String sourceSha256, long sourceSize, String objectKey
    ) {
        WordPressMediaMappingEntity mapping = existingOrNewMapping(sourceSystem, attachmentId);
        mapping.setCmsMediaAsset(mediaAssetRepository.getReferenceById(cmsMediaAssetId));
        mapping.setSourceUploadPath(sourceUploadPath);
        mapping.setSourceSha256(sourceSha256);
        mapping.setSourceSizeBytes(sourceSize);
        mapping.setObjectKey(objectKey);
        mapping.setMigrationState(WordPressMediaMigrationState.COMPLETED);
        mapping.setErrorCode(null);
        mappingRepository.saveAndFlush(mapping);
    }

    /** Never called when a COMPLETED mapping already exists for this attachment - see the orchestrator. */
    @Transactional
    public void upsertFailedMapping(
            String sourceSystem, long attachmentId, String sourceUploadPath, String sourceSha256,
            long sourceSize, String objectKey, String errorCode
    ) {
        WordPressMediaMappingEntity mapping = existingOrNewMapping(sourceSystem, attachmentId);
        if (mapping.getMigrationState() == WordPressMediaMigrationState.COMPLETED) {
            throw new IllegalStateException(
                    "Refusing to downgrade a COMPLETED media mapping for attachment " + attachmentId + " to FAILED.");
        }
        mapping.setCmsMediaAsset(null);
        mapping.setSourceUploadPath(sourceUploadPath);
        mapping.setSourceSha256(sourceSha256);
        mapping.setSourceSizeBytes(sourceSize);
        mapping.setObjectKey(objectKey);
        mapping.setMigrationState(WordPressMediaMigrationState.FAILED);
        mapping.setErrorCode(errorCode);
        mappingRepository.saveAndFlush(mapping);
    }

    private WordPressMediaMappingEntity existingOrNewMapping(String sourceSystem, long attachmentId) {
        return mappingRepository.findBySourceSystemAndWordPressAttachmentId(sourceSystem, attachmentId)
                .orElseGet(() -> WordPressMediaMappingEntity.builder()
                        .sourceSystem(sourceSystem)
                        .wordPressAttachmentId(attachmentId)
                        .build());
    }
}
