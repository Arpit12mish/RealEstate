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
public class CircleRateCalculateResponse {

    private String stateName;
    private String cityName;
    private String localityName;
    private CircleRatePropertyType propertyType;

    private BigDecimal inputArea;
    private CircleRateUnitType inputUnitType;

    private BigDecimal normalizedArea;
    private CircleRateUnitType normalizedUnitType;

    private BigDecimal ratePerUnit;
    private CircleRateUnitType rateUnitType;
    private CircleRateFormulaType formulaType;

    private BigDecimal estimatedValue;
    private String currency;
    private String displayMessage;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}