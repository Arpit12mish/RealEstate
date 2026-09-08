package com.brandPitara.sfs.company.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectStatDto {
  @Size(max = 80) private String label;
  @Size(max = 120) private String value;
  @Size(max = 80) private String iconKey;
  private Integer sortOrder;
}
