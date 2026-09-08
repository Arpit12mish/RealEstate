package com.brandPitara.sfs.cdn;

import com.brandPitara.sfs.cdn.event.ProjectCacheEvictionReason;
import com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionPublisher;
import com.brandPitara.sfs.common.contentVersion.service.ContentVersionService;
import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.dashboard.project.service.impl.DashboardProjectReviewServiceImpl;
import com.brandPitara.sfs.dashboard.review.service.DashboardFieldReviewIssueService;
import com.brandPitara.sfs.dashboard.review.service.DashboardReviewHistoryService;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.builder.repository.BuilderRepository;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.projectmeter.repository.*;
import com.brandPitara.sfs.projectmeter.service.impl.ProjectMeterSnapshotRecalculationServiceImpl;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectMutationEvictionAdditionalTest {

    @Test
    void dashboardApprovalPathPublishesVisibilityEviction() {
        ProjectRepository projects = mock(ProjectRepository.class);
        DashboardCurrentUserService currentUser = mock(DashboardCurrentUserService.class);
        DashboardFieldReviewIssueService issues = mock(DashboardFieldReviewIssueService.class);
        DashboardReviewHistoryService history = mock(DashboardReviewHistoryService.class);
        ProjectPublicCacheEvictionPublisher publisher = mock(ProjectPublicCacheEvictionPublisher.class);
        ProjectEntity project = ProjectEntity.builder().id(27L).reviewStatus(ReviewStatus.DRAFT)
                .published(false).active(true).deleted(false).build();
        when(projects.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project));
        when(projects.save(project)).thenReturn(project);
        when(currentUser.getCurrentUserOrThrow()).thenReturn(
                DashboardUserEntity.builder().id(1L).role(DashboardRole.ADMIN).active(true).build());
        when(issues.hasActiveIssues(any(), any())).thenReturn(false);
        DashboardProjectReviewServiceImpl service = new DashboardProjectReviewServiceImpl(
                projects, currentUser, issues, history, mock(ContentVersionService.class), publisher,
                mock(BuilderRepository.class), mock(ProjectPublicVisibilityPolicy.class));

        service.approve(27L, null);

        verify(publisher).publish(27L, ProjectCacheEvictionReason.VISIBILITY_CHANGED);
    }

    @Test
    void directSnapshotRecalculationPublishesAnalyticsEviction() {
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectMeterSnapshotRepository snapshots = mock(ProjectMeterSnapshotRepository.class);
        ProjectConstructionStageRepository stages = mock(ProjectConstructionStageRepository.class);
        ProjectComplianceItemRepository compliance = mock(ProjectComplianceItemRepository.class);
        ProjectAmenityProgressRepository amenities = mock(ProjectAmenityProgressRepository.class);
        ProjectLocationScoreRepository locations = mock(ProjectLocationScoreRepository.class);
        ProjectCostBreakdownRepository costs = mock(ProjectCostBreakdownRepository.class);
        ProjectPriceHistoryRepository prices = mock(ProjectPriceHistoryRepository.class);
        ProjectPublicCacheEvictionPublisher publisher = mock(ProjectPublicCacheEvictionPublisher.class);
        ProjectEntity project = ProjectEntity.builder().id(27L).build();
        when(projects.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project));
        when(stages.findByProjectIdOrderByDisplayOrderAscIdAsc(27L)).thenReturn(List.of());
        when(compliance.findByProjectIdOrderByDisplayOrderAscIdAsc(27L)).thenReturn(List.of());
        when(amenities.findByProjectIdAndActiveTrueAndPublicVisibleTrueOrderByCategoryDisplayOrderAscDisplayOrderAscIdAsc(27L))
                .thenReturn(List.of());
        when(locations.findByProjectId(27L)).thenReturn(Optional.empty());
        when(costs.findByProjectId(27L)).thenReturn(Optional.empty());
        when(prices.findByProjectIdOrderByDisplayOrderAscIdAsc(27L)).thenReturn(List.of());
        when(snapshots.findByProjectId(27L)).thenReturn(Optional.empty());
        ProjectMeterSnapshotRecalculationServiceImpl service =
                new ProjectMeterSnapshotRecalculationServiceImpl(
                        projects, snapshots, stages, compliance, amenities, locations, costs, prices, publisher);

        service.recalculateSnapshot(27L);

        verify(publisher).publish(27L, ProjectCacheEvictionReason.ANALYTICS_CHANGED);
    }
}
