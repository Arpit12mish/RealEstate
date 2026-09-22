package com.brandPitara.sfs.migration.wordpress.media;

import com.brandPitara.sfs.cms.media.config.CmsMediaProperties;
import com.brandPitara.sfs.cms.media.domain.CmsMediaStatus;
import com.brandPitara.sfs.cms.media.domain.CmsMediaType;
import com.brandPitara.sfs.cms.media.entity.CmsMediaAssetEntity;
import com.brandPitara.sfs.cms.media.exception.CmsMediaApiException;
import com.brandPitara.sfs.cms.media.validation.CmsMediaValidationResult;
import com.brandPitara.sfs.cms.media.validation.CmsMediaValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Orchestrates one WordPress attachment's real media-upload lifecycle: validate source -> store
 * bytes -> verify stored object -> mark READY -> persist attachment mapping -> return the CMS
 * media asset ID. Deliberately NOT {@code @Transactional} - every external call (zip read,
 * storage put/head/exists) happens with no database transaction open; the short, isolated DB
 * steps are delegated to {@link WordPressMediaFinalizationService} (a separate bean, so its
 * {@code @Transactional} methods actually go through the Spring proxy). This mirrors
 * {@code CmsMediaServiceImpl}, which does the same thing for the human-upload path via
 * {@code CmsMediaPersistenceService}.
 *
 * <h2>Recovery process</h2>
 * A {@code STORED_OBJECT_MISSING} failure means a COMPLETED mapping's object was deleted
 * out-of-band. Recovery is explicit: delete that one {@code wordpress_migration_media_mapping}
 * row (identified by source_system + wordpress_attachment_id) and rerun the import for that
 * attachment - the next run has no mapping to trust, re-validates and re-uploads from the
 * original zip entry, and writes a fresh COMPLETED row. A {@code SOURCE_FINGERPRINT_CHANGED}
 * failure means the WordPress export itself changed the attachment's bytes; the same explicit
 * delete-then-rerun is the only supported repair path - this class never silently overwrites a
 * completed mapping.
 */
@Service
@RequiredArgsConstructor
public class WordPressMediaImportService {

    public static final String SOURCE_SYSTEM = "wordpress";
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of("video/mp4");
    private static final long EXTRACTOR_PER_ENTRY_CAP = 512L * 1024 * 1024;
    private static final long EXTRACTOR_TOTAL_CAP = Long.MAX_VALUE;

    private final WordPressMediaFinalizationService finalization;
    private final WordPressMigrationMediaStorage storage;
    private final WordPressMediaOrphanCleanupReport orphanReport;
    private final CmsMediaProperties cmsMediaProperties;
    private final CmsMediaValidator validator;

