package com.brandPitara.sfs.projectmeter.service.impl;

import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.buildercredibility.service.BuilderCredibilityService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import com.brandPitara.sfs.projectmeter.entity.ProjectComplianceItemEntity;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceGroup;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceStatus;
import com.brandPitara.sfs.projectmeter.repository.ProjectAmenityProgressRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectComplianceItemRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectConstructionStageRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectCostBreakdownRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectLandUtilizationRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectLocationScoreRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectPaymentMilestoneRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectPriceHistoryRepository;
import com.brandPitara.sfs.projectmeter.dto.ProjectMeterMediaResponse;
import com.brandPitara.sfs.projectmeter.mapper.ProjectAmenitiesAssembler;
import com.brandPitara.sfs.projectmeter.service.reader.ProjectMeterFavoriteReader;
import com.brandPitara.sfs.projectmeter.service.reader.ProjectMeterSupplementalReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMeterServiceImplTest {

    private static final Long PROJECT_ID = 42L;
    private static final Long BUILDER_ID = 7L;

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMeterSnapshotRepository projectMeterSnapshotRepository;
    @Mock private ProjectConstructionStageRepository projectConstructionStageRepository;
    @Mock private ProjectComplianceItemRepository projectComplianceItemRepository;
    @Mock private ProjectPriceHistoryRepository projectPriceHistoryRepository;
    @Mock private ProjectPaymentMilestoneRepository projectPaymentMilestoneRepository;
    @Mock private ProjectCostBreakdownRepository projectCostBreakdownRepository;
    @Mock private ProjectLandUtilizationRepository projectLandUtilizationRepository;
    @Mock private ProjectLocationScoreRepository projectLocationScoreRepository;
    @Mock private ProjectAmenityProgressRepository projectAmenityProgressRepository;
    @Mock private ProjectMediaRepository projectMediaRepository;
    @Mock private BuilderCredibilityService builderCredibilityService;
    @Mock private ProjectFavoriteService projectFavoriteService;
    @Spy private ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy = new ProjectPublicVisibilityPolicy();
    @Mock private ProjectMeterSupplementalReader projectMeterSupplementalReader;
    @Mock private ProjectMeterFavoriteReader projectMeterFavoriteReader;
    @Spy private ProjectAmenitiesAssembler projectAmenitiesAssembler = new ProjectAmenitiesAssembler();

    @InjectMocks private ProjectMeterServiceImpl service;

    @Test
    void publicGetMeterDetailLoadsComplianceOnceAndPartitionsRows() {
        stubPublicMeterDependencies();
        when(projectComplianceItemRepository
            .findByProjectIdOrderByItemGroupAscDisplayOrderAscIdAsc(PROJECT_ID))
            .thenReturn(List.of(
                complianceItem(11L, ProjectComplianceGroup.LAND_LICENSE, "LAND_TITLE", 1),
                complianceItem(12L, ProjectComplianceGroup.APPROVAL_NOC, "FIRE_NOC", 1),
                complianceItem(13L, ProjectComplianceGroup.LEGAL_TITLE, "COMMENCEMENT", 1)
            ));

        var response = service.publicGetMeterDetail(PROJECT_ID);

        assertThat(response.getLandLicense().getItems()).hasSize(1);
        assertThat(response.getLandLicense().getItems().get(0).getItemGroup())
            .isEqualTo(ProjectComplianceGroup.LAND_LICENSE);
        assertThat(response.getLandLicense().getItems().get(0).getItemKey())
            .isEqualTo("LAND_TITLE");
        assertThat(response.getApprovals().getItems()).hasSize(1);
        assertThat(response.getApprovals().getItems().get(0).getItemGroup())
            .isEqualTo(ProjectComplianceGroup.APPROVAL_NOC);
        assertThat(response.getApprovals().getItems().get(0).getItemKey())
            .isEqualTo("FIRE_NOC");

        // Regression guard: every group must reach the client, not just
        // LAND_LICENSE/APPROVAL_NOC — LEGAL_TITLE (and RERA/UTILITY/
        // OCCUPANCY/etc.) used to be silently dropped here.
        assertThat(response.getComplianceGroups()).hasSize(3);
        assertThat(response.getComplianceGroups())
            .extracting("group")
            .containsExactly("LEGAL_TITLE", "LAND_LICENSE", "APPROVAL_NOC");
        var legalTitleGroup = response.getComplianceGroups().get(0);
        assertThat(legalTitleGroup.getGroupLabel()).isEqualTo("Legal Title");
        assertThat(legalTitleGroup.getItems()).hasSize(1);
        assertThat(legalTitleGroup.getItems().get(0).getItemKey()).isEqualTo("COMMENCEMENT");

        verifyCombinedComplianceLookupOnly();
        verify(projectRepository, times(1)).findDetailByIdAndDeletedFalse(PROJECT_ID);
        verify(projectRepository, never()).findByIdAndDeletedFalse(PROJECT_ID);
    }

    @Test
    void publicGetMeterDetailReturnsNonNullEmptyComplianceSections() {
        stubPublicMeterDependencies();
        when(projectComplianceItemRepository
            .findByProjectIdOrderByItemGroupAscDisplayOrderAscIdAsc(PROJECT_ID))
            .thenReturn(List.of());

        var response = service.publicGetMeterDetail(PROJECT_ID);

        assertThat(response.getLandLicense()).isNotNull();
        assertThat(response.getLandLicense().getItems()).isNotNull().isEmpty();
        assertThat(response.getApprovals()).isNotNull();
        assertThat(response.getApprovals().getItems()).isNotNull().isEmpty();
        assertThat(response.getComplianceGroups()).isNotNull().isEmpty();
        verifyCombinedComplianceLookupOnly();
    }

    @Test
    void dashboardGetMeterDetailUsesCombinedComplianceLookup() {
        stubDashboardMeterDependencies();
        when(projectComplianceItemRepository
            .findByProjectIdOrderByItemGroupAscDisplayOrderAscIdAsc(PROJECT_ID))
            .thenReturn(List.of(
                complianceItem(21L, ProjectComplianceGroup.LAND_LICENSE, "LAND_USE", 1),
                complianceItem(22L, ProjectComplianceGroup.APPROVAL_NOC, "AIRPORT_NOC", 1)
            ));

        var response = service.dashboardGetMeterDetail(PROJECT_ID);

        assertThat(response.getLandLicense().getItems()).hasSize(1);
        assertThat(response.getLandLicense().getItems().get(0).getItemGroup())
            .isEqualTo(ProjectComplianceGroup.LAND_LICENSE);
        assertThat(response.getApprovals().getItems()).hasSize(1);
        assertThat(response.getApprovals().getItems().get(0).getItemGroup())
            .isEqualTo(ProjectComplianceGroup.APPROVAL_NOC);
        verifyCombinedComplianceLookupOnly();
        verifyNoInteractions(projectMediaRepository);
    }

    @Test
    void dashboardGetMeterDetailSkipsCredibilityProxyForUnpublishedBuilder() {
        stubDashboardMeterDependencies();
        when(projectComplianceItemRepository
            .findByProjectIdOrderByItemGroupAscDisplayOrderAscIdAsc(PROJECT_ID))
            .thenReturn(List.of());

        ProjectEntity project = project();
        project.getBuilder().setPublished(false);
        when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));

        var response = service.dashboardGetMeterDetail(PROJECT_ID);

        assertThat(response.getBuilderCredibility()).isNull();
        verifyNoInteractions(builderCredibilityService);
    }

    @Test
    void publicGetMeterDetailDoesNotSwallowCredibilityInfrastructureFailure() {
        stubPublicMeterDependencies();
        when(projectComplianceItemRepository
            .findByProjectIdOrderByItemGroupAscDisplayOrderAscIdAsc(PROJECT_ID))
            .thenReturn(List.of());
        when(builderCredibilityService.publicGetCredibilitySummaryForLoadedBuilder(any(BuilderEntity.class)))
            .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("database unavailable"));

        assertThatThrownBy(() -> service.publicGetMeterDetail(PROJECT_ID))
            .isInstanceOf(org.springframework.dao.DataAccessResourceFailureException.class)
            .hasMessage("database unavailable");
    }

    private void stubPublicMeterDependencies() {
        stubCommonMeterDependencies();
        when(projectRepository.findDetailByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(projectMeterSupplementalReader.read(any(ProjectEntity.class))).thenReturn(emptySupplemental());
        when(projectMeterFavoriteReader.read(PROJECT_ID))
            .thenReturn(new ProjectMeterFavoriteReader.FavoriteState(false, 0L));
    }

    private void stubDashboardMeterDependencies() {
        stubCommonMeterDependencies();
        when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project()));
    }

    private void stubCommonMeterDependencies() {
        when(projectMeterSnapshotRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.empty());
        when(projectConstructionStageRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(PROJECT_ID))
            .thenReturn(List.of());
        when(projectPriceHistoryRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(PROJECT_ID))
            .thenReturn(List.of());
        when(projectPaymentMilestoneRepository
            .findByProjectIdAndActiveTrueOrderByDisplayOrderAscIdAsc(PROJECT_ID))
            .thenReturn(List.of());
        when(projectCostBreakdownRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.empty());
        when(projectLandUtilizationRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.empty());
        when(projectLocationScoreRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.empty());
        when(projectAmenityProgressRepository
            .findByProjectIdAndActiveTrueAndPublicVisibleTrueOrderByCategoryDisplayOrderAscDisplayOrderAscIdAsc(PROJECT_ID))
            .thenReturn(List.of());
    }

    private ProjectMeterSupplementalReader.SupplementalSections emptySupplemental() {
        return new ProjectMeterSupplementalReader.SupplementalSections(
            ProjectMeterMediaResponse.builder().items(List.of()).build(),
            null,
            null,
            List.of(),
            null
        );
    }

    private void verifyCombinedComplianceLookupOnly() {
        verify(projectComplianceItemRepository, times(1))
            .findByProjectIdOrderByItemGroupAscDisplayOrderAscIdAsc(PROJECT_ID);
        verify(projectComplianceItemRepository, never())
            .findByProjectIdAndItemGroupOrderByDisplayOrderAscIdAsc(
                anyLong(),
                any(ProjectComplianceGroup.class)
            );
    }

    private ProjectEntity project() {
        return ProjectEntity.builder()
            .id(PROJECT_ID)
            .name("Project 42")
            .builder(BuilderEntity.builder()
                .id(BUILDER_ID)
                .name("Builder 7")
                .published(true)
                .active(true)
                .deleted(false)
                .build())
            .published(true)
            .active(true)
            .deleted(false)
            .reviewStatus(ReviewStatus.APPROVED)
            .build();
    }

    private ProjectComplianceItemEntity complianceItem(
        Long id,
        ProjectComplianceGroup group,
        String key,
        int displayOrder
    ) {
        return ProjectComplianceItemEntity.builder()
            .id(id)
            .project(project())
            .itemGroup(group)
            .itemKey(key)
            .itemLabel(key)
            .status(ProjectComplianceStatus.VERIFIED)
            .displayOrder(displayOrder)
            .verified(true)
            .build();
    }
}
