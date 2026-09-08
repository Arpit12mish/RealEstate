package com.brandPitara.sfs.project.service.model;

import com.brandPitara.sfs.project.dto.ProjectConnectivityResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectMasterPlanResponse;

import java.util.List;
import java.util.Set;

/** Detached child content loaded after the project has passed the public visibility policy. */
public record ProjectPublicContentData(
        List<ProjectFloorPlanResponse> floorPlans,
        ProjectConnectivityResponse connectivity,
        ProjectMasterPlanResponse masterPlan,
        Set<ProjectPublicSectionFailure> failedSections
) {
    public ProjectPublicContentData {
        floorPlans = floorPlans == null ? List.of() : List.copyOf(floorPlans);
        failedSections = failedSections == null ? Set.of() : Set.copyOf(failedSections);
    }
}
