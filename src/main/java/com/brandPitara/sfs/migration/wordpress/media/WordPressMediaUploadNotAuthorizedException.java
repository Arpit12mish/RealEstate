package com.brandPitara.sfs.migration.wordpress.media;

/**
 * Thrown by the production storage implementation when a real S3 write is attempted without
 * every explicit gate in {@link WordPressMigrationMediaProperties} set. This is the mechanism
 * that keeps a default application startup from ever uploading anything.
 */
public class WordPressMediaUploadNotAuthorizedException extends RuntimeException {
    public WordPressMediaUploadNotAuthorizedException() {
        super("WordPress media upload is not authorized: migration must be enabled, dry-run must be "
                + "off, upload must be explicitly enabled, the expected bucket must match the "
                + "configured bucket, and a production confirmation token must be set.");
    }
}
