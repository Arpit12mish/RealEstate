package com.brandPitara.sfs.project.dto;

import lombok.*;
import java.util.Set;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectCardDto {
  private Long id;
  private String name;

  private Long cityId;
  private String cityName;
  private String addressLine;

  private Long priceMin;
  private Long priceMax;

  private Long builderId;
  private String builderName;
  private String builderLogoUrl;

  private String coverMediaUrl;
  private String coverMediaType;

  private Set<?> propertyTypes; // keep if needed
}
