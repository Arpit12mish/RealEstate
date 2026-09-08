package com.brandPitara.sfs.cms.media.service;

import com.brandPitara.sfs.cms.media.config.CmsMediaProperties;
import com.brandPitara.sfs.cms.media.domain.*;
import com.brandPitara.sfs.cms.media.dto.*;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.exception.CmsMediaApiException;
import com.brandPitara.sfs.cms.media.exception.CmsMediaUploadExpiredException;
import com.brandPitara.sfs.cms.media.security.CmsMediaAccessPolicy;
import com.brandPitara.sfs.cms.media.validation.*;
import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.*;
import com.brandPitara.sfs.media.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.*;

import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CmsMediaServiceImpl implements CmsMediaService {
    private final CmsMediaPersistenceService persistence;
    private final CmsMediaValidator validator;
    private final CmsMediaAccessPolicy accessPolicy;
    private final MediaObjectStorageService storage;
    private final CmsMediaProperties properties;
    private final CmsMediaMetrics metrics;
    private final DashboardActionAuditService auditService;

    @Override
    public CmsMediaUploadResponse createUpload(CmsMediaUploadRequest request, Authentication authentication) {
        Long actorId = accessPolicy.userId(authentication);
        if (actorId == null || !accessPolicy.canUpload(authentication)) throw new AccessDeniedException("CMS media upload permission is required");
        String contentType = validator.validateDeclaration(
                request.mediaType(), request.filename(), request.contentType(), request.sizeBytes()
        );
        String bucket = configuredBucket();
        String key = buildStorageKey(request.mediaType(), contentType);
        CmsMediaAssetEntity asset = persistence.create(
                request.mediaType(), bucket, key, request.filename(), contentType, request.sizeBytes(), actorId
        );
        try {
            PresignedUploadResult upload = storage.createImmutablePresignedUpload(
                    bucket, key, contentType, request.sizeBytes(), CmsMediaProperties.IMMUTABLE_CACHE_CONTROL
            );
            metrics.uploadRequested(request.mediaType(), "success");
            auditService.record(DashboardAuditAction.CMS_MEDIA_UPLOAD_CREATED,
                    ReviewEntityType.CMS_MEDIA_ASSET, asset.getId(), null);
            return new CmsMediaUploadResponse(
                    asset.getId(), asset.getStatus().name(), upload.uploadUrl(), upload.expiresInSeconds(),
                    asset.getCreatedAt().plusHours(properties.getPendingRetentionHours()), upload.requiredHeaders()
            );
        } catch (RuntimeException failure) {
            persistence.markFailed(asset.getId(), "PRESIGN_FAILED");
            metrics.uploadRequested(request.mediaType(), "failed");
            auditService.record(DashboardAuditAction.CMS_MEDIA_FAILED,
                    ReviewEntityType.CMS_MEDIA_ASSET, asset.getId(), null);
            throw CmsMediaApiException.storageUnavailable();
        }
    }

    @Override
    public CmsMediaAssetResponse complete(Long id, Authentication authentication) {
        CmsMediaAssetEntity snapshot = persistence.get(id);
        if (!accessPolicy.canFinalize(authentication, snapshot.getCreatedBy().getId())) {
            throw new AccessDeniedException("Only the upload owner or an administrator may finalize CMS media");
        }
        if (snapshot.getStatus() == CmsMediaStatus.READY) return response(persistence.get(id), false);
        if (snapshot.getStatus() != CmsMediaStatus.PENDING_UPLOAD) throw CmsMediaApiException.validationFailed();
        if (snapshot.getCreatedAt().plusHours(Math.max(properties.getPendingRetentionHours(), 1))
                .isBefore(OffsetDateTime.now())) {
            persistence.markFailed(id, "UPLOAD_EXPIRED");
            throw new CmsMediaUploadExpiredException();
        }

        StoredObjectMetadata metadata;
        try {
            metadata = storage.head(snapshot.getStorageBucket(), snapshot.getStorageKey());
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) throw CmsMediaApiException.uploadIncomplete();
            throw CmsMediaApiException.storageUnavailable();
        } catch (RuntimeException exception) {
            throw CmsMediaApiException.storageUnavailable();
        }
        if (metadata.contentLength() != snapshot.getDeclaredSizeBytes()
                || metadata.contentLength() <= 0
                || metadata.contentLength() > validator.limit(snapshot.getMediaType())
                || metadata.contentType() == null
                || !snapshot.getContentType().equals(metadata.contentType().trim().toLowerCase(Locale.ROOT))) {
            return failValidation(snapshot);
        }

        byte[] prefix;
        try {
            prefix = storage.readPrefix(snapshot.getStorageBucket(), snapshot.getStorageKey(),
                    Math.min(Math.max(properties.getValidationPrefixBytes(), 4096), 1024 * 1024));
        } catch (RuntimeException exception) {
            throw CmsMediaApiException.storageUnavailable();
        }

        CmsMediaValidationResult validation;
        try {
            validation = validator.validateObject(snapshot.getMediaType(), snapshot.getContentType(), prefix);
        } catch (CmsMediaApiException invalid) {
            return failValidation(snapshot);
        }

        persistence.markReady(id, metadata.contentLength(), metadata.eTag(), validation);
        metrics.uploadCompleted(snapshot.getMediaType(), "success");
        auditService.record(DashboardAuditAction.CMS_MEDIA_READY,
                ReviewEntityType.CMS_MEDIA_ASSET, id, null);
        return response(persistence.get(id), false);
    }

    @Override
    public Page<CmsMediaAssetResponse> list(
            CmsMediaType type, CmsMediaStatus status, Long createdBy, String search, Pageable pageable
    ) {
        return persistence.list(type, status, createdBy, search, pageable)
                .map(asset -> CmsMediaAssetResponse.from(asset, null, null));
    }

    @Override
    public CmsMediaAssetResponse get(Long id) {
        return response(persistence.get(id), true);
    }

    private CmsMediaAssetResponse failValidation(CmsMediaAssetEntity snapshot) {
        persistence.markFailed(snapshot.getId(), "OBJECT_VALIDATION_FAILED");
        metrics.validationFailed(snapshot.getMediaType());
        metrics.uploadCompleted(snapshot.getMediaType(), "failed");
        auditService.record(DashboardAuditAction.CMS_MEDIA_FAILED,
                ReviewEntityType.CMS_MEDIA_ASSET, snapshot.getId(), null);
        throw CmsMediaApiException.validationFailed();
    }

    private CmsMediaAssetResponse response(CmsMediaAssetEntity asset, boolean includePreview) {
        if (!includePreview || asset.getStatus() != CmsMediaStatus.READY) {
            return CmsMediaAssetResponse.from(asset, null, null);
        }
        try {
            PresignedReadResult read = storage.createPresignedRead(asset.getStorageBucket(), asset.getStorageKey());
            return CmsMediaAssetResponse.from(asset, read.url(), read.expiresInSeconds());
        } catch (RuntimeException exception) {
            log.warn("CMS media presigned read failed for asset {}: {}", asset.getId(), exception.toString(), exception);
            throw CmsMediaApiException.storageUnavailable();
        }
    }

    private String configuredBucket() {
        String bucket = properties.getBucket();
        if (bucket == null || bucket.isBlank()) {
            log.warn("CMS media request rejected: app.cms.media.bucket (CMS_MEDIA_S3_BUCKET) is not configured.");
            throw CmsMediaApiException.storageUnavailable();
        }
        return bucket.trim();
    }

    private String buildStorageKey(CmsMediaType type, String contentType) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String family = type == CmsMediaType.IMAGE ? "images" : "videos";
        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "video/mp4" -> "mp4";
            default -> throw CmsMediaApiException.invalidType("Unsupported CMS media content type.");
        };
        return "cms/" + family + "/" + now.getYear() + "/" + String.format("%02d", now.getMonthValue())
                + "/" + UUID.randomUUID() + "." + extension;
    }
}
