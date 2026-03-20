package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectConnectivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectConnectivityPlaceUpsertRequest {

  @NotBlank
  private String placeName;

  @NotNull
  private ProjectConnectivityType placeType;

  private String distanceLabel;
  private String imageUrl;
  private Integer sortOrder;
  private Boolean active;
}