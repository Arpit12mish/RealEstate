package com.brandPitara.sfs.media.service;

public interface MediaObjectStorageService {

    PresignedUploadResult createPresignedUpload(
            String bucket, String storageKey, String contentType, long contentLength
    );

    /**
     * Creates a one-shot upload for an immutable object. Implementations must
     * bind the cache policy and reject replacement of an existing key.
     */
    PresignedUploadResult createImmutablePresignedUpload(
            String bucket,
            String storageKey,
            String contentType,
            long contentLength,
            String cacheControl
    );

    StoredObjectMetadata head(String bucket, String storageKey);

    byte[] readPrefix(String bucket, String storageKey, int maximumBytes);

    PresignedReadResult createPresignedRead(String bucket, String storageKey);

    void delete(String bucket, String storageKey);
}
