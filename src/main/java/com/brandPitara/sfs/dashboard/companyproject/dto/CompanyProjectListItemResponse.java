package com.brandPitara.sfs.dashboard.companyproject.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectListItemResponse {
  private Long id;
  private String name;
  private Long companyId;
  private String companyName;
  private String companyType;
  private Long cityId;
  private String cityName;
  private String coverMediaUrl;
  private boolean active;
  private boolean deleted;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
  private long brandsUsedCount;
}
