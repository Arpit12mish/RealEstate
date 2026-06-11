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
    private boolean canSubmitReview;
    private boolean canSubmitForReview;
    private boolean canApprove;
    private boolean canRollbackApproval;
    private boolean canReject;
    private boolean canReopen;
    private boolean canDelete;
    private boolean canPublish;
    private boolean canMarkFieldIssue;
    private boolean canMarkIssueFixed;
    private boolean canRecalculateSnapshot;

    public static AllowedProjectActionsDto compute(
            DashboardProjectReviewResponse review,
            DashboardUserEntity user
    ) {
        if (user == null || review == null) {
            return AllowedProjectActionsDto.builder().build();
        }

        DashboardRole role = user.getRole();
        ReviewStatus status = review.getReviewStatus() != null ? review.getReviewStatus() : ReviewStatus.DRAFT;

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
        // Admins can approve from any non-approved status; reviewers only from PENDING_REVIEW.
        boolean canApprove = isAdmin
                ? status != ReviewStatus.APPROVED
                : isReviewer && status == ReviewStatus.PENDING_REVIEW;
        boolean canRollbackApproval = (isAdmin || isReviewer) && status == ReviewStatus.APPROVED;
        boolean canReject = (isAdmin || isReviewer) && status == ReviewStatus.PENDING_REVIEW;
        boolean canReopen = (isAdmin || isReviewer) && status == ReviewStatus.REJECTED;
        boolean canPublish = isAdmin && (status == ReviewStatus.APPROVED || Boolean.TRUE.equals(review.getPublished()));
        boolean canMarkFieldIssue = (isAdmin || isReviewer) && status == ReviewStatus.PENDING_REVIEW;
        boolean canMarkIssueFixed = isAdmin || (isOwner && editableStatus);

        return AllowedProjectActionsDto.builder()
                .canEdit(canEdit)
                .canSubmitReview(canSubmit)
                .canSubmitForReview(canSubmit)
                .canApprove(canApprove)
                .canRollbackApproval(canRollbackApproval)
                .canReject(canReject)
                .canReopen(canReopen)
                .canDelete(isAdmin)
                .canPublish(canPublish)
                .canMarkFieldIssue(canMarkFieldIssue)
                .canMarkIssueFixed(canMarkIssueFixed)
                .canRecalculateSnapshot(canEdit)
                .build();
    }
}
