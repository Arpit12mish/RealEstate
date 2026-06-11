package com.brandPitara.sfs.builderimprovement.controller.publicapi;

import com.brandPitara.sfs.builderimprovement.dto.response.BuilderImprovementResponse;
import com.brandPitara.sfs.builderimprovement.dto.response.BuilderImprovementSummaryResponse;
import com.brandPitara.sfs.builderimprovement.service.BuilderImprovementQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/builders")
@RequiredArgsConstructor
public class BuilderImprovementPublicController {

    private final BuilderImprovementQueryService queryService;

    @GetMapping("/{builderId}/improvement-summary")
    public BuilderImprovementSummaryResponse getSummary(@PathVariable Long builderId) {
        return queryService.getBuilderImprovementSummary(builderId);
    }

    @GetMapping("/{builderId}/improvement-journey")
    public BuilderImprovementResponse getBuilderJourney(@PathVariable Long builderId) {
        return queryService.getBuilderLevelJourney(builderId);
    }

    @GetMapping("/{builderId}/projects/{projectId}/improvement-journey")
    public BuilderImprovementResponse getProjectJourney(
            @PathVariable Long builderId,
            @PathVariable Long projectId
    ) {
        return queryService.getProjectLevelJourney(builderId, projectId);
    }
}