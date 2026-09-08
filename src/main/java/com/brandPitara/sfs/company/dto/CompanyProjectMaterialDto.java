package com.brandPitara.sfs.company.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectMaterialDto {
  @Size(max = 120) private String name;
  private String imageUrl;
  private Integer sortOrder;
}
