package com.brandPitara.sfs.dashboard.company.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyMediaResponse {
  private Long id;
  private Long companyId;
  private String mediaUrl;
  private String mediaType;
  private String usageType;
  private String title;
  private String altText;
  private int sortOrder;
  private boolean publicVisible;
  private boolean active;
}
