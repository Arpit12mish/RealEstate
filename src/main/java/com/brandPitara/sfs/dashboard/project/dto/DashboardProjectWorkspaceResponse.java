package com.brandPitara.sfs.dashboard.project.dto;

import com.brandPitara.sfs.dashboard.review.dto.FieldReviewIssueResponse;
import com.brandPitara.sfs.dashboard.review.dto.ReviewHistoryResponse;
import com.brandPitara.sfs.project.dto.*;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterSummaryResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardProjectWorkspaceResponse {

    private ProjectResponse project;
    private DashboardProjectReviewResponse review;
    private AllowedProjectActionsDto allowedActions;
    private List<ProjectMediaResponse> media;
    private List<ProjectFloorPlanResponse> floorPlans;
    private List<ProjectHighlightResponse> highlights;
    private ProjectConnectivityResponse connectivity;
    private ProjectMasterPlanResponse masterPlan;
    private ProjectMeterSummaryResponse meterSummary;
    private List<FieldReviewIssueResponse> fieldIssues;
    private List<ReviewHistoryResponse> reviewHistory;
    private ProjectApprovalReadinessDto approvalReadiness;
}
