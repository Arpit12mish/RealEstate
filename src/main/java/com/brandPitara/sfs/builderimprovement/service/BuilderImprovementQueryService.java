package com.brandPitara.sfs.builderimprovement.service;

import com.brandPitara.sfs.builderimprovement.dto.response.BuilderImprovementResponse;
import com.brandPitara.sfs.builderimprovement.dto.response.BuilderImprovementSummaryResponse;

public interface BuilderImprovementQueryService {

    BuilderImprovementSummaryResponse getBuilderImprovementSummary(Long builderId);

    BuilderImprovementResponse getBuilderLevelJourney(Long builderId);

    BuilderImprovementResponse getProjectLevelJourney(Long builderId, Long projectId);
}