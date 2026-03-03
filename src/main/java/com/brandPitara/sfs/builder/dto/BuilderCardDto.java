package com.brandPitara.sfs.builder.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class BuilderCardDto {
  private Long id;
  private String name;
  private String logoUrl;
  private Long cityId;
  private String cityName;
}
