package com.brandPitara.sfs.calculator.model;

import com.brandPitara.sfs.calculator.enums.CalculatorType;

public record CalculatorCardDefinition(
    CalculatorType calculatorType,
    String title,
    String subtitle,
    String iconKey,
    String route,
    String actionTarget,
    Integer displayOrder,
    Boolean active
) {
}
