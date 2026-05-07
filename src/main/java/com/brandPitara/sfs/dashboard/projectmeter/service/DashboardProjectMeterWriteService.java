package com.brandPitara.sfs.dashboard.projectmeter.service;

import com.brandPitara.sfs.dashboard.projectmeter.dto.*;
import com.brandPitara.sfs.projectmeter.dto.*;

import java.util.List;

public interface DashboardProjectMeterWriteService {

    List<ProjectConstructionStageResponse> listConstructionStages(Long projectId);
    ProjectConstructionStageResponse createConstructionStage(Long projectId, DashboardProjectConstructionStageRequest request);
    ProjectConstructionStageResponse updateConstructionStage(Long projectId, Long stageId, DashboardProjectConstructionStageRequest request);
    void deleteConstructionStage(Long projectId, Long stageId);

    List<ProjectComplianceItemResponse> listComplianceItems(Long projectId);
    ProjectComplianceItemResponse createComplianceItem(Long projectId, DashboardProjectComplianceItemRequest request);
    ProjectComplianceItemResponse updateComplianceItem(Long projectId, Long itemId, DashboardProjectComplianceItemRequest request);
    void deleteComplianceItem(Long projectId, Long itemId);

    List<ProjectAmenityItemResponse> listAmenities(Long projectId);
    ProjectAmenityItemResponse createAmenity(Long projectId, DashboardProjectAmenityRequest request);
    ProjectAmenityItemResponse updateAmenity(Long projectId, Long amenityId, DashboardProjectAmenityRequest request);
    void deleteAmenity(Long projectId, Long amenityId);

    List<ProjectPriceHistoryPointResponse> listPriceHistory(Long projectId);
    ProjectPriceHistoryPointResponse createPriceHistory(Long projectId, DashboardProjectPriceHistoryRequest request);
    ProjectPriceHistoryPointResponse updatePriceHistory(Long projectId, Long priceHistoryId, DashboardProjectPriceHistoryRequest request);
    void deletePriceHistory(Long projectId, Long priceHistoryId);

    List<ProjectPaymentMilestoneResponse> listPaymentMilestones(Long projectId);
    ProjectPaymentMilestoneResponse createPaymentMilestone(Long projectId, DashboardProjectPaymentMilestoneRequest request);
    ProjectPaymentMilestoneResponse updatePaymentMilestone(Long projectId, Long milestoneId, DashboardProjectPaymentMilestoneRequest request);
    void deletePaymentMilestone(Long projectId, Long milestoneId);

    ProjectCostBreakdownResponse getCostBreakdown(Long projectId);
    ProjectCostBreakdownResponse upsertCostBreakdown(Long projectId, DashboardProjectCostBreakdownRequest request);

    ProjectLandUtilizationResponse getLandUtilization(Long projectId);
    ProjectLandUtilizationResponse upsertLandUtilization(Long projectId, DashboardProjectLandUtilizationRequest request);

    ProjectLocationRadarResponse getLocationScore(Long projectId);
    ProjectLocationRadarResponse upsertLocationScore(Long projectId, DashboardProjectLocationScoreRequest request);

    DashboardProjectMeterWriteResponse recalculateSnapshot(Long projectId);
}