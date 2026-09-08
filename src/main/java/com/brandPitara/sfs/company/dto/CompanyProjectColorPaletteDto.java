package com.brandPitara.sfs.company.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectColorPaletteDto {
  @Size(max = 80) private String name;
  @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "hex must use #RRGGBB format")
  private String hex;
  private Integer sortOrder;
}
