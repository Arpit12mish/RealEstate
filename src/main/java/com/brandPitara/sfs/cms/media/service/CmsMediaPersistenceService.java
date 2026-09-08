package com.brandPitara.sfs.cms.media.service;

import com.brandPitara.sfs.cms.media.domain.*;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.exception.*;
import com.brandPitara.sfs.cms.media.repository.CmsMediaAssetRepository;
import com.brandPitara.sfs.cms.media.validation.CmsMediaValidationResult;
import com.brandPitara.sfs.dashboard.user.repository.DashboardUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CmsMediaPersistenceService {
    private final CmsMediaAssetRepository repository;
    private final DashboardUserRepository userRepository;

    @Transactional
    public CmsMediaAssetEntity create(
            CmsMediaType type, String bucket, String key, String filename,
            String contentType, long declaredSize, Long creatorId
    ) {
        return repository.saveAndFlush(CmsMediaAssetEntity.builder()
                .mediaType(type)
                .status(CmsMediaStatus.PENDING_UPLOAD)
                .storageBucket(bucket)
                .storageKey(key)
                .originalFilename(filename.trim())
                .contentType(contentType)
                .declaredSizeBytes(declaredSize)
                .createdBy(userRepository.getReferenceById(creatorId))
                .build());
    }

    @Transactional(readOnly = true)
    public CmsMediaAssetEntity get(Long id) {
        return repository.findDetailedById(id).orElseThrow(() -> CmsMediaApiException.notFound(id));
    }

    @Transactional(readOnly = true)
    public Page<CmsMediaAssetEntity> list(
            CmsMediaType type, CmsMediaStatus status, Long creator, String search, Pageable pageable
    ) {
        return repository.findPage(type, status, creator, pattern(search), pageable);
    }

    /**
     * Pre-builds the full LIKE pattern in Java (mirrors CmsMetadataServiceImpl.pattern()) instead of
     * concatenating it in JPQL. When :search is bound as SQL NULL inside a JPQL concat()/|| expression,
     * PgJDBC cannot statically infer the parameter's type and falls back to binding it as bytea, which
     * fails "function lower(bytea) does not exist" once lower() is applied to the concat result — the
     * exact class of issue already documented on cms_revision_has_tag in V155. Binding a plain,
     * already-wildcarded String (or null) directly against `LIKE :search` gives Postgres an unambiguous
     * text context and needs no concat() at all, so the type-inference trap never applies.
     */
    private String pattern(String value) {
        if (value == null || value.isBlank()) return null;
        return "%" + value.trim().toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%";
    }

    @Transactional
    public CmsMediaAssetEntity markReady(
            Long id, long actualSize, String etag, CmsMediaValidationResult validation
    ) {
        CmsMediaAssetEntity asset = repository.findByIdForUpdate(id)
                .orElseThrow(() -> CmsMediaApiException.notFound(id));
        if (asset.getStatus() == CmsMediaStatus.READY) return asset;
        if (asset.getStatus() != CmsMediaStatus.PENDING_UPLOAD) throw new CmsMediaUploadExpiredException();
        asset.setStatus(CmsMediaStatus.READY);
        asset.setSizeBytes(actualSize);
        asset.setObjectETag(safeEtag(etag));
        asset.setWidth(validation.width());
        asset.setHeight(validation.height());
        asset.setDurationMillis(validation.durationMillis());
        asset.setReadyAt(OffsetDateTime.now());
        asset.setFailedAt(null);
        asset.setFailureCode(null);
        return repository.saveAndFlush(asset);
    }

    @Transactional
    public CmsMediaAssetEntity markFailed(Long id, String failureCode) {
        CmsMediaAssetEntity asset = repository.findByIdForUpdate(id)
                .orElseThrow(() -> CmsMediaApiException.notFound(id));
        if (asset.getStatus() == CmsMediaStatus.READY) return asset;
        asset.setStatus(CmsMediaStatus.FAILED);
        asset.setFailedAt(OffsetDateTime.now());
        asset.setFailureCode(failureCode);
        return repository.saveAndFlush(asset);
    }

    @Transactional(readOnly = true)
    public List<Long> cleanupCandidates(OffsetDateTime pendingBefore, OffsetDateTime failedBefore, int size) {
        return repository.findCleanupCandidates(pendingBefore, failedBefore, PageRequest.of(0, size));
    }

    @Transactional
    public Optional<CmsMediaAssetEntity> claimCleanup(Long id) {
        Optional<CmsMediaAssetEntity> found = repository.findByIdForUpdate(id);
        if (found.isEmpty()) return Optional.empty();
        CmsMediaAssetEntity asset = found.get();
        if (asset.getStatus() != CmsMediaStatus.PENDING_UPLOAD
                && asset.getStatus() != CmsMediaStatus.FAILED
                && asset.getStatus() != CmsMediaStatus.DELETING) {
            return Optional.empty();
        }
        asset.setStatus(CmsMediaStatus.DELETING);
        repository.saveAndFlush(asset);
        return Optional.of(asset);
    }

    @Transactional
    public void finishCleanup(Long id) {
        repository.findByIdForUpdate(id).ifPresent(asset -> {
            if (asset.getStatus() == CmsMediaStatus.DELETING) repository.delete(asset);
        });
    }

    @Transactional
    public void releaseCleanup(Long id) {
        repository.findByIdForUpdate(id).ifPresent(asset -> {
            if (asset.getStatus() == CmsMediaStatus.DELETING) {
                asset.setStatus(CmsMediaStatus.FAILED);
                asset.setFailedAt(OffsetDateTime.now());
                asset.setFailureCode("CLEANUP_STORAGE_UNAVAILABLE");
            }
        });
    }

    private String safeEtag(String etag) {
        if (etag == null) return null;
        return etag.length() <= 128 ? etag : etag.substring(0, 128);
    }
}
