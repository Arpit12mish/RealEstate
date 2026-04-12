package com.brandPitara.sfs.calculator.circlerate.dto;

import com.brandPitara.sfs.calculator.circlerate.enums.CircleRateFormulaType;
import com.brandPitara.sfs.calculator.circlerate.enums.CircleRatePropertyType;
import com.brandPitara.sfs.calculator.circlerate.enums.CircleRateUnitType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CircleRateRuleResponse {

    private Long id;
    private String stateName;
    private String cityName;
    private String localityName;
    private CircleRatePropertyType propertyType;
    private CircleRateUnitType unitType;
    private CircleRateFormulaType formulaType;
    private BigDecimal ratePerUnit;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean active;
    private String sourceNote;
}