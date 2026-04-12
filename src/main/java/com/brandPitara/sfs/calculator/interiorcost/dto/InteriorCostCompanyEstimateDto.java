package com.brandPitara.sfs.calculator.interiorcost.dto;

import com.brandPitara.sfs.calculator.interiorcost.enums.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteriorCostCompanyEstimateDto {

    private Long companyId;
    private String companyName;
    private String companyLogoUrl;

    private InteriorPropertyType propertyType;
    private InteriorAreaUnitType areaUnit;
    private InteriorBhkType bhkType;
    private InteriorPackageType packageType;
    private InteriorScopeType scopeType;

    private BigDecimal baseRatePerUnit;
    private BigDecimal minimumProjectCost;
    private BigDecimal baseEstimate;
    private BigDecimal addOnAmount;
    private BigDecimal contingencyAmount;
    private BigDecimal taxAmount;
    private BigDecimal finalEstimate;

    private BigDecimal contingencyPercent;
    private BigDecimal taxPercent;

    private List<String> assumptions;
    private Boolean isLowest;
}