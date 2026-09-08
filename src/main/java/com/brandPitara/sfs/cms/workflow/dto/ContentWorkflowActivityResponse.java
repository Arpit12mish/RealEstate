package com.brandPitara.sfs.cms.workflow.dto;

import com.brandPitara.sfs.cms.workflow.domain.ContentWorkflowAction;

import java.time.OffsetDateTime;

public record ContentWorkflowActivityResponse(
        Long id,
        Long revisionId,
        Integer revisionNumber,
        ContentWorkflowAction action,
        String comment,
        Long actorDashboardUserId,
        String actorDisplayName,
        OffsetDateTime createdAt
) {
}
