package com.brandPitara.sfs.dashboard.company.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyCertificateResponse {
  private Long id;
  private Long companyId;
  private String title;
  private String issuer;
  private String description;
  private String certificateUrl;
  private String certificateFileUrl;
  private Integer year;
  private boolean verified;
  private boolean publicVisible;
  private int sortOrder;
  private boolean active;
}
