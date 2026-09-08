package com.brandPitara.sfs.dashboard.projectmeter.service.impl;

import com.brandPitara.sfs.cdn.event.ProjectCacheEvictionReason;
import com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionPublisher;
import com.brandPitara.sfs.dashboard.projectmeter.dto.DashboardProjectAmenityRequest;
import com.brandPitara.sfs.dashboard.projectmeter.dto.DashboardProjectPriceInsightsRequest;
import com.brandPitara.sfs.dashboard.validator.DashboardProjectMeterValidator;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.projectmeter.entity.ProjectAmenityProgressEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import com.brandPitara.sfs.projectmeter.repository.*;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterSnapshotRecalculationService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DashboardProjectMeterEvictionTest {

    @Test
    void amenityWritePublishesAmenityEviction() {
        Fixture fixture = new Fixture();
        when(fixture.projects.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project()));
        when(fixture.amenities.save(any(ProjectAmenityProgressEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DashboardProjectAmenityRequest request = new DashboardProjectAmenityRequest();
        request.setAmenityCode("POOL");
        request.setAmenityLabel("Pool");
        fixture.service().createAmenity(27L, request);

        verify(fixture.publisher).publish(27L, ProjectCacheEvictionReason.AMENITIES_CHANGED);
    }

    @Test
    void priceInsightWritePublishesAnalyticsEviction() {
        Fixture fixture = new Fixture();
        ProjectMeterSnapshotEntity snapshot = ProjectMeterSnapshotEntity.builder().project(project()).build();
        when(fixture.projects.findByIdAndDeletedFalse(27L)).thenReturn(Optional.of(project()));
        when(fixture.snapshots.findByProjectId(27L)).thenReturn(Optional.of(snapshot));

        DashboardProjectPriceInsightsRequest request = new DashboardProjectPriceInsightsRequest();
        request.setAverageAreaPrice(15000L);
        fixture.service().updatePriceInsights(27L, request);

        verify(fixture.publisher).publish(27L, ProjectCacheEvictionReason.ANALYTICS_CHANGED);
    }

    private ProjectEntity project() {
        return ProjectEntity.builder().id(27L).active(true).published(true).deleted(false).build();
    }

    private static final class Fixture {
        final ProjectRepository projects = mock(ProjectRepository.class);
        final ProjectMeterSnapshotRepository snapshots = mock(ProjectMeterSnapshotRepository.class);
        final ProjectConstructionStageRepository stages = mock(ProjectConstructionStageRepository.class);
        final ProjectComplianceItemRepository compliance = mock(ProjectComplianceItemRepository.class);
        final ProjectAmenityProgressRepository amenities = mock(ProjectAmenityProgressRepository.class);
        final ProjectPriceHistoryRepository prices = mock(ProjectPriceHistoryRepository.class);
        final ProjectPaymentMilestoneRepository milestones = mock(ProjectPaymentMilestoneRepository.class);
        final ProjectCostBreakdownRepository costs = mock(ProjectCostBreakdownRepository.class);
        final ProjectLandUtilizationRepository land = mock(ProjectLandUtilizationRepository.class);
        final ProjectLocationScoreRepository location = mock(ProjectLocationScoreRepository.class);
        final ProjectMeterSnapshotRecalculationService recalculation = mock(ProjectMeterSnapshotRecalculationService.class);
        final DashboardProjectMeterValidator validator = mock(DashboardProjectMeterValidator.class);
        final ProjectPublicCacheEvictionPublisher publisher = mock(ProjectPublicCacheEvictionPublisher.class);

        DashboardProjectMeterWriteServiceImpl service() {
            return new DashboardProjectMeterWriteServiceImpl(
                    projects, snapshots, stages, compliance, amenities, prices, milestones,
                    costs, land, location, recalculation, validator, publisher);
        }
    }
}
