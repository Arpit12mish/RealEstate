package com.brandPitara.sfs.company.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectPriceBreakdownItemDto {
  @Size(max = 160) private String label;
  private BigDecimal amount;
  @Size(max = 80) private String amountLabel;
  private BigDecimal percentage;
  private Integer sortOrder;
}
