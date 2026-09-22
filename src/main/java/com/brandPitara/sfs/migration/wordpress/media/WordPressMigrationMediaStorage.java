package com.brandPitara.sfs.migration.wordpress.media;

import java.nio.file.Path;

/**
 * Narrow storage port for the WordPress media migration - deliberately smaller than
 * {@link com.brandPitara.sfs.media.service.MediaObjectStorageService} (no presigned URLs; this is
 * a server-side, non-interactive path). The production implementation delegates to the
 * application's existing S3 client/config; the test implementation stores real bytes in memory.
 */
public interface WordPressMigrationMediaStorage {

    /**
     * Writes {@code localFile}'s bytes to {@code bucket}/{@code key}, refusing to overwrite an
     * existing object at that key with different content (content-addressed keys make a genuine
     * collision here effectively impossible, but the guarantee is enforced regardless via a
     * conditional write). Never call this without having already verified {@code key} either
     * does not exist or already matches these exact bytes (see {@link #head}).
     */
    void store(String bucket, String key, Path localFile, String contentType);

    /** @throws WordPressMediaStorageObjectNotFoundException if no object exists at that key. */
    WordPressStoredObjectMetadata head(String bucket, String key);

    boolean exists(String bucket, String key);

    /**
     * Deletes an object this operation itself just created and that is not (yet) referenced by
     * any durable mapping row - never a pre-existing or checksum-reused object.
     */
    void deleteOrphan(String bucket, String key);
}
