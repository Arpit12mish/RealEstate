package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import com.brandPitara.sfs.project.enums.ProjectConnectivityType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectivityProviderSearchRequest {

  @NotNull
  private ProjectConnectivityCategory category;

  @Size(max = 120)
  private String query;

  @Size(max = 120)
  private String keyword;

  private ProjectConnectivityType placeType;

  @Min(500)
  @Max(50000)
  private Integer radiusMeters;

  @Min(1)
  @Max(50)
  private Integer limit;
}
