package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import com.brandPitara.sfs.projectmeter.dto.ProjectAmenitiesResponse;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectPublicResponse {

  private Long id;

  private Long builderId;
  private String builderName;
  private String builderLogoUrl;

  private String name;
  private String slug;
  private String description;

  private Long cityId;
  private String cityName;

  private String addressLine;
  private Double latitude;
  private Double longitude;

  private Long priceMin;
  private Long priceMax;

  private Long monthlyEmiMin;
  private Long monthlyEmiMax;
  private Long averagePricePerSqft;

  private LocalDate startDate;
  private LocalDate possessionDate;
  private String reraNumber;

  private ProjectStatus status;
  private Set<PropertyType> propertyTypes;

  private String coverMediaUrl;
  private String coverMediaType;
  private String brochureUrl;
  private boolean hasVideo;
  private boolean hasImages;

  private Long favoriteCount;
  private Boolean isFavorite;

  private ProjectPricingSummaryResponse pricing;
  private ProjectLocationResponse location;
  private List<ProjectFloorPlanGroupResponse> floorPlanGroups;
  private ProjectConnectivityResponse connectivity;
  private List<ProjectMediaResponse> glimpses;
  private ProjectAmenitiesResponse amenities;
  private ProjectMasterPlanResponse masterPlan;

  public LocalDate getProjectStartDate() {
    return startDate;
  }
}
