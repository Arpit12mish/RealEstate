package com.brandPitara.sfs.publiccontent.service;

import com.brandPitara.sfs.publiccontent.dto.PublicContentDetailResponse;

public record PublicContentResult(
        String etag,
        boolean notModified,
        PublicContentDetailResponse body
) {
}
