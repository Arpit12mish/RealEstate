package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
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

  @NotBlank
  @Size(max = 120)
  private String query;

  @Min(100)
  @Max(50000)
  private Integer radiusMeters;
}
