package com.brandPitara.sfs.builder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BuilderUpsertRequest {

  @NotBlank
  @Size(max = 150)
  private String name;

  private String logoUrl;
  private String description;

  private String phone;
  private String whatsapp;
  private String email;

  private String addressLine;
  private Long cityId;

  private Double latitude;
  private Double longitude;

  private Integer priority;
  private Boolean active;
}
