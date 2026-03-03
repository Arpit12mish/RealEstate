package com.brandPitara.sfs.builder.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BuilderAboutDto {
  private Long id;
  private String name;
  private String logoUrl;
  private String description;

  private String phone;
  private String whatsapp;
  private String email;

  private String addressLine;
  private Long cityId;
  private String cityName;

  private Double latitude;
  private Double longitude;

  private Boolean active;
  private Boolean published;
  private Integer priority;
}