package com.brandPitara.sfs.calculator.circlerate.strategy;

import com.brandPitara.sfs.calculator.circlerate.dto.CircleRateCalculateRequest;
import com.brandPitara.sfs.calculator.circlerate.dto.CircleRateCalculateResponse;
import com.brandPitara.sfs.calculator.circlerate.entity.CircleRateRuleEntity;
import com.brandPitara.sfs.calculator.circlerate.enums.CircleRateFormulaType;

public interface CircleRateFormulaStrategy {

    CircleRateFormulaType getSupportedFormulaType();

    CircleRateCalculateResponse calculate(
            CircleRateRuleEntity rule,
            CircleRateCalculateRequest request
    );
}