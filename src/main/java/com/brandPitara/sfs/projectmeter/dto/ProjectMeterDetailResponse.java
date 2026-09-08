package com.brandPitara.sfs.projectmeter.dto;

import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilitySummaryResponse;
import com.brandPitara.sfs.project.dto.ProjectConnectivityResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanGroupResponse;
import com.brandPitara.sfs.project.dto.ProjectMasterPlanResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMeterDetailResponse {
    private ProjectMeterProjectResponse project;
    private ProjectMeterMediaResponse media;
    private ProjectMeterSummaryResponse summary;
    private ProjectConstructionProgressResponse construction;
    private ProjectLandLicenseResponse landLicense;
    private ProjectApprovalsResponse approvals;
    private List<ProjectComplianceGroupResponse> complianceGroups;
    private ProjectPriceInsightsResponse priceInsights;
    private List<ProjectPriceHistoryPointResponse> propertyRates;
    private List<ProjectPaymentMilestoneResponse> paymentPlan;
    private ProjectCostBreakdownResponse estimatedCost;
    private ProjectLandUtilizationResponse landUtilization;
    private ProjectLocationRadarResponse locationRadar;
    private ProjectAmenitiesResponse amenities;

    private BuilderCredibilitySummaryResponse builderCredibility;
    private ProjectMasterPlanResponse masterPlan;
    private List<ProjectFloorPlanGroupResponse> floorPlanGroups;
    private ProjectConnectivityResponse connectivity;
}
