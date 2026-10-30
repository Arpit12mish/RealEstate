package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * "Festive picks" block under the hero. Title and subtitle come from the home_section_config row
 * so they can be renamed (or the row disabled) without an app release; the tab list is fixed
 * here and keyed, because the app owns each tab's artwork and card layout.
 */
@Component
public class FestivePicksSectionLoader implements HomeSectionLoader {

    static final String KEY = "FESTIVE_PICKS";
    static final String DEFAULT_TITLE = "Festive picks on SFS";
    static final String DEFAULT_SUBTITLE = "Verified builders, homes, shops and pros in one place";

    public record Tab(String key, String label) {
    }

    static final List<Tab> TABS = List.of(
            new Tab("TRUSTED_BUILDERS", "Trusted Builders"),
            new Tab("VERIFIED_PROPERTIES", "Verified Properties"),
            new Tab("LOCAL_SHOPS", "Local Shops"),
            new Tab("HOME_SERVICES", "Home Services"),
            new Tab("TOP_ARCHITECTS", "Top Architects"),
            new Tab("INTERIOR_DESIGNERS", "Interior Designers"),
            new Tab("SMART_CALCULATORS", "Smart Calculators"),
            new Tab("PARTNER_BRANDS", "Partner Brands"));

    @Override
    public HomeSectionType supports() {
        return HomeSectionType.FESTIVE_PICKS;
    }

    @Override
    public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {
        return HomeSectionDto.<Tab>builder()
                .type(HomeSectionType.FESTIVE_PICKS)
                .key(KEY)
                .title(StringUtils.hasText(cfg.getTitle()) ? cfg.getTitle() : DEFAULT_TITLE)
                .subtitle(StringUtils.hasText(cfg.getSubtitle()) ? cfg.getSubtitle() : DEFAULT_SUBTITLE)
                .items(TABS)
                .build();
    }
}
