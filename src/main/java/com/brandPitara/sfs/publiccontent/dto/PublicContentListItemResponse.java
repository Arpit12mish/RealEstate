package com.brandPitara.sfs.publiccontent.dto;

import com.brandPitara.sfs.cms.content.domain.ContentType;

import java.time.OffsetDateTime;

public record PublicContentListItemResponse(
        Long id,
        String slug,
        ContentType contentType,
        String title,
        String excerpt,
        Integer readingTimeMinutes,
        PublicAuthorSummaryResponse author,
        PublicCategorySummaryResponse category,
        PublicContentCoverResponse cover,
        OffsetDateTime publishedAt
) {
    public PublicContentListItemResponse(Long id,String slug,ContentType contentType,String title,String excerpt,
                                         OffsetDateTime publishedAt) {
        this(id,slug,contentType,title,excerpt,null,null,null,null,publishedAt);
    }
}
