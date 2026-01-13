package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectResponse {

  private Long id;

  private Long builderId;
  private String builderName;
  private String builderLogoUrl;

  private String name;
  private String slug;
  private String description;

  private Long cityId;
  private String cityName;

  private String addressLine;
  private Double latitude;
  private Double longitude;

  private Long priceMin;
  private Long priceMax;

  private LocalDate possessionDate;
  private String reraNumber;

  private ProjectStatus status;
  private Set<PropertyType> propertyTypes;

  private boolean active;
  private boolean published;
  private int priority;
}
