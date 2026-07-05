package com.brandPitara.sfs.media.service;

import java.util.Map;

/**
 * App-owned request for {@link MediaStorageService#createPresignedUpload}.
 * Callers own their own storage-key naming and content-type validation; this
 * only carries what the storage adapter actually needs to presign a PUT.
 */
public record PresignedUploadRequest(
        String storageKey,
        String contentType,
        Map<String, String> additionalHeaders
) {
    public PresignedUploadRequest(String storageKey, String contentType) {
        this(storageKey, contentType, Map.of());
    }
}
