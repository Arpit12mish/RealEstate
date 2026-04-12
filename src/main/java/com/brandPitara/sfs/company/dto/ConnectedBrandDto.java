package com.brandPitara.sfs.company.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectedBrandDto {
  private Long brandId;
  private String name;
  private String logoUrl;
}