package com.brandPitara.sfs.dashboard.builderhighlight.progress.controller;

import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightBuilderProgressResponse;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.dto.BuilderHighlightOverviewResponse;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.enums.BuilderHighlightOverallProgressStatus;
import com.brandPitara.sfs.dashboard.builderhighlight.progress.service.BuilderHighlightProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BuilderHighlightProgressController {

    private final BuilderHighlightProgressService progressService;

    @GetMapping("/api/dashboard/builder-highlights/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public BuilderHighlightOverviewResponse overview() {
        return progressService.getOverview();
    }

    @GetMapping("/api/dashboard/builders/{builderId}/highlights/progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public BuilderHighlightBuilderProgressResponse builderProgress(@PathVariable Long builderId) {
        return progressService.getBuilderProgress(builderId);
    }

    @GetMapping("/api/dashboard/builders/highlights/progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'DATA_ENTRY')")
    public Page<BuilderHighlightBuilderProgressResponse> listProgress(
        @RequestParam(required = false) String builderIds,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Long cityId,
        @RequestParam(required = false) BuilderHighlightOverallProgressStatus highlightStatus,
        @RequestParam(required = false) BuilderHighlightType missingSection,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), 50),
            Sort.by("builderName").ascending()
        );
        return progressService.listBuilderProgress(builderIds, q, cityId, highlightStatus, missingSection, pageable);
    }
}
