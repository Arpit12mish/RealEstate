package com.brandPitara.sfs.publiccontent.repository;

import com.brandPitara.sfs.cms.content.document.ContentDocument;
import com.brandPitara.sfs.cms.content.domain.ContentType;

import java.time.OffsetDateTime;

public interface PublicContentDetailView {
    Long getPostId();
    Long getRevisionId();
    ContentType getContentType();
    String getTitle();
    String getSlug();
    String getExcerpt();
    Integer getReadingTimeMinutes();
    String getSeoTitle();
    String getSeoDescription();
    String getCanonicalUrl();
    Boolean getRobotsIndex();
    Boolean getRobotsFollow();
    ContentDocument getContentDocument();
    Short getContentDocumentSchemaVersion();
    OffsetDateTime getRevisionCreatedAt();
    OffsetDateTime getPublishedAt();
    Long getPublicAuthorId();
    String getPublicAuthorName();
    String getPublicAuthorSlug();
    String getPublicAuthorDesignation();
    Long getPublicAuthorProfileMediaAssetId();
    Long getCategoryId();
    String getCategoryName();
    String getCategorySlug();
    java.util.List<com.brandPitara.sfs.cms.workflow.domain.ContentTagSnapshot> getTagSnapshots();
    Long getCoverMediaAssetId();
    String getCoverAltText();
}
