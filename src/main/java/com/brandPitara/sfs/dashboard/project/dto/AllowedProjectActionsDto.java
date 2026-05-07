package com.brandPitara.sfs.dashboard.project.dto;

import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AllowedProjectActionsDto {

    private boolean canEdit;
    private boolean canSubmitForReview;
    private boolean canApprove;
    private boolean canReject;
    private boolean canDelete;
    private boolean canPublish;

    public static AllowedProjectActionsDto compute(
            DashboardProjectReviewResponse review,
            DashboardUserEntity user
    ) {
        if (user == null || review == null) {
            return AllowedProjectActionsDto.builder().build();
        }

        DashboardRole role = user.getRole();
        ReviewStatus status = review.getReviewStatus();

        boolean isAdmin = role == DashboardRole.ADMIN;
        boolean isReviewer = role == DashboardRole.REVIEWER;
        boolean isDataEntry = role == DashboardRole.DATA_ENTRY;

        boolean isOwner = isDataEntry
                && review.getCreatedByDashboardUserId() != null
                && review.getCreatedByDashboardUserId().equals(user.getId());

        boolean editableStatus = status == ReviewStatus.DRAFT
                || status == ReviewStatus.RECHECK
                || status == ReviewStatus.REJECTED;

        boolean canEdit = isAdmin || (isOwner && editableStatus);
        boolean canSubmit = isAdmin || (isOwner && editableStatus);
        boolean canApproveOrReject = (isAdmin || isReviewer) && status == ReviewStatus.PENDING_REVIEW;

        return AllowedProjectActionsDto.builder()
                .canEdit(canEdit)
                .canSubmitForReview(canSubmit)
                .canApprove(canApproveOrReject)
                .canReject(canApproveOrReject)
                .canDelete(isAdmin)
                .canPublish(isAdmin)
                .build();
    }
}
