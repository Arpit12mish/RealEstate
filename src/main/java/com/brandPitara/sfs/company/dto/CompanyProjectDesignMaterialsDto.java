package com.brandPitara.sfs.company.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectDesignMaterialsDto {
  @Size(max = 160) private String designStyle;
  @Size(max = 1000) private String designConcept;
  @Valid @Size(max = 10) @Builder.Default
  private List<CompanyProjectColorPaletteDto> colorPalette = new ArrayList<>();
  @Valid @Size(max = 30) @Builder.Default
  private List<CompanyProjectMaterialDto> materialsUsed = new ArrayList<>();
  @Size(max = 180) private String flooring;
  @Size(max = 180) private String wallFinish;
  @Size(max = 300) private String lighting;
  @Size(max = 300) private String furniture;
}
