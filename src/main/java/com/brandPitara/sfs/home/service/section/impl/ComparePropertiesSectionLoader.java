package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.home.dto.ComparePropertiesCardDto;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ComparePropertiesSectionLoader implements HomeSectionLoader {

    private static final String DEFAULT_TITLE    = "Compare Properties";
    private static final String DEFAULT_SUBTITLE = "Compare prices, amenities, approvals and project scores side by side";
    private static final String DEFAULT_CTA_TEXT  = "Compare Now";
    private static final String ACTION_TYPE       = "OPEN_COMPARE_SEARCH";
    private static final String ACTION_VALUE      = "/search?mode=compare";
    private static final String MEDIA_TYPE        = "LOTTIE_JSON";

    @Override
    public HomeSectionType supports() {
        return HomeSectionType.COMPARE_PROPERTIES;
    }

    @Override
    public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {
        String mediaUrl = cfg.getParam1();
        if (mediaUrl == null || mediaUrl.isBlank()) {
            return null;
        }

        ComparePropertiesCardDto card = ComparePropertiesCardDto.builder()
                .mediaType(MEDIA_TYPE)
                .mediaUrl(mediaUrl)
                .ctaText(cfg.getTitle() != null ? cfg.getTitle() : DEFAULT_CTA_TEXT)
                .actionType(ACTION_TYPE)
                .actionValue(ACTION_VALUE)
                .build();

        return HomeSectionDto.<ComparePropertiesCardDto>builder()
                .type(HomeSectionType.COMPARE_PROPERTIES)
                .key("COMPARE_PROPERTIES")
                .title(cfg.getTitle() != null ? cfg.getTitle() : DEFAULT_TITLE)
                .subtitle(cfg.getSubtitle() != null ? cfg.getSubtitle() : DEFAULT_SUBTITLE)
                .items(List.of(card))
                .build();
    }
}
