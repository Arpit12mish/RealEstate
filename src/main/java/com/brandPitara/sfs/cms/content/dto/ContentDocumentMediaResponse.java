package com.brandPitara.sfs.cms.content.dto;

public record ContentDocumentMediaResponse(
        Long id,
        String mediaType,
        String filename,
        String contentType,
        Long sizeBytes,
        Integer width,
        Integer height,
        Long durationMillis,
        String previewUrl,
        Integer previewExpiresInSeconds
) {
}
