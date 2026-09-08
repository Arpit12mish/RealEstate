package com.brandPitara.sfs.publiccontent.dto;

import java.util.List;

public record PublicContentPageResponse(
        List<PublicContentListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
}
