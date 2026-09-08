package com.brandPitara.sfs.publiccontent.repository;

import com.brandPitara.sfs.cms.content.domain.ContentType;

import java.time.OffsetDateTime;

public interface PublicContentListView {
    Long getPostId();
    ContentType getContentType();
    String getTitle();
    String getSlug();
    String getExcerpt();
    Integer getReadingTimeMinutes();
    OffsetDateTime getPublishedAt();
    Long getPublicAuthorId();
    String getPublicAuthorName();
    String getPublicAuthorSlug();
    String getPublicAuthorDesignation();
    Long getCategoryId();
    String getCategoryName();
    String getCategorySlug();
    Long getCoverMediaAssetId();
    String getCoverAltText();
}