    public WordPressMediaImportOutcome importAttachment(WordPressMediaImportRequest request) {
        long attachmentId = request.attachmentId();
        String bucket = configuredBucket();

        Optional<WordPressMediaMappingEntity> existingMapping =
                finalization.findMapping(SOURCE_SYSTEM, attachmentId);

        WordPressMediaExtractor extractor =
                new WordPressMediaExtractor(EXTRACTOR_PER_ENTRY_CAP, EXTRACTOR_TOTAL_CAP);
        WordPressMediaExtractor.ExtractedMedia extracted;
        try {
            extracted = extractor.extractOne(
                    request.zipPath(), attachmentId, request.sourceUploadPath(), request.outputDir()
            );
        } catch (IOException extractionFailure) {
            throw new WordPressMediaImportException(
                    "ZIP_EXTRACTION_FAILED", "Failed to extract attachment " + attachmentId
                    + " from the media archive.", extractionFailure
            );
        }

        CmsMediaType mediaType = mediaTypeFor(extracted.detectedMimeType());
        if (mediaType == null) {
            throw WordPressMediaImportException.disallowedMimeType(attachmentId, extracted.detectedMimeType());
        }
        requireExtensionConsistency(attachmentId, request.sourceUploadPath(), extracted.detectedMimeType());
        long limit = validator.limit(mediaType);
        if (extracted.sizeBytes() > limit) {
            throw WordPressMediaImportException.sizeLimitExceeded(attachmentId, extracted.sizeBytes(), limit);
        }

        if (existingMapping.isPresent() && existingMapping.get().getMigrationState() == WordPressMediaMigrationState.COMPLETED) {
            return reuseCompletedMapping(existingMapping.get(), extracted, bucket);
        }

        CmsMediaValidationResult validation = decodeAndValidate(attachmentId, mediaType, extracted);

        String objectKey = WordPressMediaObjectKeyFactory.build(
                mediaType, extracted.detectedMimeType(), extracted.sha256Hex(), OffsetDateTime.now(ZoneOffset.UTC)
        );

        Optional<CmsMediaAssetEntity> existingAssetAtKey = finalization.findReadyAssetByStorageKey(objectKey);
        if (existingAssetAtKey.isPresent()) {
            return reuseExistingObject(attachmentId, request, extracted, bucket, objectKey, existingAssetAtKey.get(), existingMapping.isPresent());
        }

        boolean alreadyStoredOutOfBand = storage.exists(bucket, objectKey);
        boolean createdByThisOperation = false;
        if (!alreadyStoredOutOfBand) {
            try {
                storage.store(bucket, objectKey, extracted.localFile(), extracted.detectedMimeType());
                createdByThisOperation = true;
            } catch (RuntimeException uploadFailure) {
                finalization.upsertFailedMapping(
                        SOURCE_SYSTEM, attachmentId, request.sourceUploadPath(), extracted.sha256Hex(),
                        extracted.sizeBytes(), objectKey, "UPLOAD_FAILED"
                );
                throw WordPressMediaImportException.uploadFailed(attachmentId, uploadFailure);
            }
        }

        WordPressStoredObjectMetadata verified;
        try {
            verified = verifyStoredObject(attachmentId, extracted, objectKey, bucket);
        } catch (WordPressMediaImportException verificationFailure) {
            recordFailureAndMaybeCleanup(attachmentId, request, extracted, objectKey, bucket,
                    verificationFailure.errorCode(), createdByThisOperation);
            throw verificationFailure;
        }

        CmsMediaAssetEntity asset;
        try {
            asset = finalization.createAndMarkReady(
                    mediaType, bucket, objectKey, safeOriginalFilename(request.sourceUploadPath(), extracted.detectedMimeType()),
                    extracted.detectedMimeType(), extracted.sizeBytes(), request.actor(),
                    verified.sha256Hex() != null ? verified.sha256Hex() : extracted.sha256Hex(), validation
            );
        } catch (RuntimeException dbFailure) {
            recordFailureAndMaybeCleanup(attachmentId, request, extracted, objectKey, bucket,
                    "DB_FINALIZATION_FAILED", createdByThisOperation);
            throw WordPressMediaImportException.databaseFinalizationFailed(attachmentId, dbFailure);
        }

        finalization.upsertCompletedMapping(
                SOURCE_SYSTEM, attachmentId, asset.getId(), request.sourceUploadPath(),
                extracted.sha256Hex(), extracted.sizeBytes(), objectKey
        );
        return new WordPressMediaImportOutcome(
                attachmentId, asset.getId(), existingMapping.isPresent(), alreadyStoredOutOfBand, objectKey, null
        );
    }

