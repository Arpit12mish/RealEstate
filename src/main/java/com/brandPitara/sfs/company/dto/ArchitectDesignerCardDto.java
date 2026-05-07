package com.brandPitara.sfs.company.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchitectDesignerCardDto {
  private Long companyId;

  private String name;
  private String companyType;

  private String logoUrl;
  private String coverImageUrl;

  // ex: "Architecture | Interiors | Landscape"
  private String specializationText;

  // Stats row
  private String projectsCompleted;
  private String yearsExperience;
  private String citiesServed;
  private String awardsCount;

  // Bottom descriptive lines
  private String infoLine1;
  private String infoLine2;
}