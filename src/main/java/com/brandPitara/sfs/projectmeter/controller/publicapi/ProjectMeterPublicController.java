package com.brandPitara.sfs.projectmeter.controller.publicapi;

import com.brandPitara.sfs.projectmeter.dto.ProjectConstructionProgressResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterDetailResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterSummaryResponse;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectMeterPublicController {

    private final ProjectMeterService projectMeterService;

    @GetMapping("/{projectId}/meter-summary")
    public ProjectMeterSummaryResponse meterSummary(@PathVariable Long projectId) {
        return projectMeterService.publicGetMeterSummary(projectId);
    }

    @GetMapping("/{projectId}/construction-progress")
    public ProjectConstructionProgressResponse constructionProgress(@PathVariable Long projectId) {
        return projectMeterService.publicGetConstructionProgress(projectId);
    }

    @GetMapping("/{projectId}/meter")
    public ProjectMeterDetailResponse meter(@PathVariable Long projectId) {
        return projectMeterService.publicGetMeterDetail(projectId);
    }
}