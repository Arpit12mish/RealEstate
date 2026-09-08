package com.brandPitara.sfs.company.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectMediaResponse {
  private Long id;
  private Long companyProjectId;
  private String mediaUrl;
  private String mediaType;
  private String categoryKey;
  private String categoryLabel;
  private String title;
  private String description;
  private String metricLabel;
  private String metricValue;
  private Integer sortOrder;
  private Boolean publicVisible;
  private Boolean active;
}
