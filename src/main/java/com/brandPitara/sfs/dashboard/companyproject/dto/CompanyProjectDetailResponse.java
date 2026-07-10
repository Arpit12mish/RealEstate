package com.brandPitara.sfs.dashboard.companyproject.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectDetailResponse {
  private Long id;
  private String name;
  private Long companyId;
  private String companyName;
  private String companyType;
  private Long cityId;
  private String cityName;
  private String addressLine;
  private String clientName;
  private String projectArea;
  private String detail3;
  private List<String> tags;
  private String description;
  private String coverMediaUrl;
  private String coverMediaType;
  private boolean active;
  private boolean deleted;
}
