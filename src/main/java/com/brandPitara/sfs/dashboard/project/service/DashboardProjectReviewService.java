package com.brandPitara.sfs.dashboard.project.service;

import com.brandPitara.sfs.dashboard.project.dto.DashboardProjectReviewResponse;
import com.brandPitara.sfs.dashboard.project.dto.ProjectReviewDecisionRequest;
import com.brandPitara.sfs.dashboard.project.dto.ProjectSubmitReviewRequest;

public interface DashboardProjectReviewService {

    DashboardProjectReviewResponse getReviewStatus(Long projectId);

    DashboardProjectReviewResponse submitForReview(
            Long projectId,
            ProjectSubmitReviewRequest request
    );

    DashboardProjectReviewResponse approve(
            Long projectId,
            ProjectReviewDecisionRequest request
    );

    DashboardProjectReviewResponse rollbackApproval(
            Long projectId,
            ProjectReviewDecisionRequest request
    );

    DashboardProjectReviewResponse reject(
            Long projectId,
            ProjectReviewDecisionRequest request
    );

    DashboardProjectReviewResponse reopen(
            Long projectId,
            ProjectReviewDecisionRequest request
    );
}
