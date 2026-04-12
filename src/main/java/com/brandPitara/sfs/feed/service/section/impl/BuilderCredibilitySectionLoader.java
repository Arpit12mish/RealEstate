package com.brandPitara.sfs.feed.service.section.impl;

import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilitySummaryResponse;
import com.brandPitara.sfs.buildercredibility.service.BuilderCredibilityService;
import com.brandPitara.sfs.feed.entity.FeedSectionConfigEntity;
import com.brandPitara.sfs.feed.service.section.FeedContext;
import com.brandPitara.sfs.feed.service.section.FeedSectionLoader;
import com.brandPitara.sfs.home.dto.HomeSectionDto;
import com.brandPitara.sfs.home.enums.HomeSectionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BuilderCredibilitySectionLoader implements FeedSectionLoader {

    private final BuilderCredibilityService builderCredibilityService;

    @Override
    public String supports() {
        return "BUILDER_CREDIBILITY";
    }

    @Override
    public HomeSectionDto<?> load(FeedSectionConfigEntity cfg, FeedContext ctx) {
        if (ctx == null || ctx.entityId() == null) {
            return null;
        }

        BuilderCredibilitySummaryResponse summary =
            builderCredibilityService.publicGetCredibilitySummary(ctx.entityId());

        return HomeSectionDto.<BuilderCredibilitySummaryResponse>builder()
            .key("CREDIBILITY")
            .type(HomeSectionType.BUILDER_CREDIBILITY)
            .title(cfg == null ? null : cfg.getTitle())
            .items(List.of(summary))
            .build();
    }
}