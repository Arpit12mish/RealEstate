package com.brandPitara.sfs.dashboard.projectmeter.controller;

import com.brandPitara.sfs.dashboard.audit.service.DashboardActionAuditService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import com.brandPitara.sfs.dashboard.project.service.DashboardProjectOwnershipService;
import com.brandPitara.sfs.dashboard.projectmeter.dto.*;
import com.brandPitara.sfs.dashboard.projectmeter.service.DashboardProjectMeterWriteService;
import com.brandPitara.sfs.projectmeter.dto.*;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/projects/{projectId}/meter")
@RequiredArgsConstructor
public class DashboardProjectMeterController {

    private final DashboardProjectMeterWriteService service;
    private final ProjectMeterService projectMeterService;
    private final DashboardProjectOwnershipService dashboardProjectOwnershipService;
    private final DashboardActionAuditService dashboardActionAuditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public ProjectMeterDetailResponse getDashboardMeterDetail(@PathVariable Long projectId) {
        return projectMeterService.dashboardGetMeterDetail(projectId);
    }

    @GetMapping("/construction-stages")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<ProjectConstructionStageResponse> listStages(@PathVariable Long projectId) {
        return service.listConstructionStages(projectId);
    }

    @PostMapping("/construction-stages")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectConstructionStageResponse createStage(
            @PathVariable Long projectId,
            @Valid @RequestBody DashboardProjectConstructionStageRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectConstructionStageResponse response = service.createConstructionStage(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.CONSTRUCTION_STAGE_CREATED, ReviewEntityType.PROJECT_METER, response.getId(), projectId);
        return response;
    }

    @PutMapping("/construction-stages/{stageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectConstructionStageResponse updateStage(
            @PathVariable Long projectId,
            @PathVariable Long stageId,
            @Valid @RequestBody DashboardProjectConstructionStageRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectConstructionStageResponse response = service.updateConstructionStage(projectId, stageId, request);
        dashboardActionAuditService.record(DashboardAuditAction.CONSTRUCTION_STAGE_UPDATED, ReviewEntityType.PROJECT_METER, stageId, projectId);
        return response;
    }

    @DeleteMapping("/construction-stages/{stageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteStage(@PathVariable Long projectId, @PathVariable Long stageId) {
        service.deleteConstructionStage(projectId, stageId);
        dashboardActionAuditService.record(DashboardAuditAction.CONSTRUCTION_STAGE_DELETED, ReviewEntityType.PROJECT_METER, stageId, projectId);
    }

    @GetMapping("/compliance-items")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<ProjectComplianceItemResponse> listCompliance(@PathVariable Long projectId) {
        return service.listComplianceItems(projectId);
    }

    @PostMapping("/compliance-items")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectComplianceItemResponse createCompliance(
            @PathVariable Long projectId,
            @Valid @RequestBody DashboardProjectComplianceItemRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectComplianceItemResponse response = service.createComplianceItem(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.COMPLIANCE_ITEM_CREATED, ReviewEntityType.PROJECT_METER, response.getId(), projectId);
        return response;
    }

    @PutMapping("/compliance-items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectComplianceItemResponse updateCompliance(
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @Valid @RequestBody DashboardProjectComplianceItemRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectComplianceItemResponse response = service.updateComplianceItem(projectId, itemId, request);
        dashboardActionAuditService.record(DashboardAuditAction.COMPLIANCE_ITEM_UPDATED, ReviewEntityType.PROJECT_METER, itemId, projectId);
        return response;
    }

    @DeleteMapping("/compliance-items/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCompliance(@PathVariable Long projectId, @PathVariable Long itemId) {
        service.deleteComplianceItem(projectId, itemId);
        dashboardActionAuditService.record(DashboardAuditAction.COMPLIANCE_ITEM_DELETED, ReviewEntityType.PROJECT_METER, itemId, projectId);
    }

    @GetMapping("/amenities")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<ProjectAmenityItemResponse> listAmenities(@PathVariable Long projectId) {
        return service.listAmenities(projectId);
    }

