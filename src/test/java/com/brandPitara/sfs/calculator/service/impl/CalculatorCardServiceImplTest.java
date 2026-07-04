package com.brandPitara.sfs.calculator.service.impl;

import com.brandPitara.sfs.calculator.enums.CalculatorType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculatorCardServiceImplTest {

    @Test
    void publicListHomeCalculatorCardsReturnsActiveOrderedPublicCards() {
        CalculatorCardServiceImpl service = new CalculatorCardServiceImpl();

        var cards = service.publicListHomeCalculatorCards(10);

        assertThat(cards)
            .extracting("calculatorType")
            .containsExactly(
                CalculatorType.EMI,
                CalculatorType.INTERIOR_COST,
                CalculatorType.STAMP_DUTY,
                CalculatorType.CIRCLE_RATE
            );
        assertThat(cards).allSatisfy(card -> {
            assertThat(card.getActive()).isTrue();
            assertThat(card.getRoute()).isNotBlank();
            assertThat(card.getAction()).isNotNull();
            assertThat(card.getAction().getType()).isEqualTo("NAVIGATE");
        });
    }

    @Test
    void publicListHomeCalculatorCardsAppliesLimit() {
        CalculatorCardServiceImpl service = new CalculatorCardServiceImpl();

        assertThat(service.publicListHomeCalculatorCards(2)).hasSize(2);
    }
}
