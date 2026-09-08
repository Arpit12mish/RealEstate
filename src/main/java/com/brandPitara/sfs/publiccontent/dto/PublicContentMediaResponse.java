package com.brandPitara.sfs.publiccontent.dto;

import com.brandPitara.sfs.cms.media.domain.CmsMediaType;

public record PublicContentMediaResponse(
        Long id,
        CmsMediaType mediaType,
        String contentType,
        Long sizeBytes,
        Integer width,
        Integer height,
        Long durationMillis,
        String deliveryUrl
) {
}
