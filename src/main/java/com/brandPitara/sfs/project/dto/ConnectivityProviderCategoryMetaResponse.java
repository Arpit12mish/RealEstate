package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectivityProviderCategoryMetaResponse {
  private ProjectConnectivityCategory category;
  private String categoryLabel;
  private String iconKey;
  private String defaultQuery;
  private Integer defaultRadiusMeters;
}
