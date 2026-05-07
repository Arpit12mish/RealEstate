package com.brandPitara.sfs.dashboard.review.dto;

import com.brandPitara.sfs.dashboard.common.enums.FieldReviewStatus;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.review.entity.DashboardFieldReviewIssueEntity;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldReviewIssueResponse {

    private Long id;

    private ReviewEntityType entityType;
    private Long entityId;

    private String fieldKey;
    private String fieldLabel;

    private FieldReviewStatus status;
    private String remarks;

    private Long markedById;
    private String markedByName;
    private String markedByEmail;

    private Long fixedById;
    private String fixedByName;
    private String fixedByEmail;

    private OffsetDateTime markedAt;
    private OffsetDateTime fixedAt;

    private Boolean active;

    public static FieldReviewIssueResponse from(DashboardFieldReviewIssueEntity entity) {
        if (entity == null) {
            return null;
        }

        return FieldReviewIssueResponse.builder()
                .id(entity.getId())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .fieldKey(entity.getFieldKey())
                .fieldLabel(entity.getFieldLabel())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .markedById(entity.getMarkedBy() != null ? entity.getMarkedBy().getId() : null)
                .markedByName(entity.getMarkedBy() != null ? entity.getMarkedBy().getName() : null)
                .markedByEmail(entity.getMarkedBy() != null ? entity.getMarkedBy().getEmail() : null)
                .fixedById(entity.getFixedBy() != null ? entity.getFixedBy().getId() : null)
                .fixedByName(entity.getFixedBy() != null ? entity.getFixedBy().getName() : null)
                .fixedByEmail(entity.getFixedBy() != null ? entity.getFixedBy().getEmail() : null)
                .markedAt(entity.getMarkedAt())
                .fixedAt(entity.getFixedAt())
                .active(entity.getActive())
                .build();
    }
}