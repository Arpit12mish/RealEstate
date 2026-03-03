package com.brandPitara.sfs.builder.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuilderStatTileDto {
  private String label;
  private String value;
}