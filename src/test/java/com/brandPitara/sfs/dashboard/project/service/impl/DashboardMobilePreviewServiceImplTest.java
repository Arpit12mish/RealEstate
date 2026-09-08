package com.brandPitara.sfs.dashboard.project.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.project.dto.ProjectResponse;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.service.ProjectDetailComposer;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterDetailResponse;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import com.brandPitara.sfs.projectmeter.service.ProjectMeterService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DashboardMobilePreviewServiceImplTest {

    @Test
    void unpublishedBuilderProducesPreviewWithNullCredibilityAndWarnings() {
        ProjectRepository projects = mock(ProjectRepository.class);
        ProjectMediaRepository media = mock(ProjectMediaRepository.class);
        ProjectMeterSnapshotRepository snapshots = mock(ProjectMeterSnapshotRepository.class);
        ProjectMeterService meterService = mock(ProjectMeterService.class);
        ProjectDetailComposer composer = mock(ProjectDetailComposer.class);

        BuilderEntity builder = BuilderEntity.builder()
                .id(51L).name("Max Estates")
                .published(false).active(true).deleted(false).build();
        ProjectEntity project = ProjectEntity.builder()
                .id(130L).name("Project 130").builder(builder)
                .published(false).active(true).deleted(false)
                .reviewStatus(ReviewStatus.DRAFT).build();
        ProjectMeterDetailResponse meter = ProjectMeterDetailResponse.builder()
                .builderCredibility(null).build();
        ProjectResponse detail = ProjectResponse.builder()
                .floorPlanGroups(List.of()).build();

        when(projects.findDetailByIdAndDeletedFalse(130L)).thenReturn(Optional.of(project));
        when(media.findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdDesc(130L))
                .thenReturn(List.of());
        when(snapshots.findByProjectId(130L)).thenReturn(Optional.empty());
        when(meterService.dashboardGetMeterDetail(130L)).thenReturn(meter);
        when(composer.composeForPreview(eq(project), eq(List.of()), eq(meter))).thenReturn(detail);

        DashboardMobilePreviewServiceImpl service = new DashboardMobilePreviewServiceImpl(
                projects, media, snapshots, meterService, composer,
                new ProjectPublicVisibilityPolicy());

        var response = service.getPreview(130L);

        assertThat(response.getMeter().getBuilderCredibility()).isNull();
        assertThat(response.getMeta().getWarnings()).contains(
                "Builder is not published",
                "Project cannot be publicly visible until the builder is published",
                "Builder credibility data is not available");
        verify(meterService).dashboardGetMeterDetail(130L);
    }
}
