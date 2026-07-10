package com.brandPitara.sfs.dashboard.company.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyConnectedBrandResponse {
  private Long id;
  private Long companyId;
  private Long brandId;
  private String brandName;
  private String brandSlug;
  private String brandLogoUrl;
  private boolean brandPublished;
  private boolean brandActive;
  private boolean publicVisible;
  private boolean verified;
  private boolean featured;
  private int sortOrder;
  private String title;
  private String description;
  private boolean active;
}
