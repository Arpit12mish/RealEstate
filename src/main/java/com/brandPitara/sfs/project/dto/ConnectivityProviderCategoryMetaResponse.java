package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectConnectivityCategory;
import lombok.*;

import java.util.List;

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
  private List<QueryPreset> queries;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class QueryPreset {
    private String placeType;
    private String label;
    private String keyword;
  }
}
