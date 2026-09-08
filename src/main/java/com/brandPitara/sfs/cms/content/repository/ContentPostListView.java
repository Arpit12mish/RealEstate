package com.brandPitara.sfs.cms.content.repository;

import com.brandPitara.sfs.cms.content.domain.ContentStatus;
import com.brandPitara.sfs.cms.content.domain.ContentType;

import java.time.OffsetDateTime;

public interface ContentPostListView {
    Long getId();

    ContentType getContentType();

    ContentStatus getStatus();

    String getTitle();

    String getSlug();

    String getExcerpt();

    Long getContentOwnerDashboardUserId();

    String getContentOwnerDisplayName();

    Long getUpdatedByDashboardUserId();

    String getUpdatedByDisplayName();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getUpdatedAt();

    Long getVersion();
}
