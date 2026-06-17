package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.dto.TrendingCityCardResponse;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.service.PublicCityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TrendingCitiesSectionLoader implements HomeSectionLoader {

    private static final String DEFAULT_TITLE = "Trending Cities";
    private static final String DEFAULT_SUBTITLE = "Hot real estate markets";

    private final PublicCityService publicCityService;

    @Override
    public HomeSectionType supports() {
        return HomeSectionType.TRENDING_CITIES;
    }

    @Override
    public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {
        int limit = Math.max(1, cfg.getMaxItems() != null ? cfg.getMaxItems() : 10);
        List<TrendingCityCardResponse> cards = publicCityService.getTrendingCities(limit);

        return HomeSectionDto.<TrendingCityCardResponse>builder()
                .type(HomeSectionType.TRENDING_CITIES)
                .key("TRENDING_CITIES")
                .title(StringUtils.hasText(cfg.getTitle()) ? cfg.getTitle() : DEFAULT_TITLE)
                .subtitle(StringUtils.hasText(cfg.getSubtitle()) ? cfg.getSubtitle() : DEFAULT_SUBTITLE)
                .items(cards)
                .build();
    }
}
