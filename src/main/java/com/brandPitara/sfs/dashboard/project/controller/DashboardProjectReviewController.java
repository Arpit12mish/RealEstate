package com.brandPitara.sfs.dashboard.project.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.project.dto.DashboardProjectReviewResponse;
import com.brandPitara.sfs.dashboard.project.dto.ProjectReviewDecisionRequest;
import com.brandPitara.sfs.dashboard.project.dto.ProjectSubmitReviewRequest;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/projects")
public class DashboardProjectReviewController {

    private final DashboardProjectReviewService dashboardProjectReviewService;
    private final DashboardProjectOwnershipService dashboardProjectOwnershipService;
    private final DashboardActionAuditService dashboardActionAuditService;

    public DashboardProjectReviewController(
            DashboardProjectReviewService dashboardProjectReviewService,
            DashboardProjectOwnershipService dashboardProjectOwnershipService,
            DashboardActionAuditService dashboardActionAuditService
    ) {
        this.dashboardProjectReviewService = dashboardProjectReviewService;
        this.dashboardProjectOwnershipService = dashboardProjectOwnershipService;
        this.dashboardActionAuditService = dashboardActionAuditService;
    }

    @GetMapping("/{projectId}/review-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public DashboardProjectReviewResponse reviewStatus(@PathVariable Long projectId) {
        return dashboardProjectReviewService.getReviewStatus(projectId);
    }

    @PostMapping("/{projectId}/submit-review")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DashboardProjectReviewResponse submitReview(
            @PathVariable Long projectId,
            @RequestBody(required = false) ProjectSubmitReviewRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanSubmitProject(projectId);
        DashboardProjectReviewResponse response = dashboardProjectReviewService.submitForReview(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.PROJECT_SUBMITTED_FOR_REVIEW, ReviewEntityType.PROJECT, projectId, projectId);
        return response;
    }

    @PostMapping("/{projectId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public DashboardProjectReviewResponse approve(
            @PathVariable Long projectId,
            @RequestBody(required = false) ProjectReviewDecisionRequest request
    ) {
        DashboardProjectReviewResponse response = dashboardProjectReviewService.approve(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.PROJECT_APPROVED, ReviewEntityType.PROJECT, projectId, projectId);
        return response;
    }

    @PostMapping("/{projectId}/approval/rollback")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public DashboardProjectReviewResponse rollbackApproval(
            @PathVariable Long projectId,
            @RequestBody ProjectReviewDecisionRequest request
    ) {
        DashboardProjectReviewResponse response = dashboardProjectReviewService.rollbackApproval(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.PROJECT_APPROVAL_ROLLED_BACK, ReviewEntityType.PROJECT, projectId, projectId);
        return response;
    }

    @PostMapping("/{projectId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public DashboardProjectReviewResponse reject(
            @PathVariable Long projectId,
            @RequestBody ProjectReviewDecisionRequest request
    ) {
        DashboardProjectReviewResponse response = dashboardProjectReviewService.reject(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.PROJECT_REJECTED, ReviewEntityType.PROJECT, projectId, projectId);
        return response;
    }

    @PostMapping("/{projectId}/reopen")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public DashboardProjectReviewResponse reopen(
            @PathVariable Long projectId,
            @RequestBody(required = false) ProjectReviewDecisionRequest request
    ) {
        DashboardProjectReviewResponse response = dashboardProjectReviewService.reopen(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.PROJECT_REOPENED, ReviewEntityType.PROJECT, projectId, projectId);
        return response;
    }
}
