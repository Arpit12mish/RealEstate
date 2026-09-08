package com.brandPitara.sfs.media.service;

public record StoredObjectMetadata(
        long contentLength,
        String contentType,
        String eTag
) {
}
