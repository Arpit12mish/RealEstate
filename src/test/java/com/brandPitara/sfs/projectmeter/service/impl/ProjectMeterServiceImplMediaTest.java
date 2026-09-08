package com.brandPitara.sfs.projectmeter.service.impl;

import com.brandPitara.sfs.buildercredibility.service.BuilderCredibilityService;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.enums.ProjectMediaType;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.project.policy.ProjectPublicVisibilityPolicy;
import com.brandPitara.sfs.project.service.ProjectFavoriteService;
import com.brandPitara.sfs.projectmeter.repository.ProjectAmenityProgressRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectComplianceItemRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectConstructionStageRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectCostBreakdownRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectLandUtilizationRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectLocationScoreRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectPaymentMilestoneRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectPriceHistoryRepository;
import com.brandPitara.sfs.projectmeter.mapper.ProjectMeterMapper;
import com.brandPitara.sfs.projectmeter.mapper.ProjectAmenitiesAssembler;
import com.brandPitara.sfs.projectmeter.service.reader.ProjectMeterFavoriteReader;
import com.brandPitara.sfs.projectmeter.service.reader.ProjectMeterSupplementalReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMeterServiceImplMediaTest {

    private static final Long PROJECT_ID = 27L;

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
    @Mock private ProjectPublicVisibilityPolicy projectPublicVisibilityPolicy;
    @Mock private ProjectMeterSupplementalReader projectMeterSupplementalReader;
    @Mock private ProjectMeterFavoriteReader projectMeterFavoriteReader;
    @Spy private ProjectAmenitiesAssembler projectAmenitiesAssembler = new ProjectAmenitiesAssembler();

    @InjectMocks private ProjectMeterServiceImpl service;

    @BeforeEach
    void setUpPublicProjectAndEmptyMeterSections() {
        ProjectEntity project = ProjectEntity.builder()
            .id(PROJECT_ID)
            .name("M3M Jewel")
            .published(true)
            .active(true)
            .deleted(false)
            .reviewStatus(ReviewStatus.APPROVED)
            .build();

        when(projectRepository.findDetailByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectMeterSnapshotRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.empty());
        when(projectConstructionStageRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(PROJECT_ID))
            .thenReturn(List.of());
        when(projectMeterFavoriteReader.read(PROJECT_ID))
            .thenReturn(new ProjectMeterFavoriteReader.FavoriteState(false, 0L));
        when(projectComplianceItemRepository
            .findByProjectIdOrderByItemGroupAscDisplayOrderAscIdAsc(PROJECT_ID))
            .thenReturn(List.of());
        when(projectPriceHistoryRepository.findByProjectIdOrderByDisplayOrderAscIdAsc(PROJECT_ID)).thenReturn(List.of());
        when(projectPaymentMilestoneRepository.findByProjectIdAndActiveTrueOrderByDisplayOrderAscIdAsc(PROJECT_ID))
            .thenReturn(List.of());
        when(projectCostBreakdownRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.empty());
        when(projectLandUtilizationRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.empty());
        when(projectLocationScoreRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.empty());
        when(projectAmenityProgressRepository
            .findByProjectIdAndActiveTrueAndPublicVisibleTrueOrderByCategoryDisplayOrderAscDisplayOrderAscIdAsc(PROJECT_ID))
            .thenReturn(List.of());
    }

    @Test
    void publicGetMeterDetailReturnsOrderedMediaWithoutChangingExistingSections() {
        List<ProjectMediaEntity> media = List.of(
            image(101L, "https://cdn/m3m-cover.jpg", 0),
            image(102L, "https://cdn/m3m-gallery.jpg", 1)
        );
        when(projectMeterSupplementalReader.read(org.mockito.ArgumentMatchers.any(ProjectEntity.class)))
            .thenReturn(supplemental(ProjectMeterMapper.toMediaResponse(media)));

        var detail = service.publicGetMeterDetail(PROJECT_ID);

        assertThat(detail.getMedia().getCoverImageUrl()).isEqualTo("https://cdn/m3m-cover.jpg");
        assertThat(detail.getMedia().getItems()).extracting(item -> item.getId()).containsExactly(101L, 102L);
        assertThat(detail.getSummary()).isNotNull();
        assertThat(detail.getConstruction()).isNotNull();
        assertThat(detail.getLandLicense()).isNotNull();
        assertThat(detail.getApprovals()).isNotNull();
        assertThat(detail.getPriceInsights()).isNotNull();
        assertThat(detail.getAmenities()).isNotNull();
        assertThat(detail.getPropertyRates()).isEmpty();
        assertThat(detail.getPaymentPlan()).isEmpty();
    }

    @Test
    void publicGetMeterDetailReturnsEmptyMediaWithoutBreakingResponse() {
        when(projectMeterSupplementalReader.read(org.mockito.ArgumentMatchers.any(ProjectEntity.class)))
            .thenReturn(supplemental(ProjectMeterMapper.toMediaResponse(List.of())));

        var detail = service.publicGetMeterDetail(PROJECT_ID);

        assertThat(detail.getMedia().getCoverImageUrl()).isNull();
        assertThat(detail.getMedia().getItems()).isEmpty();
        assertThat(detail.getSummary()).isNotNull();
    }

    private ProjectMediaEntity image(Long id, String url, int sortOrder) {
        return ProjectMediaEntity.builder()
            .id(id)
            .mediaType(ProjectMediaType.IMAGE)
            .url(url)
            .sortOrder(sortOrder)
            .active(true)
            .deleted(false)
            .build();
    }

    private ProjectMeterSupplementalReader.SupplementalSections supplemental(
        com.brandPitara.sfs.projectmeter.dto.ProjectMeterMediaResponse media
    ) {
        return new ProjectMeterSupplementalReader.SupplementalSections(
            media,
            null,
            null,
            List.of(),
            null
        );
    }
}
