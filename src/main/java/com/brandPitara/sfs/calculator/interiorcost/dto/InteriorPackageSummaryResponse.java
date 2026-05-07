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
public class InteriorPackageSummaryResponse {

    private String cityName;
    private InteriorPropertyType propertyType;
    private InteriorBhkType bhkType;
    private InteriorScopeType scopeType;
    private BigDecimal area;
    private InteriorAreaUnitType areaUnit;
    private String currency;
    private String message;
    private List<InteriorPackageSummaryItemDto> packages;
}