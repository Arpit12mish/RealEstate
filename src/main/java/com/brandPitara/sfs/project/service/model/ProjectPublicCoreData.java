package com.brandPitara.sfs.project.service.model;

import com.brandPitara.sfs.project.dto.ProjectConnectivityResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanGroupResponse;
import com.brandPitara.sfs.project.dto.ProjectLocationResponse;
import com.brandPitara.sfs.project.dto.ProjectMasterPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectMediaResponse;
import com.brandPitara.sfs.project.dto.ProjectPricingSummaryResponse;
import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import com.brandPitara.sfs.projectmeter.dto.ProjectAmenitiesResponse;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Viewer-independent, detached Project Detail representation used as the single internal source
 * for the legacy public response and a future deterministic public API.
 */
@Value
@Builder(toBuilder = true)
public class ProjectPublicCoreData {
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
    Set<ProjectPublicSectionFailure> failedSections;
}
