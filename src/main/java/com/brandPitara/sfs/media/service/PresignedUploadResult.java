package com.brandPitara.sfs.media.service;

import java.util.Map;

/**
 * Result of a presigned upload request. {@code requiredHeaders} echoes back
 * whatever headers the caller asked to be included in
 * {@link PresignedUploadRequest#additionalHeaders()} (e.g. Content-Type,
 * Cache-Control) so it can be returned to the client unchanged.
 */
public record PresignedUploadResult(
        String uploadUrl,
        String publicUrl,
        String storageKey,
        int expiresInSeconds,
        Map<String, String> requiredHeaders
) {}