    @PostMapping("/amenities")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectAmenityItemResponse createAmenity(
            @PathVariable Long projectId,
            @Valid @RequestBody DashboardProjectAmenityRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectAmenityItemResponse response = service.createAmenity(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.AMENITY_CREATED, ReviewEntityType.PROJECT_METER, response.getId(), projectId);
        return response;
    }

    @PutMapping("/amenities/{amenityId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectAmenityItemResponse updateAmenity(
            @PathVariable Long projectId,
            @PathVariable Long amenityId,
            @Valid @RequestBody DashboardProjectAmenityRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectAmenityItemResponse response = service.updateAmenity(projectId, amenityId, request);
        dashboardActionAuditService.record(DashboardAuditAction.AMENITY_UPDATED, ReviewEntityType.PROJECT_METER, amenityId, projectId);
        return response;
    }

    @DeleteMapping("/amenities/{amenityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteAmenity(@PathVariable Long projectId, @PathVariable Long amenityId) {
        service.deleteAmenity(projectId, amenityId);
        dashboardActionAuditService.record(DashboardAuditAction.AMENITY_DELETED, ReviewEntityType.PROJECT_METER, amenityId, projectId);
    }

    @GetMapping("/price-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<ProjectPriceHistoryPointResponse> listPriceHistory(@PathVariable Long projectId) {
        return service.listPriceHistory(projectId);
    }

    @PostMapping("/price-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectPriceHistoryPointResponse createPriceHistory(
            @PathVariable Long projectId,
            @Valid @RequestBody DashboardProjectPriceHistoryRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);

        ProjectPriceHistoryPointResponse response =
                service.createPriceHistory(projectId, request);

        dashboardActionAuditService.record(
                DashboardAuditAction.PRICE_HISTORY_CREATED,
                ReviewEntityType.PROJECT_METER,
                projectId,
                projectId
        );

