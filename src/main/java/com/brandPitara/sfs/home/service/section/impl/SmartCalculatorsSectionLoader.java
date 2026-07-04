package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.calculator.dto.CalculatorCardResponse;
import com.brandPitara.sfs.calculator.service.CalculatorCardService;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SmartCalculatorsSectionLoader implements HomeSectionLoader {

    private static final String DEFAULT_TITLE = "Smart Calculators";
    private static final String DEFAULT_SUBTITLE = "Plan costs before you decide";

    private final CalculatorCardService calculatorCardService;

    @Override
    public HomeSectionType supports() {
        return HomeSectionType.SMART_CALCULATORS;
    }

    @Override
    public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {
        int limit = Math.min(Math.max(cfg.getMaxItems() != null ? cfg.getMaxItems() : 4, 1), 10);
        List<CalculatorCardResponse> cards = calculatorCardService.publicListHomeCalculatorCards(limit);

        return HomeSectionDto.<CalculatorCardResponse>builder()
            .type(HomeSectionType.SMART_CALCULATORS)
            .key("SMART_CALCULATORS")
            .title(StringUtils.hasText(cfg.getTitle()) ? cfg.getTitle() : DEFAULT_TITLE)
            .subtitle(StringUtils.hasText(cfg.getSubtitle()) ? cfg.getSubtitle() : DEFAULT_SUBTITLE)
            .items(cards == null ? List.of() : cards)
            .build();
    }
}
