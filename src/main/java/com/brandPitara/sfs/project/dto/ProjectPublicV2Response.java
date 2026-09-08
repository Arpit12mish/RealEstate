package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import com.brandPitara.sfs.projectmeter.dto.ProjectAmenitiesResponse;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Deterministic public Project Detail response. Viewer membership is intentionally absent. */
@Value
@Builder
public class ProjectPublicV2Response {
    Long id;
    Long builderId;
    String builderName;
    String builderLogoUrl;
    String name;
    String slug;
    String description;
    Long cityId;
    String cityName;
    String addressLine;
    Double latitude;
    Double longitude;
    Long priceMin;
    Long priceMax;
    Long monthlyEmiMin;
    Long monthlyEmiMax;
    Long averagePricePerSqft;
    LocalDate startDate;
    LocalDate possessionDate;
    String reraNumber;
    ProjectStatus status;
    Set<PropertyType> propertyTypes;
    String coverMediaUrl;
    String coverMediaType;
    String brochureUrl;
    boolean hasVideo;
    boolean hasImages;
    Long favoriteCount;
    ProjectPricingSummaryResponse pricing;
    ProjectLocationResponse location;
    List<ProjectFloorPlanGroupResponse> floorPlanGroups;
    ProjectConnectivityResponse connectivity;
    List<ProjectMediaResponse> glimpses;
    ProjectAmenitiesResponse amenities;
    ProjectMasterPlanResponse masterPlan;

    public LocalDate getProjectStartDate() {
        return startDate;
    }
}
