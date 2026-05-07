package com.brandPitara.sfs.dashboard.overview.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardOverviewResponse {

    private long totalProjects;
    private long draftProjects;
    private long pendingReviewProjects;
    private long recheckProjects;
    private long approvedProjects;
    private long rejectedProjects;
    private long openFieldIssues;
    private long todaysSubmissions;
}
