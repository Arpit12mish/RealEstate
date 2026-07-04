package com.brandPitara.sfs.calculator.service.impl;

import com.brandPitara.sfs.calculator.dto.CalculatorCardResponse;
import com.brandPitara.sfs.calculator.enums.CalculatorType;
import com.brandPitara.sfs.calculator.mapper.CalculatorCardMapper;
import com.brandPitara.sfs.calculator.model.CalculatorCardDefinition;
import com.brandPitara.sfs.calculator.service.CalculatorCardService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class CalculatorCardServiceImpl implements CalculatorCardService {

    private static final int MAX_HOME_CALCULATORS = 10;

    private static final List<CalculatorCardDefinition> HOME_CALCULATORS = List.of(
        new CalculatorCardDefinition(
            CalculatorType.EMI,
            "EMI Calculator",
            "Estimate monthly loan payments",
            "calculator-emi",
            "/calculators/emi",
            "EMI_CALCULATOR",
            10,
            true
        ),
        new CalculatorCardDefinition(
            CalculatorType.INTERIOR_COST,
            "Interior Cost",
            "Compare furnishing and fit-out budgets",
            "calculator-interior-cost",
            "/calculators/interior-cost",
            "INTERIOR_COST_CALCULATOR",
            20,
            true
        ),
        new CalculatorCardDefinition(
            CalculatorType.STAMP_DUTY,
            "Stamp Duty",
            "Estimate registration and duty charges",
            "calculator-stamp-duty",
            "/calculators/stamp-duty",
            "STAMP_DUTY_CALCULATOR",
            30,
            true
        ),
        new CalculatorCardDefinition(
            CalculatorType.CIRCLE_RATE,
            "Circle Rate",
            "Check government guidance values",
            "calculator-circle-rate",
            "/calculators/circle-rate",
            "CIRCLE_RATE_CALCULATOR",
            40,
            true
        )
    );

    @Override
    public List<CalculatorCardResponse> publicListHomeCalculatorCards(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_HOME_CALCULATORS);

        return HOME_CALCULATORS.stream()
            .filter(definition -> Boolean.TRUE.equals(definition.active()))
            .sorted(Comparator
                .comparing(CalculatorCardDefinition::displayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(definition -> definition.calculatorType().name()))
            .limit(safeLimit)
            .map(CalculatorCardMapper::toResponse)
            .filter(Objects::nonNull)
            .toList();
    }
}
