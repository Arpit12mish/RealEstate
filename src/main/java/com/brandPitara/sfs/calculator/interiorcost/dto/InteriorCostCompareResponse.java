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
public class InteriorCostCompareResponse {

    private String cityName;
    private InteriorPropertyType propertyType;
    private BigDecimal area;
    private InteriorAreaUnitType areaUnit;
    private InteriorBhkType bhkType;
    private InteriorPackageType packageType;
    private InteriorScopeType scopeType;

    private Integer kitchenCount;
    private Integer wardrobeCount;
    private Integer bathroomCount;

    private String currency;
    private String message;
    private List<InteriorCostCompanyEstimateDto> results;
}