package com.brandPitara.sfs.feed.service.section;

import lombok.Builder;

@Builder
public record FeedContext(
    String screen,      // ex: "BUILDER"
    Long entityId,      // builderId / brandId / projectId
    Long cityId,
    Long categoryId,
    Long clientVersion
) {}