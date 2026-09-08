package com.brandPitara.sfs.cms.workflow.dto;

import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.cms.workflow.domain.ContentRevisionReason;

import java.time.OffsetDateTime;

public record ContentRevisionListResponse(
        Long id,
        int revisionNumber,
        ContentType contentType,
        String title,
        String slug,
        long createdFromPostVersion,
        ContentRevisionReason revisionReason,
        Long createdByDashboardUserId,
        String createdByDisplayName,
        OffsetDateTime createdAt
) {
}
