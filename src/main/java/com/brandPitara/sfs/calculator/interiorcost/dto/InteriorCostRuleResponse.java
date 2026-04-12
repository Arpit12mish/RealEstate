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
public class InteriorCostRuleResponse {

    private Long id;
    private Long companyId;
    private String companyName;
    private String cityName;
    private InteriorPropertyType propertyType;
    private InteriorAreaUnitType areaUnit;
    private InteriorBhkType bhkType;
    private InteriorPackageType packageType;
    private InteriorScopeType scopeType;
    private BigDecimal minArea;
    private BigDecimal maxArea;
    private BigDecimal baseRatePerUnit;
    private BigDecimal minimumProjectCost;
    private BigDecimal contingencyPercent;
    private BigDecimal taxPercent;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean active;
    private String sourceNote;
}