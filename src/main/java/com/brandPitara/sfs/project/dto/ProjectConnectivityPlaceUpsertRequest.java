package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectConnectivityType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

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
}