    private WordPressMediaImportOutcome reuseCompletedMapping(
            WordPressMediaMappingEntity mapping, WordPressMediaExtractor.ExtractedMedia extracted, String bucket
    ) {
        long attachmentId = mapping.getWordPressAttachmentId();
        if (!mapping.getSourceSha256().equals(extracted.sha256Hex())) {
            throw WordPressMediaImportException.fingerprintMismatch(
                    attachmentId, mapping.getSourceSha256(), extracted.sha256Hex()
            );
        }
        if (!storage.exists(bucket, mapping.getObjectKey())) {
            throw WordPressMediaImportException.storedObjectMissing(attachmentId, bucket, mapping.getObjectKey());
        }
        WordPressStoredObjectMetadata head = storage.head(bucket, mapping.getObjectKey());
        if (head.contentLength() != mapping.getSourceSizeBytes()) {
            throw WordPressMediaImportException.verificationFailed(
                    attachmentId, "stored size no longer matches the recorded mapping size");
        }
        CmsMediaAssetEntity asset = finalization.requireById(mapping.getCmsMediaAsset().getId());
        if (asset.getStatus() != CmsMediaStatus.READY) {
            throw WordPressMediaImportException.verificationFailed(
                    attachmentId, "the mapped CMS media asset is no longer READY");
        }
        return new WordPressMediaImportOutcome(attachmentId, asset.getId(), true, true, mapping.getObjectKey(), null);
    }

    private WordPressMediaImportOutcome reuseExistingObject(
            long attachmentId, WordPressMediaImportRequest request, WordPressMediaExtractor.ExtractedMedia extracted,
            String bucket, String objectKey, CmsMediaAssetEntity asset, boolean hadPriorMapping
    ) {
        if (!Objects.equals(asset.getSizeBytes(), extracted.sizeBytes())) {
            throw WordPressMediaImportException.existingObjectDifferentBytes(attachmentId, objectKey);
        }
        if (!storage.exists(bucket, objectKey)) {
            throw WordPressMediaImportException.storedObjectMissing(attachmentId, bucket, objectKey);
        }
        finalization.upsertCompletedMapping(
                SOURCE_SYSTEM, attachmentId, asset.getId(), request.sourceUploadPath(),
                extracted.sha256Hex(), extracted.sizeBytes(), objectKey
        );
        return new WordPressMediaImportOutcome(attachmentId, asset.getId(), hadPriorMapping, true, objectKey, null);
    }

    private WordPressStoredObjectMetadata verifyStoredObject(
            long attachmentId, WordPressMediaExtractor.ExtractedMedia extracted, String objectKey, String bucket
    ) {
        WordPressStoredObjectMetadata head;
        try {
            head = storage.head(bucket, objectKey);
        } catch (WordPressMediaStorageObjectNotFoundException notFound) {
            throw WordPressMediaImportException.verificationFailed(attachmentId, "object not found immediately after upload");
        } catch (RuntimeException headFailure) {
            throw WordPressMediaImportException.verificationFailed(attachmentId, "HEAD request failed: " + headFailure.getMessage());
        }
        if (head.contentLength() != extracted.sizeBytes()) {
            throw WordPressMediaImportException.verificationFailed(
                    attachmentId, "stored size " + head.contentLength() + " != source size " + extracted.sizeBytes());
        }
        if (head.contentType() == null || !extracted.detectedMimeType().equalsIgnoreCase(head.contentType())) {
            throw WordPressMediaImportException.verificationFailed(attachmentId, "stored content-type mismatch");
        }
        if (head.sha256Hex() != null && !head.sha256Hex().equalsIgnoreCase(extracted.sha256Hex())) {
            throw WordPressMediaImportException.checksumMismatch(attachmentId);
        }
        return head;
    }

