package com.brandPitara.sfs.home.service.section.impl;

import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.entity.HomeSectionConfigEntity;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import com.brandPitara.sfs.home.service.section.HomeSectionLoader;
import com.brandPitara.sfs.home.service.section.SectionContext;
import com.brandPitara.sfs.instagram.dto.PublicInstagramReelItemResponse;
import com.brandPitara.sfs.instagram.service.InstagramReelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InstagramReelsSectionLoader implements HomeSectionLoader {

    private static final String DEFAULT_TITLE = "Instagram Reels";
    private static final String DEFAULT_SUBTITLE = "See our latest reels on instagram";

    private final InstagramReelService instagramReelService;

    @Override
    public HomeSectionType supports() {
        return HomeSectionType.INSTAGRAM_REELS;
    }

    @Override
    public HomeSectionDto<?> load(HomeSectionConfigEntity cfg, SectionContext ctx) {
        int requestedLimit = cfg.getMaxItems() != null ? cfg.getMaxItems() : 10;
        List<PublicInstagramReelItemResponse> items = instagramReelService.publicHomeItems(requestedLimit);

        return HomeSectionDto.<PublicInstagramReelItemResponse>builder()
            .type(HomeSectionType.INSTAGRAM_REELS)
            .key("INSTAGRAM_REELS")
            .title(StringUtils.hasText(cfg.getTitle()) ? cfg.getTitle() : DEFAULT_TITLE)
            .subtitle(StringUtils.hasText(cfg.getSubtitle()) ? cfg.getSubtitle() : DEFAULT_SUBTITLE)
            .items(items)
            .build();
    }
}
