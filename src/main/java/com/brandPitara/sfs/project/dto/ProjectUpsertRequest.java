package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.Set;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectUpsertRequest {

  @NotBlank
  @Size(max = 180)
  private String name;

  private String slug;
  private String description;

  private Long cityId;
  private String addressLine;

  private Double latitude;
  private Double longitude;

  private Long priceMin;
  private Long priceMax;

  private LocalDate possessionDate;
  private String reraNumber;

  private ProjectStatus status;
  private Set<PropertyType> propertyTypes;

  private Integer priority;
  private Boolean active;
}
