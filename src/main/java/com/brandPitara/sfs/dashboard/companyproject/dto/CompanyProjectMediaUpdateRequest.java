package com.brandPitara.sfs.dashboard.companyproject.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectMediaUpdateRequest {
  private String mediaUrl;
  @Size(max = 20) private String mediaType;
  @Size(max = 80) private String categoryKey;
  @Size(max = 120) private String categoryLabel;
  @Size(max = 160) private String title;
  private String description;
  @Size(max = 80) private String metricLabel;
  @Size(max = 120) private String metricValue;
  @Min(0) @Max(9999) private Integer sortOrder;
  private Boolean publicVisible;
  private Boolean active;
}
