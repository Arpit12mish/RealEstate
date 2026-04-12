package com.brandPitara.sfs.calculator.circlerate.strategy.impl;

import com.brandPitara.sfs.calculator.circlerate.dto.CircleRateCalculateRequest;
import com.brandPitara.sfs.calculator.circlerate.dto.CircleRateCalculateResponse;
import com.brandPitara.sfs.calculator.circlerate.entity.CircleRateRuleEntity;
import com.brandPitara.sfs.calculator.circlerate.enums.CircleRateFormulaType;
import com.brandPitara.sfs.calculator.circlerate.enums.CircleRateUnitType;
import com.brandPitara.sfs.calculator.circlerate.strategy.CircleRateFormulaStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SimpleRateXAreaStrategy implements CircleRateFormulaStrategy {

    private static final BigDecimal SQFT_TO_SQM = new BigDecimal("0.092903");
    private static final BigDecimal SQYD_TO_SQM = new BigDecimal("0.836127");
    private static final BigDecimal SQM_TO_SQFT = new BigDecimal("10.7639");
    private static final BigDecimal SQM_TO_SQYD = new BigDecimal("1.19599");

    @Override
    public CircleRateFormulaType getSupportedFormulaType() {
        return CircleRateFormulaType.SIMPLE_RATE_X_AREA;
    }

    @Override
    public CircleRateCalculateResponse calculate(
            CircleRateRuleEntity rule,
            CircleRateCalculateRequest request
    ) {
        BigDecimal normalizedArea = convertArea(
                request.getArea(),
                request.getUnitType(),
                rule.getUnitType()
        );

        BigDecimal estimatedValue = normalizedArea.multiply(rule.getRatePerUnit())
                .setScale(2, RoundingMode.HALF_UP);

        return CircleRateCalculateResponse.builder()
                .stateName(request.getStateName())
                .cityName(request.getCityName())
                .localityName(request.getLocalityName())
                .propertyType(request.getPropertyType())
                .inputArea(request.getArea())
                .inputUnitType(request.getUnitType())
                .normalizedArea(normalizedArea)
                .normalizedUnitType(rule.getUnitType())
                .ratePerUnit(rule.getRatePerUnit())
                .rateUnitType(rule.getUnitType())
                .formulaType(rule.getFormulaType())
                .estimatedValue(estimatedValue)
                .currency("INR")
                .displayMessage("Estimated minimum government value is ₹" + estimatedValue.toPlainString())
                .effectiveFrom(rule.getEffectiveFrom())
                .effectiveTo(rule.getEffectiveTo())
                .build();
    }

    private BigDecimal convertArea(
            BigDecimal area,
            CircleRateUnitType fromUnit,
            CircleRateUnitType toUnit
    ) {
        if (fromUnit == toUnit) {
            return area.setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal areaInSqm = switch (fromUnit) {
            case SQM -> area;
            case SQFT -> area.multiply(SQFT_TO_SQM);
            case SQYD -> area.multiply(SQYD_TO_SQM);
        };

        BigDecimal converted = switch (toUnit) {
            case SQM -> areaInSqm;
            case SQFT -> areaInSqm.multiply(SQM_TO_SQFT);
            case SQYD -> areaInSqm.multiply(SQM_TO_SQYD);
        };

        return converted.setScale(4, RoundingMode.HALF_UP);
    }
}