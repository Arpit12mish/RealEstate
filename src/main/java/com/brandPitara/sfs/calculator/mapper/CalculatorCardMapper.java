package com.brandPitara.sfs.calculator.mapper;

import com.brandPitara.sfs.calculator.dto.CalculatorActionDto;
import com.brandPitara.sfs.calculator.dto.CalculatorCardResponse;
import com.brandPitara.sfs.calculator.model.CalculatorCardDefinition;

public final class CalculatorCardMapper {

    private CalculatorCardMapper() {
    }

    public static CalculatorCardResponse toResponse(CalculatorCardDefinition definition) {
        if (definition == null) {
            return null;
        }

        return CalculatorCardResponse.builder()
            .calculatorType(definition.calculatorType())
            .title(definition.title())
            .subtitle(definition.subtitle())
            .iconKey(definition.iconKey())
            .route(definition.route())
            .action(CalculatorActionDto.builder()
                .type("NAVIGATE")
                .target(definition.actionTarget())
                .path(definition.route())
                .build())
            .displayOrder(definition.displayOrder())
            .active(Boolean.TRUE.equals(definition.active()))
            .build();
    }
}
