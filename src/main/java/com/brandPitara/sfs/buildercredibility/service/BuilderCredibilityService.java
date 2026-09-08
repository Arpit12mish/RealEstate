package com.brandPitara.sfs.buildercredibility.service;

import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilityCardResponse;
import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilityResponse;
import com.brandPitara.sfs.buildercredibility.dto.BuilderCredibilitySummaryResponse;

import java.util.List;
import java.util.Collection;
import java.util.Map;

public interface BuilderCredibilityService {
    BuilderCredibilityResponse publicGetCredibility(Long builderId);
    BuilderCredibilitySummaryResponse publicGetCredibilitySummary(Long builderId);
    Map<Long, BuilderCredibilitySummaryResponse> publicGetCredibilitySummaries(Collection<Long> builderIds);
    List<BuilderCredibilityCardResponse> publicListCredibilityCards(Long cityId, int limit);
}
