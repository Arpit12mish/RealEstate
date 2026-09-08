package com.brandPitara.sfs.company.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectClientRequirementDto {
  @Size(max = 180) private String title;
  @Size(max = 1000) private String description;
  @Size(max = 80) private String iconKey;
  @Size(max = 20) private String priority;
  private Integer sortOrder;
}
