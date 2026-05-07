package com.brandPitara.sfs.dashboard.overview.service.impl;

import com.brandPitara.sfs.dashboard.common.enums.FieldReviewStatus;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dashboard.overview.dto.DashboardOverviewResponse;
import com.brandPitara.sfs.dashboard.overview.service.DashboardOverviewService;
import com.brandPitara.sfs.dashboard.review.repository.DashboardFieldReviewIssueRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardOverviewServiceImpl implements DashboardOverviewService {

    private static final Set<FieldReviewStatus> OPEN_ISSUE_STATUSES =
            Set.of(FieldReviewStatus.WRONG, FieldReviewStatus.RECHECK);

    private final ProjectRepository projectRepository;
    private final DashboardFieldReviewIssueRepository fieldReviewIssueRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview() {
        OffsetDateTime startOfDay = OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime startOfNextDay = startOfDay.plusDays(1);

        return DashboardOverviewResponse.builder()
                .totalProjects(projectRepository.countByDeletedFalse())
                .draftProjects(projectRepository.countByDeletedFalseAndReviewStatus(ReviewStatus.DRAFT))
                .pendingReviewProjects(projectRepository.countByDeletedFalseAndReviewStatus(ReviewStatus.PENDING_REVIEW))
                .recheckProjects(projectRepository.countByDeletedFalseAndReviewStatus(ReviewStatus.RECHECK))
                .approvedProjects(projectRepository.countByDeletedFalseAndReviewStatus(ReviewStatus.APPROVED))
                .rejectedProjects(projectRepository.countByDeletedFalseAndReviewStatus(ReviewStatus.REJECTED))
                .openFieldIssues(fieldReviewIssueRepository.countByActiveTrueAndStatusIn(OPEN_ISSUE_STATUSES))
                .todaysSubmissions(projectRepository.countByDeletedFalseAndSubmittedAtBetween(startOfDay, startOfNextDay))
                .build();
    }
}
