package com.brandPitara.sfs.home.service.section;

import lombok.Builder;

@Builder
public record SectionContext(
    Long cityId,
    Long categoryId,

    // Optional depending on page
    Long projectId,
    Long builderId,
    Long brandId,
    Long distributorId,

    // Optional: auth user or flags later
    Long userId
) {}
