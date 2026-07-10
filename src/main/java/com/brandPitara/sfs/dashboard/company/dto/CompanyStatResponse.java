package com.brandPitara.sfs.dashboard.company.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyStatResponse {
  private Long id;
  private Long companyId;
  private String label;
  private String value;
  private String iconKey;
  private int sortOrder;
  private boolean publicVisible;
  private boolean active;
}
