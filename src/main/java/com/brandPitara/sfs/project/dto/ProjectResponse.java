package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import com.brandPitara.sfs.projectmeter.dto.ProjectAmenitiesResponse;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {

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
  private ReviewStatus reviewStatus;
  private Set<PropertyType> propertyTypes;

  private boolean active;
  private boolean published;
  private int priority;

  private String coverMediaUrl;
  private String coverMediaType;
  private String brochureUrl;
  private boolean hasVideo;
  private boolean hasImages;

  private Long favoriteCount;
  private Boolean isFavorite;

  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  // --- aggregated sections for normal project screen ---
  private ProjectPricingSummaryResponse pricing;
  private ProjectLocationResponse location;
  private List<ProjectFloorPlanGroupResponse> floorPlanGroups;
  private ProjectConnectivityResponse connectivity;
  private List<ProjectMediaResponse> glimpses;
  private ProjectAmenitiesResponse amenities;

  public LocalDate getProjectStartDate() {
    return startDate;
  }
}
