package com.brandPitara.sfs.project.dto;

import com.brandPitara.sfs.project.enums.ProjectStatus;
import com.brandPitara.sfs.project.enums.PropertyType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

  @Size(max = 120)
  @Pattern(
    regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
    message = "Slug must contain only lowercase letters, digits, and hyphens"
  )
  private String slug;

  @Size(max = 4000)
  private String description;

  private Long cityId;

  @Size(max = 300)
  private String addressLine;

  @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
  @DecimalMax(value = "90.0",  message = "Latitude must be between -90 and 90")
  private Double latitude;

  @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
  @DecimalMax(value = "180.0",  message = "Longitude must be between -180 and 180")
  private Double longitude;

  @Min(value = 0, message = "priceMin must be 0 or greater")
  private Long priceMin;

  @Min(value = 0, message = "priceMax must be 0 or greater")
  private Long priceMax;

  private LocalDate possessionDate;

  @Size(max = 50, message = "RERA number must not exceed 50 characters")
  private String reraNumber;

  private ProjectStatus status;
  private Set<PropertyType> propertyTypes;

  @Min(0) @Max(9999)
  private Integer priority;
  private Boolean active;
}
