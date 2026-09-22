package com.brandPitara.sfs.migration.wordpress.media;

/**
 * Thrown when a WordPress attachment cannot be safely imported into CMS media - never caught to
 * silently fabricate, overwrite, or leave a partially-verified result. Each factory names a
 * distinct, safe (no raw bytes, no credentials, no full stack of an unrelated cause) failure
 * reason, mirrored into the mapping row's {@code error_code} column by the caller.
 */
public class WordPressMediaImportException extends RuntimeException {

    private final String errorCode;

    public WordPressMediaImportException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WordPressMediaImportException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }

    public static WordPressMediaImportException fingerprintMismatch(
            long attachmentId, String previousSha256, String currentSha256
    ) {
        return new WordPressMediaImportException("SOURCE_FINGERPRINT_CHANGED",
                "WordPress attachment " + attachmentId + " has a different checksum than its last "
                        + "completed import (" + previousSha256 + " -> " + currentSha256 + "). Refusing to "
                        + "silently replace a completed media mapping - use an explicit repair mode.");
    }

    public static WordPressMediaImportException storedObjectMissing(long attachmentId, String bucket, String key) {
        return new WordPressMediaImportException("STORED_OBJECT_MISSING",
                "WordPress attachment " + attachmentId + " has a COMPLETED mapping pointing at "
                        + bucket + "/" + key + ", but no object exists there any more. Refusing to "
                        + "silently re-upload over an inconsistent record - run the explicit repair path.");
    }

    public static WordPressMediaImportException missingZipEntry(long attachmentId, String path) {
        return new WordPressMediaImportException("ZIP_ENTRY_MISSING",
                "WordPress attachment " + attachmentId + " references upload path '" + path
                        + "', which is not present in the media archive.");
    }

    public static WordPressMediaImportException ambiguousZipEntry(long attachmentId, String path) {
        return new WordPressMediaImportException("ZIP_ENTRY_AMBIGUOUS",
                "WordPress attachment " + attachmentId + "'s upload path '" + path
                        + "' matches more than one archive entry (duplicate name or case-insensitive collision).");
    }

    public static WordPressMediaImportException disallowedMimeType(long attachmentId, String detectedMimeType) {
        return new WordPressMediaImportException("DISALLOWED_MIME_TYPE",
                "WordPress attachment " + attachmentId + " was sniffed as '" + detectedMimeType
                        + "', which the CMS does not accept for public media.");
    }

    public static WordPressMediaImportException extensionMimeMismatch(
            long attachmentId, String extension, String detectedMimeType
    ) {
        return new WordPressMediaImportException("EXTENSION_MIME_MISMATCH",
                "WordPress attachment " + attachmentId + "'s file extension '" + extension
                        + "' is not consistent with its detected content type '" + detectedMimeType + "'.");
    }

    public static WordPressMediaImportException sizeLimitExceeded(long attachmentId, long sizeBytes, long limitBytes) {
        return new WordPressMediaImportException("SIZE_LIMIT_EXCEEDED",
                "WordPress attachment " + attachmentId + " is " + sizeBytes + " bytes, exceeding the "
                        + "CMS limit of " + limitBytes + " bytes for its media type.");
    }

    public static WordPressMediaImportException corruptMedia(long attachmentId, String detectedMimeType) {
        return new WordPressMediaImportException("CORRUPT_MEDIA",
                "WordPress attachment " + attachmentId + " could not be decoded as valid "
                        + detectedMimeType + " - it appears corrupt or truncated.");
    }

    public static WordPressMediaImportException verificationFailed(long attachmentId, String reason) {
        return new WordPressMediaImportException("STORED_OBJECT_VERIFICATION_FAILED",
                "WordPress attachment " + attachmentId + "'s stored object failed post-upload "
                        + "verification: " + reason);
    }

    public static WordPressMediaImportException checksumMismatch(long attachmentId) {
        return new WordPressMediaImportException("CHECKSUM_MISMATCH",
                "WordPress attachment " + attachmentId + "'s stored object checksum does not match "
                        + "the source bytes.");
    }

    public static WordPressMediaImportException existingObjectDifferentBytes(long attachmentId, String key) {
        return new WordPressMediaImportException("OBJECT_KEY_COLLISION",
                "WordPress attachment " + attachmentId + " would collide with an existing object at "
                        + key + " that has a different size/checksum. Refusing to overwrite it.");
    }

    public static WordPressMediaImportException uploadFailed(long attachmentId, Throwable cause) {
        return new WordPressMediaImportException("UPLOAD_FAILED",
                "WordPress attachment " + attachmentId + " could not be uploaded to storage.", cause);
    }

    public static WordPressMediaImportException databaseFinalizationFailed(long attachmentId, Throwable cause) {
        return new WordPressMediaImportException("DB_FINALIZATION_FAILED",
                "WordPress attachment " + attachmentId + " was uploaded but the database record could "
                        + "not be finalized.", cause);
    }
}