    private void recordFailureAndMaybeCleanup(
            long attachmentId, WordPressMediaImportRequest request, WordPressMediaExtractor.ExtractedMedia extracted,
            String objectKey, String bucket, String errorCode, boolean createdByThisOperation
    ) {
        finalization.upsertFailedMapping(
                SOURCE_SYSTEM, attachmentId, request.sourceUploadPath(), extracted.sha256Hex(),
                extracted.sizeBytes(), objectKey, errorCode
        );
        if (!createdByThisOperation) {
            orphanReport.record(attachmentId, bucket, objectKey, "not-created-by-this-operation:" + errorCode);
            return;
        }
        // Content-addressed keys mean a different attachment with byte-identical bytes can race
        // this same key: between our own store() and this cleanup, a concurrent importAttachment
        // for that other attachment may have already verified and finalized a READY asset here
        // (createAndMarkReady's own DataIntegrityViolationException handling only protects the
        // two concurrent DB inserts from each other - it does not stop THIS operation's cleanup,
        // triggered by an unrelated verification/DB failure, from deleting the object the winner
        // now depends on). Re-checking immediately before delete - rather than trusting the
        // createdByThisOperation flag captured earlier - closes that window down to the gap
        // between this SELECT and the delete call, instead of the whole upload+verify duration.
        if (finalization.findReadyAssetByStorageKey(objectKey).isPresent()) {
            orphanReport.record(attachmentId, bucket, objectKey, "adopted-by-concurrent-import:" + errorCode);
            return;
        }
        try {
            storage.deleteOrphan(bucket, objectKey);
        } catch (RuntimeException cleanupFailure) {
            orphanReport.record(attachmentId, bucket, objectKey, "cleanup-failed:" + errorCode);
        }
    }

    private CmsMediaValidationResult decodeAndValidate(
            long attachmentId, CmsMediaType mediaType, WordPressMediaExtractor.ExtractedMedia extracted
    ) {
        int prefixSize = Math.min(Math.max(cmsMediaProperties.getValidationPrefixBytes(), 4096), 1024 * 1024);
        byte[] prefix = readLocalPrefix(extracted.localFile(), prefixSize);
        try {
            return validator.validateObject(mediaType, extracted.detectedMimeType(), prefix);
        } catch (CmsMediaApiException corrupt) {
            throw WordPressMediaImportException.corruptMedia(attachmentId, extracted.detectedMimeType());
        }
    }

    private byte[] readLocalPrefix(Path file, int maxBytes) {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[maxBytes];
            int total = 0;
            int read;
            while (total < buffer.length && (read = in.read(buffer, total, buffer.length - total)) != -1) {
                total += read;
            }
            return total == buffer.length ? buffer : java.util.Arrays.copyOf(buffer, total);
        } catch (IOException e) {
            throw new WordPressMediaImportException("LOCAL_READ_FAILED", "Could not read extracted file for validation.", e);
        }
    }

    private CmsMediaType mediaTypeFor(String detectedMimeType) {
        if (ALLOWED_IMAGE_TYPES.contains(detectedMimeType)) {
            return CmsMediaType.IMAGE;
        }
        if (ALLOWED_VIDEO_TYPES.contains(detectedMimeType)) {
            return CmsMediaType.VIDEO;
        }
        return null;
    }

    private void requireExtensionConsistency(long attachmentId, String sourceUploadPath, String detectedMimeType) {
        String name = fileNameOf(sourceUploadPath).toLowerCase(Locale.ROOT);
        boolean matches = switch (detectedMimeType) {
            case "image/jpeg" -> name.endsWith(".jpg") || name.endsWith(".jpeg");
            case "image/png" -> name.endsWith(".png");
            case "image/webp" -> name.endsWith(".webp");
            case "video/mp4" -> name.endsWith(".mp4");
            default -> false;
        };
        if (!matches) {
            String extension = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "(none)";
            throw WordPressMediaImportException.extensionMimeMismatch(attachmentId, extension, detectedMimeType);
        }
    }

    private String fileNameOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash == -1 ? path : path.substring(slash + 1);
    }

    private String safeOriginalFilename(String sourceUploadPath, String contentType) {
        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "video/mp4" -> "mp4";
            default -> "bin";
        };
        return WordPressMediaObjectKeyFactory.sanitizeFilename(fileNameOf(sourceUploadPath)) + "." + extension;
    }

    private String configuredBucket() {
        String bucket = cmsMediaProperties.getBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new WordPressMediaImportException("MEDIA_BUCKET_NOT_CONFIGURED",
                    "app.cms.media.bucket is not configured.");
        }
        return bucket.trim();
    }
}
