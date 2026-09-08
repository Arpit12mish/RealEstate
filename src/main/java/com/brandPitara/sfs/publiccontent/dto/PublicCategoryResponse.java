package com.brandPitara.sfs.publiccontent.dto;

public record PublicCategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        long publishedContentCount
) {
}
