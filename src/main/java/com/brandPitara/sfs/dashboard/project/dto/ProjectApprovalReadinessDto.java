package com.brandPitara.sfs.dashboard.project.dto;

import com.brandPitara.sfs.dashboard.review.dto.FieldReviewIssueResponse;
import com.brandPitara.sfs.project.dto.ProjectConnectivityResponse;
import com.brandPitara.sfs.project.dto.ProjectFloorPlanResponse;
import com.brandPitara.sfs.project.dto.ProjectHighlightResponse;
import com.brandPitara.sfs.project.dto.ProjectMediaResponse;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterSummaryResponse;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.List;

@Getter
@Builder
public class ProjectApprovalReadinessDto {

    private boolean hasName;
    private boolean hasDescription;
    private boolean hasPriceRange;
    private boolean hasLocation;
    private boolean hasReraNumber;
    private boolean hasPossessionDate;
    private boolean hasMedia;
    private boolean hasFloorPlans;
    private boolean hasHighlights;
    private boolean hasConnectivity;
    private boolean hasMasterPlan;
    private boolean hasMeterData;
    private boolean noActiveFieldIssues;
    private boolean isReadyForSubmission;

    public static ProjectApprovalReadinessDto compute(
            ProjectResponse project,
            List<ProjectMediaResponse> media,
            List<ProjectFloorPlanResponse> floorPlans,
            List<ProjectHighlightResponse> highlights,
            ProjectConnectivityResponse connectivity,
            com.brandPitara.sfs.project.dto.ProjectMasterPlanResponse masterPlan,
            ProjectMeterSummaryResponse meterSummary,
            List<FieldReviewIssueResponse> fieldIssues
    ) {
        boolean hasName        = StringUtils.hasText(project.getName());
        boolean hasDescription = StringUtils.hasText(project.getDescription());
        boolean hasPriceRange  = project.getPriceMin() != null && project.getPriceMax() != null;
        boolean hasLocation    = project.getLatitude() != null && project.getLongitude() != null;
        boolean hasReraNumber  = StringUtils.hasText(project.getReraNumber());
        boolean hasPossDate    = project.getPossessionDate() != null;
        boolean hasMedia       = media != null && !media.isEmpty();
        boolean hasFloorPlans  = floorPlans != null && !floorPlans.isEmpty();
        boolean hasHighlights  = highlights != null && !highlights.isEmpty();
        boolean hasConn        = connectivity != null;
        boolean hasMasterPlan  = masterPlan != null
                && ((masterPlan.getStats() != null && !masterPlan.getStats().isEmpty())
                || StringUtils.hasText(masterPlan.getImageUrl()));
        boolean hasMeter       = meterSummary != null;
        boolean noActiveIssues = fieldIssues == null || fieldIssues.stream()
                .noneMatch(i -> Boolean.TRUE.equals(i.getActive()));

        boolean ready = hasName && hasDescription && hasPriceRange
                && hasLocation && hasReraNumber && hasPossDate
                && hasMedia && noActiveIssues;

        return ProjectApprovalReadinessDto.builder()
                .hasName(hasName)
                .hasDescription(hasDescription)
                .hasPriceRange(hasPriceRange)
                .hasLocation(hasLocation)
                .hasReraNumber(hasReraNumber)
                .hasPossessionDate(hasPossDate)
                .hasMedia(hasMedia)
                .hasFloorPlans(hasFloorPlans)
                .hasHighlights(hasHighlights)
                .hasConnectivity(hasConn)
                .hasMasterPlan(hasMasterPlan)
                .hasMeterData(hasMeter)
                .noActiveFieldIssues(noActiveIssues)
                .isReadyForSubmission(ready)
                .build();
    }
}
