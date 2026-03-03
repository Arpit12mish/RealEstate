// com.brandPitara.sfs.home.dto.HomeHeaderDto.java
package com.brandPitara.sfs.home.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class HomeHeaderDto {
  private Long cityId;
  private String cityName;
  private List<String> sentences; // “Verified brands”, etc.
}
