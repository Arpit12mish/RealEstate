package com.brandPitara.sfs.cms.content.dto;

import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;

import java.time.OffsetDateTime;
import java.util.Map;

public record ContentDocumentResponse(
        Long contentId,
        Long version,
        OffsetDateTime updatedAt,
        ContentDocument document,
        int wordCount,
        Map<Long, ContentDocumentMediaResponse> media
) {
    public static ContentDocumentResponse from(
            ContentPostEntity post,
            int wordCount,
            Map<Long, ContentDocumentMediaResponse> media
    ) {
        return new ContentDocumentResponse(
                post.getId(),
                post.getVersion(),
                post.getUpdatedAt(),
                post.getContentDocument(),
                wordCount,
                media
        );
    }
}
