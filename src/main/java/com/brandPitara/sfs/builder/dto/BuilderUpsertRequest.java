package com.brandPitara.sfs.builder.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BuilderUpsertRequest {

  @NotBlank
  @Size(max = 150)
  private String name;

  @Size(max = 500)
  private String logoUrl;

  @Size(max = 4000)
  private String description;

  @Size(max = 20)
  private String phone;

  @Size(max = 20)
  private String whatsapp;

  @Email
  @Size(max = 120)
  private String email;

  @Size(max = 300)
  private String addressLine;
  private Long cityId;

  @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
  @DecimalMax(value = "90.0",  message = "Latitude must be between -90 and 90")
  private Double latitude;

  @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
  @DecimalMax(value = "180.0",  message = "Longitude must be between -180 and 180")
  private Double longitude;

  @Min(0) @Max(9999)
  private Integer priority;
  private Boolean active;
}
