package com.brandPitara.sfs.dashboard.project.service.impl;

import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.project.dto.AllowedProjectActionsDto;
import com.brandPitara.sfs.dashboard.project.dto.DashboardProjectReviewResponse;
import com.brandPitara.sfs.dashboard.project.dto.DashboardProjectWorkspaceResponse;
import com.brandPitara.sfs.dashboard.project.dto.ProjectApprovalReadinessDto;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectReviewService;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectWorkspaceService;
import com.brandPitara.sfs.dashboard.review.service.DashboardFieldReviewIssueService;
import com.brandPitara.sfs.dashboard.review.service.DashboardReviewHistoryService;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.project.service.ProjectConnectivityService;
import com.brandPitara.sfs.project.service.ProjectFloorPlanService;
import com.brandPitara.sfs.project.service.ProjectHighlightService;
import com.brandPitara.sfs.project.service.ProjectMediaService;
import com.brandPitara.sfs.project.service.ProjectService;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterSummaryResponse;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardProjectWorkspaceServiceImpl implements DashboardProjectWorkspaceService {

    private final ProjectService projectService;
    private final DashboardProjectReviewService reviewService;
    private final ProjectMediaService mediaService;
    private final ProjectFloorPlanService floorPlanService;
    private final ProjectHighlightService highlightService;
    private final ProjectConnectivityService connectivityService;
    private final ProjectMeterService meterService;
    private final DashboardFieldReviewIssueService fieldIssueService;
    private final DashboardReviewHistoryService reviewHistoryService;
    private final DashboardCurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public DashboardProjectWorkspaceResponse getWorkspace(Long projectId) {
        DashboardProjectReviewResponse review = reviewService.getReviewStatus(projectId);
        DashboardUserEntity currentUser = currentUserService.getCurrentUserOrThrow();

        var project      = projectService.adminGet(projectId);
        var media        = mediaService.adminList(projectId);
        var floorPlans   = floorPlanService.adminList(projectId);
        var highlights   = highlightService.adminList(projectId);
        var connectivity = connectivityService.adminGet(projectId);
        var meterSummary = safeGetMeterSummary(projectId);
        var fieldIssues  = fieldIssueService.listIssues(ReviewEntityType.PROJECT, projectId, false);
        var history      = reviewHistoryService.listHistory(ReviewEntityType.PROJECT, projectId);

        return DashboardProjectWorkspaceResponse.builder()
                .project(project)
                .review(review)
                .allowedActions(AllowedProjectActionsDto.compute(review, currentUser))
                .media(media)
                .floorPlans(floorPlans)
                .highlights(highlights)
                .connectivity(connectivity)
                .meterSummary(meterSummary)
                .fieldIssues(fieldIssues)
                .reviewHistory(history)
                .approvalReadiness(ProjectApprovalReadinessDto.compute(
                        project, media, floorPlans, highlights, connectivity, meterSummary, fieldIssues))
                .build();
    }

    private ProjectMeterSummaryResponse safeGetMeterSummary(Long projectId) {
        try {
            return meterService.adminGetMeterSummary(projectId);
        } catch (Exception e) {
            log.debug("Meter summary unavailable for project {}: {}", projectId, e.getMessage());
            return null;
        }
    }
}
