package com.brandPitara.sfs.dashboard.company.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyListItemResponse {
  private Long id;
  private String name;
  private String slug;
  private String companyType;
  private String logoUrl;
  private Long cityId;
  private String cityName;
  private boolean active;
  private boolean published;
  private boolean deleted;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
  private long connectedBrandsCount;
}
