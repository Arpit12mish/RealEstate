package com.brandPitara.sfs.company.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectBudgetDto {
  private BigDecimal totalBudget;
  @Size(max = 10) private String currency;
  @Size(max = 80) private String budgetLabel;
  @Size(max = 300) private String note;
  @Size(max = 100) private String costPerSqftLabel;
  @Size(max = 100) private String builtUpAreaLabel;
}
