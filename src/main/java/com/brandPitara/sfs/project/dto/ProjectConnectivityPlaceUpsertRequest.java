package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import com.brandPitara.sfs.project.enums.ProjectConnectivityType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConnectivityPlaceUpsertRequest {

  @NotBlank
  @Size(max = 150)
  private String placeName;

  @NotNull
  private ProjectConnectivityType placeType;

  @Size(max = 80)
  private String distanceLabel;

  @Size(max = 500)
  private String imageUrl;

  @Min(0) @Max(9999)
  private Integer sortOrder;

  private Boolean active;

  // Map intelligence fields
  @DecimalMin("-90.0")
  @DecimalMax("90.0")
  private Double latitude;

  @DecimalMin("-180.0")
  @DecimalMax("180.0")
  private Double longitude;

  @Min(0)
  private Integer distanceMeters;

  @Min(0)
  private Integer durationSeconds;

  @Size(max = 80)
  private String durationLabel;

  @Size(max = 180)
  private String externalPlaceId;

  @Size(max = 40)
  private String provider;

  @DecimalMin("0.0")
  @DecimalMax("5.0")
  private BigDecimal rating;

  @Min(0)
  private Integer userRatingCount;

  @Size(max = 500)
  private String address;

  private ProjectConnectivityCategory category;

  private Boolean verified;
  private Boolean featured;
}
