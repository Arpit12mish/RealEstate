package com.brandPitara.sfs.home.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProjectPlanCardDto {
  private String imageUrl;
  private String title;
  private List<String> tags;
  private String description;
  private String companyLogoUrl;
}