        return response;
    }

    @PutMapping("/price-history/{priceHistoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectPriceHistoryPointResponse updatePriceHistory(
            @PathVariable Long projectId,
            @PathVariable Long priceHistoryId,
            @Valid @RequestBody DashboardProjectPriceHistoryRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectPriceHistoryPointResponse response = service.updatePriceHistory(projectId, priceHistoryId, request);
        dashboardActionAuditService.record(DashboardAuditAction.PRICE_HISTORY_UPDATED, ReviewEntityType.PROJECT_METER, priceHistoryId, projectId);
        return response;
    }

    @DeleteMapping("/price-history/{priceHistoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePriceHistory(@PathVariable Long projectId, @PathVariable Long priceHistoryId) {
        service.deletePriceHistory(projectId, priceHistoryId);
        dashboardActionAuditService.record(DashboardAuditAction.PRICE_HISTORY_DELETED, ReviewEntityType.PROJECT_METER, priceHistoryId, projectId);
    }

    @GetMapping("/payment-milestones")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public List<ProjectPaymentMilestoneResponse> listMilestones(@PathVariable Long projectId) {
        return service.listPaymentMilestones(projectId);
    }

    @PostMapping("/payment-milestones")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectPaymentMilestoneResponse createMilestone(
            @PathVariable Long projectId,
            @Valid @RequestBody DashboardProjectPaymentMilestoneRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectPaymentMilestoneResponse response = service.createPaymentMilestone(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.PAYMENT_MILESTONE_CREATED, ReviewEntityType.PROJECT_METER, response.getId(), projectId);
        return response;
    }

    @PutMapping("/payment-milestones/{milestoneId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectPaymentMilestoneResponse updateMilestone(
            @PathVariable Long projectId,
            @PathVariable Long milestoneId,
            @Valid @RequestBody DashboardProjectPaymentMilestoneRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectPaymentMilestoneResponse response = service.updatePaymentMilestone(projectId, milestoneId, request);
        dashboardActionAuditService.record(DashboardAuditAction.PAYMENT_MILESTONE_UPDATED, ReviewEntityType.PROJECT_METER, milestoneId, projectId);
        return response;
    }

    @DeleteMapping("/payment-milestones/{milestoneId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteMilestone(@PathVariable Long projectId, @PathVariable Long milestoneId) {
        service.deletePaymentMilestone(projectId, milestoneId);
        dashboardActionAuditService.record(DashboardAuditAction.PAYMENT_MILESTONE_DELETED, ReviewEntityType.PROJECT_METER, milestoneId, projectId);
    }

    @GetMapping("/cost-breakdown")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public ProjectCostBreakdownResponse getCostBreakdown(@PathVariable Long projectId) {
        return service.getCostBreakdown(projectId);
    }

    @PutMapping("/cost-breakdown")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectCostBreakdownResponse upsertCostBreakdown(
            @PathVariable Long projectId,
            @RequestBody DashboardProjectCostBreakdownRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectCostBreakdownResponse response = service.upsertCostBreakdown(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.COST_BREAKDOWN_UPSERTED, ReviewEntityType.PROJECT_METER, projectId, projectId);
        return response;
    }

    @GetMapping("/land-utilization")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public ProjectLandUtilizationResponse getLandUtilization(@PathVariable Long projectId) {
        return service.getLandUtilization(projectId);
    }

    @PutMapping("/land-utilization")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectLandUtilizationResponse upsertLandUtilization(
            @PathVariable Long projectId,
            @RequestBody DashboardProjectLandUtilizationRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectLandUtilizationResponse response = service.upsertLandUtilization(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.LAND_UTILIZATION_UPSERTED, ReviewEntityType.PROJECT_METER, projectId, projectId);
        return response;
    }

    @GetMapping("/location-score")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public ProjectLocationRadarResponse getLocationScore(@PathVariable Long projectId) {
        return service.getLocationScore(projectId);
    }

    @PutMapping("/location-score")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public ProjectLocationRadarResponse upsertLocationScore(
            @PathVariable Long projectId,
            @RequestBody DashboardProjectLocationScoreRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        ProjectLocationRadarResponse response = service.upsertLocationScore(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.LOCATION_SCORE_UPSERTED, ReviewEntityType.PROJECT_METER, projectId, projectId);
        return response;
    }

    @PostMapping("/snapshot/recalculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public DashboardProjectMeterWriteResponse recalculateSnapshot(@PathVariable Long projectId) {
        DashboardProjectMeterWriteResponse response = service.recalculateSnapshot(projectId);
        dashboardActionAuditService.record(DashboardAuditAction.SNAPSHOT_RECALCULATED, ReviewEntityType.PROJECT_METER_SNAPSHOT, projectId, projectId);
        return response;
    }

    @PatchMapping("/snapshot/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public DashboardProjectMeterWriteResponse verifySnapshot(
            @PathVariable Long projectId,
            @Valid @RequestBody DashboardProjectMeterSnapshotVerifyRequest request
    ) {
        DashboardProjectMeterWriteResponse response = service.verifySnapshot(projectId, request);
        DashboardAuditAction action = Boolean.TRUE.equals(request.getVerified())
                ? DashboardAuditAction.SNAPSHOT_VERIFIED
                : DashboardAuditAction.SNAPSHOT_UNVERIFIED;
        dashboardActionAuditService.record(action, ReviewEntityType.PROJECT_METER_SNAPSHOT, projectId, projectId);
        return response;
    }

    @PatchMapping("/timeline")
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_ENTRY')")
    public DashboardProjectMeterWriteResponse updateTimeline(
            @PathVariable Long projectId,
            @Valid @RequestBody DashboardProjectTimelineRequest request
    ) {
        dashboardProjectOwnershipService.assertCurrentUserCanEditProject(projectId);
        DashboardProjectMeterWriteResponse response = service.updateTimeline(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.SNAPSHOT_RECALCULATED, ReviewEntityType.PROJECT_METER_SNAPSHOT, projectId, projectId);
        return response;
    }

    @PatchMapping("/price-insights")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public DashboardProjectMeterWriteResponse updatePriceInsights(
            @PathVariable Long projectId,
            @Valid @RequestBody DashboardProjectPriceInsightsRequest request
    ) {
        DashboardProjectMeterWriteResponse response = service.updatePriceInsights(projectId, request);
        dashboardActionAuditService.record(DashboardAuditAction.PRICE_INSIGHTS_UPDATED, ReviewEntityType.PROJECT_METER_SNAPSHOT, projectId, projectId);
        return response;
    }
}
