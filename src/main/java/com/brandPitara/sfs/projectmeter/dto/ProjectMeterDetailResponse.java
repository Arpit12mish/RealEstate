package com.brandPitara.sfs.projectmeter.dto;

import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilitySummaryResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMeterDetailResponse {
    private ProjectMeterSummaryResponse summary;
    private ProjectConstructionProgressResponse construction;
    private ProjectLandLicenseResponse landLicense;
    private ProjectApprovalsResponse approvals;
    private ProjectPriceInsightsResponse priceInsights;
    private List<ProjectPriceHistoryPointResponse> propertyRates;
    private List<ProjectPaymentMilestoneResponse> paymentPlan;
    private ProjectCostBreakdownResponse estimatedCost;
    private ProjectLandUtilizationResponse landUtilization;
    private ProjectLocationRadarResponse locationRadar;
    private ProjectAmenitiesResponse amenities;

    private BuilderCredibilitySummaryResponse builderCredibility;
}