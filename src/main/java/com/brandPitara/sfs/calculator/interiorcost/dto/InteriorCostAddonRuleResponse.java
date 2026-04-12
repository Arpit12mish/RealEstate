package com.brandPitara.sfs.calculator.interiorcost.dto;

import com.brandPitara.sfs.calculator.interiorcost.enums.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteriorCostAddonRuleResponse {

    private Long id;
    private Long companyId;
    private String companyName;
    private String cityName;
    private InteriorPackageType packageType;
    private InteriorAddonType addonType;
    private BigDecimal unitPrice;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean active;
    private String sourceNote;
}