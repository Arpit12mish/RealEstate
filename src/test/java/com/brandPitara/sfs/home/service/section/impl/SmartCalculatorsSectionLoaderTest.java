package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.calculator.dto.CalculatorCardResponse;
import com.brandPitara.sfs.calculator.enums.CalculatorType;
import com.brandPitara.sfs.calculator.service.CalculatorCardService;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SmartCalculatorsSectionLoaderTest {

    @Test
    void loadBuildsSmartCalculatorsHomeSection() {
        CalculatorCardService calculatorCardService = mock(CalculatorCardService.class);
        SmartCalculatorsSectionLoader loader = new SmartCalculatorsSectionLoader(calculatorCardService);

        HomeSectionConfigEntity cfg = HomeSectionConfigEntity.builder()
            .title("Smart Calculators")
            .subtitle("Plan before buying")
            .maxItems(4)
            .build();
        CalculatorCardResponse card = CalculatorCardResponse.builder()
            .calculatorType(CalculatorType.EMI)
            .title("EMI Calculator")
            .active(true)
            .build();

        when(calculatorCardService.publicListHomeCalculatorCards(4)).thenReturn(List.of(card));

        HomeSectionDto<?> section = loader.load(cfg, null);

        assertThat(section.getType()).isEqualTo(HomeSectionType.SMART_CALCULATORS);
        assertThat(section.getKey()).isEqualTo("SMART_CALCULATORS");
        assertThat(section.getTitle()).isEqualTo("Smart Calculators");
        assertThat(section.getSubtitle()).isEqualTo("Plan before buying");
        assertThat(section.getItems()).hasSize(1);
        assertThat(section.getItems().get(0)).isSameAs(card);
        verify(calculatorCardService).publicListHomeCalculatorCards(4);
    }

    @Test
    void loadCapsLimitAtTenAndUsesDefaultCopy() {
        CalculatorCardService calculatorCardService = mock(CalculatorCardService.class);
        SmartCalculatorsSectionLoader loader = new SmartCalculatorsSectionLoader(calculatorCardService);

        HomeSectionConfigEntity cfg = HomeSectionConfigEntity.builder()
            .title(" ")
            .subtitle(null)
            .maxItems(99)
            .build();
        when(calculatorCardService.publicListHomeCalculatorCards(10)).thenReturn(List.of());

        HomeSectionDto<?> section = loader.load(cfg, null);

        assertThat(section.getTitle()).isEqualTo("Smart Calculators");
        assertThat(section.getSubtitle()).isEqualTo("Plan costs before you decide");
        assertThat(section.getItems()).isEmpty();
        verify(calculatorCardService).publicListHomeCalculatorCards(10);
    }
}
