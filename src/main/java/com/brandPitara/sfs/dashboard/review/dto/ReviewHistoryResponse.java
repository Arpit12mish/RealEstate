package com.brandPitara.sfs.dashboard.review.dto;

import com.brandPitara.sfs.dashboard.common.enums.ReviewActionType;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dashboard.review.entity.DashboardContentReviewHistoryEntity;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewHistoryResponse {

    private Long id;

    private ReviewEntityType entityType;
    private Long entityId;

    private ReviewStatus oldStatus;
    private ReviewStatus newStatus;

    private ReviewActionType actionType;
    private String remarks;

    private Long actionById;
    private String actionByName;
    private String actionByEmail;

    private OffsetDateTime createdAt;

    public static ReviewHistoryResponse from(DashboardContentReviewHistoryEntity entity) {
        if (entity == null) {
            return null;
        }

        return ReviewHistoryResponse.builder()
                .id(entity.getId())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .oldStatus(entity.getOldStatus())
                .newStatus(entity.getNewStatus())
                .actionType(entity.getActionType())
                .remarks(entity.getRemarks())
                .actionById(entity.getActionBy() != null ? entity.getActionBy().getId() : null)
                .actionByName(entity.getActionBy() != null ? entity.getActionBy().getName() : null)
                .actionByEmail(entity.getActionBy() != null ? entity.getActionBy().getEmail() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}