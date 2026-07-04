package com.brandPitara.sfs.dashboard.builderhighlight.progress.service;

import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightBuilderProgressResponse;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightOverviewResponse;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.enums.BuilderHighlightOverallProgressStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BuilderHighlightProgressService {

    BuilderHighlightOverviewResponse getOverview();

    BuilderHighlightBuilderProgressResponse getBuilderProgress(Long builderId);

    Page<BuilderHighlightBuilderProgressResponse> listBuilderProgress(
        String builderIdsCsv,
        String q,
        Long cityId,
        BuilderHighlightOverallProgressStatus highlightStatus,
        BuilderHighlightType missingSection,
        Pageable pageable
    );
}
