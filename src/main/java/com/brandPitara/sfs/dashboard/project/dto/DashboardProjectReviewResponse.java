package com.brandPitara.sfs.dashboard.project.dto;

import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardProjectReviewResponse {

    private Long projectId;
    private String projectName;
    private String projectSlug;

    private Boolean active;
    private Boolean published;
    private Boolean deleted;

    private ReviewStatus reviewStatus;

    private Long createdByDashboardUserId;

    private Long submittedByDashboardUserId;
    private OffsetDateTime submittedAt;

    private Long reviewedByDashboardUserId;
    private OffsetDateTime reviewedAt;

    private String reviewRemarks;

    public static DashboardProjectReviewResponse from(ProjectEntity project) {
        if (project == null) {
            return null;
        }

        return DashboardProjectReviewResponse.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .projectSlug(project.getSlug())
                .active(project.getActive())
                .published(project.getPublished())
                .deleted(project.getDeleted())
                .reviewStatus(project.getReviewStatus())
                .createdByDashboardUserId(project.getCreatedByDashboardUserId())
                .submittedByDashboardUserId(project.getSubmittedByDashboardUserId())
                .submittedAt(project.getSubmittedAt())
                .reviewedByDashboardUserId(project.getReviewedByDashboardUserId())
                .reviewedAt(project.getReviewedAt())
                .reviewRemarks(project.getReviewRemarks())
                .build();
    }
}