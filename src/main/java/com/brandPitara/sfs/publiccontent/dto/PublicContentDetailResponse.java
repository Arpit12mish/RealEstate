package com.brandPitara.sfs.publiccontent.dto;

import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.domain.ContentType;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;

public record PublicContentDetailResponse(
        Long id,
        String slug,
        ContentType contentType,
        String title,
        String excerpt,
        Integer readingTimeMinutes,
        PublicAuthorSummaryResponse author,
        PublicCategorySummaryResponse category,
        List<PublicTagSummaryResponse> tags,
        PublicContentCoverResponse cover,
        PublicContentSeoResponse seo,
        ContentDocument document,
        OffsetDateTime publishedAt,
        OffsetDateTime revisionCreatedAt,
        Map<Long, PublicContentMediaResponse> media
) {
    public PublicContentDetailResponse(Long id,String slug,ContentType contentType,String title,String excerpt,
            PublicContentSeoResponse seo,ContentDocument document,OffsetDateTime publishedAt,
            OffsetDateTime revisionCreatedAt,Map<Long,PublicContentMediaResponse> media) {
        this(id,slug,contentType,title,excerpt,null,null,null,List.of(),null,seo,document,publishedAt,revisionCreatedAt,media);
    }
}
