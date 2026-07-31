package com.brandPitara.sfs.builder.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BuilderPublicResponse {

  private Long id;
  private String name;
  private String slug;
  private String logoUrl;
  private String description;

  private String addressLine;
  private Long cityId;
  private String cityName;

  private Double latitude;
  private Double longitude;
}
