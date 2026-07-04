package com.brandPitara.sfs.projectcompare.service.impl;

import com.brandPitara.sfs.buildercredibility.service.BuilderCredibilityService;
import com.brandPitara.sfs.builder.entity.BuilderEntity;
import com.brandPitara.sfs.dashboard.common.enums.ReviewStatus;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.exception.NotFoundException;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.repository.ProjectConnectivityPlaceRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import com.brandPitara.sfs.project.repository.ProjectRepository;
import com.brandPitara.sfs.projectcompare.builder.ProjectComparisonSectionBuilder;
import com.brandPitara.sfs.projectcompare.dto.request.ProjectComparisonRequest;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonProjectHeader;
import com.brandPitara.sfs.projectcompare.dto.response.ComparisonSection;
import com.brandPitara.sfs.projectcompare.dto.response.ProjectComparisonResponse;
import com.brandPitara.sfs.projectcompare.enums.ComparisonSectionKey;
import com.brandPitara.sfs.projectmeter.repository.ProjectAmenityProgressRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectComplianceItemRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectConstructionStageRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectLocationScoreRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectComparisonServiceImplTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMeterSnapshotRepository snapshotRepository;
    @Mock private ProjectLocationScoreRepository locationScoreRepository;
    @Mock private ProjectAmenityProgressRepository amenityRepository;
    @Mock private ProjectComplianceItemRepository complianceRepository;
    @Mock private ProjectConstructionStageRepository stageRepository;
    @Mock private ProjectFloorPlanRepository floorPlanRepository;
    @Mock private ProjectConnectivityPlaceRepository connectivityPlaceRepository;
    @Mock private ProjectMediaRepository mediaRepository;
    @Mock private BuilderCredibilityService builderCredibilityService;
    @Mock private ProjectComparisonSectionBuilder sectionBuilder;

    @InjectMocks private ProjectComparisonServiceImpl service;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ProjectEntity project(long id, String name) {
        ProjectEntity p = new ProjectEntity();
        p.setId(id);
        p.setName(name);
        p.setSlug("slug-" + id);
        p.setReviewStatus(ReviewStatus.APPROVED);
        BuilderEntity builder = new BuilderEntity();
        builder.setId(100L);
        builder.setName("Builder");
        p.setBuilder(builder);
        CityEntity city = new CityEntity();
        city.setId(1L);
        city.setName("Gurugram");
        p.setCity(city);
        return p;
    }

    private void stubEmptyBatchRepos(List<Long> ids) {
        when(snapshotRepository.findByProjectIdIn(ids)).thenReturn(List.of());
        when(locationScoreRepository.findByProjectIdIn(ids)).thenReturn(List.of());
        when(amenityRepository
                .findByProjectIdInAndActiveTrueAndPublicVisibleTrueOrderByProjectIdAscCategoryDisplayOrderAscDisplayOrderAscIdAsc(ids))
                .thenReturn(List.of());
        when(complianceRepository
                .findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(ids))
                .thenReturn(List.of());
        when(stageRepository
                .findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(ids))
                .thenReturn(List.of());
        when(floorPlanRepository
                .findByProjectIdInAndActiveTrueAndDeletedFalseOrderByProjectIdAscSortOrderAscIdAsc(ids))
                .thenReturn(List.of());
        when(connectivityPlaceRepository
                .findByProjectIdInAndActiveTrueAndDeletedFalseOrderByProjectIdAscCategoryAscSortOrderAscIdAsc(ids))
                .thenReturn(List.of());
        when(mediaRepository.findActiveByProjectIds(ids)).thenReturn(List.of());
    }

    private ComparisonSection stubSection(ComparisonSectionKey key) {
        return ComparisonSection.builder()
                .sectionKey(key)
                .sectionTitle(key.toTitle())
                .displayOrder(key.defaultOrder())
                .initiallyExpanded(key.initiallyExpanded())
                .rows(List.of())
                .build();
    }

    private void stubSectionBuilder(List<ProjectEntity> projects) {
        lenient().when(sectionBuilder.buildHeaders(any(), any())).thenReturn(
                projects.stream().map(p -> ComparisonProjectHeader.builder()
                        .projectId(p.getId()).name(p.getName()).slug(p.getSlug()).build()).toList());
        lenient().when(sectionBuilder.buildOverview(any(), any())).thenReturn(stubSection(ComparisonSectionKey.OVERVIEW));
        lenient().when(sectionBuilder.buildPrice(any(), any())).thenReturn(stubSection(ComparisonSectionKey.PRICE));
        lenient().when(sectionBuilder.buildUnits(any(), any())).thenReturn(stubSection(ComparisonSectionKey.UNITS));
        lenient().when(sectionBuilder.buildAmenities(any(), any(), any())).thenReturn(stubSection(ComparisonSectionKey.AMENITIES));
        lenient().when(sectionBuilder.buildLocation(any(), any(), any())).thenReturn(stubSection(ComparisonSectionKey.LOCATION));
        lenient().when(sectionBuilder.buildConstruction(any(), any(), any())).thenReturn(stubSection(ComparisonSectionKey.CONSTRUCTION));
        lenient().when(sectionBuilder.buildCompliance(any(), any())).thenReturn(stubSection(ComparisonSectionKey.COMPLIANCE));
        lenient().when(sectionBuilder.buildBuilder(any(), any())).thenReturn(stubSection(ComparisonSectionKey.BUILDER));
        lenient().when(sectionBuilder.buildMeter(any(), any(), any(), any())).thenReturn(stubSection(ComparisonSectionKey.METER));
    }

    // ─── Tests ────────────────────────────────────────────────────────────────

    @Test
    void valid2ProjectComparison_returnsAllSectionsAndHeaders() {
        List<Long> ids = List.of(1L, 2L);
        ProjectEntity p1 = project(1L, "Alpha");
        ProjectEntity p2 = project(2L, "Beta");
        List<ProjectEntity> projects = List.of(p1, p2);

        when(projectRepository.findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(ids, ReviewStatus.APPROVED))
                .thenReturn(projects);
        stubEmptyBatchRepos(ids);
        stubSectionBuilder(projects);

        ProjectComparisonRequest req = new ProjectComparisonRequest();
        req.setProjectIds(ids);

        ProjectComparisonResponse response = service.compare(req);

        assertThat(response.getProjects()).hasSize(2);
        assertThat(response.getSections()).hasSize(ComparisonSectionKey.values().length);
    }

    @Test
    void responseOrderMatchesRequestOrder() {
        List<Long> ids = List.of(2L, 1L);  // 2 first, 1 second
        ProjectEntity p1 = project(1L, "Alpha");
        ProjectEntity p2 = project(2L, "Beta");
        // Repository returns them in db order (1 before 2)
        when(projectRepository.findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(ids, ReviewStatus.APPROVED))
                .thenReturn(List.of(p1, p2));
        stubEmptyBatchRepos(ids);

        List<ProjectEntity> requestOrder = List.of(p2, p1);
        stubSectionBuilder(requestOrder);

        ProjectComparisonRequest req = new ProjectComparisonRequest();
        req.setProjectIds(ids);

        ProjectComparisonResponse response = service.compare(req);

        assertThat(response.getProjects().get(0).getProjectId()).isEqualTo(2L);
        assertThat(response.getProjects().get(1).getProjectId()).isEqualTo(1L);
    }

    @Test
    void duplicateIds_throwsIllegalArgument() {
        ProjectComparisonRequest req = new ProjectComparisonRequest();
        req.setProjectIds(List.of(1L, 1L));

        assertThatThrownBy(() -> service.compare(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate project ID");
    }

    @Test
    void nullIdInList_throwsIllegalArgument() {
        ProjectComparisonRequest req = new ProjectComparisonRequest();
        req.setProjectIds(java.util.Arrays.asList(1L, null));

        assertThatThrownBy(() -> service.compare(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void lessThan2Ids_throwsIllegalArgument() {
        ProjectComparisonRequest req = new ProjectComparisonRequest();
        req.setProjectIds(List.of(1L));

        assertThatThrownBy(() -> service.compare(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least 2");
    }

    @Test
    void moreThan4Ids_throwsIllegalArgument() {
        ProjectComparisonRequest req = new ProjectComparisonRequest();
        req.setProjectIds(List.of(1L, 2L, 3L, 4L, 5L));

        assertThatThrownBy(() -> service.compare(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At most 4");
    }

    @Test
    void unpublishedProject_throwsNotFoundException() {
        List<Long> ids = List.of(1L, 2L);
        // repo returns only 1 project (project 2 is not public)
        when(projectRepository.findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(ids, ReviewStatus.APPROVED))
                .thenReturn(List.of(project(1L, "Alpha")));

        ProjectComparisonRequest req = new ProjectComparisonRequest();
        req.setProjectIds(ids);

        assertThatThrownBy(() -> service.compare(req))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("2");
    }

    @Test
    void sectionKeyFilter_returnsOnlyRequestedSections() {
        List<Long> ids = List.of(1L, 2L);
        List<ProjectEntity> projects = List.of(project(1L, "A"), project(2L, "B"));

        when(projectRepository.findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(ids, ReviewStatus.APPROVED))
                .thenReturn(projects);
        stubEmptyBatchRepos(ids);
        stubSectionBuilder(projects);

        ProjectComparisonRequest req = new ProjectComparisonRequest();
        req.setProjectIds(ids);
        req.setSectionKeys(List.of(ComparisonSectionKey.OVERVIEW, ComparisonSectionKey.PRICE));

        ProjectComparisonResponse response = service.compare(req);

        assertThat(response.getSections()).hasSize(2);
        List<ComparisonSectionKey> keys = response.getSections().stream()
                .map(ComparisonSection::getSectionKey).toList();
        assertThat(keys).containsExactlyInAnyOrder(ComparisonSectionKey.OVERVIEW, ComparisonSectionKey.PRICE);
    }

    @Test
    void sectionsAreReturnedInDisplayOrder() {
        List<Long> ids = List.of(1L, 2L);
        List<ProjectEntity> projects = List.of(project(1L, "A"), project(2L, "B"));

        when(projectRepository.findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(ids, ReviewStatus.APPROVED))
                .thenReturn(projects);
        stubEmptyBatchRepos(ids);
        stubSectionBuilder(projects);

        ProjectComparisonRequest req = new ProjectComparisonRequest();
        req.setProjectIds(ids);
        // request in reverse order
        req.setSectionKeys(List.of(ComparisonSectionKey.PRICE, ComparisonSectionKey.OVERVIEW));

        ProjectComparisonResponse response = service.compare(req);

        List<Integer> orders = response.getSections().stream()
                .map(ComparisonSection::getDisplayOrder).toList();
        assertThat(orders).isSorted();
        assertThat(response.getSections().get(0).getSectionKey()).isEqualTo(ComparisonSectionKey.OVERVIEW);
    }

    @Test
    void duplicateSectionKeyInRequest_deduped() {
        List<Long> ids = List.of(1L, 2L);
        List<ProjectEntity> projects = List.of(project(1L, "A"), project(2L, "B"));

        when(projectRepository.findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(ids, ReviewStatus.APPROVED))
                .thenReturn(projects);
        stubEmptyBatchRepos(ids);
        stubSectionBuilder(projects);

        ProjectComparisonRequest req = new ProjectComparisonRequest();
        req.setProjectIds(ids);
        req.setSectionKeys(List.of(ComparisonSectionKey.OVERVIEW, ComparisonSectionKey.OVERVIEW));

        ProjectComparisonResponse response = service.compare(req);

        assertThat(response.getSections()).hasSize(1);
    }

    @Test
    void noSectionKeys_returnsAllSections() {
        List<Long> ids = List.of(1L, 2L);
        List<ProjectEntity> projects = List.of(project(1L, "A"), project(2L, "B"));

        when(projectRepository.findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(ids, ReviewStatus.APPROVED))
                .thenReturn(projects);
        stubEmptyBatchRepos(ids);
        stubSectionBuilder(projects);

        ProjectComparisonRequest req = new ProjectComparisonRequest();
        req.setProjectIds(ids);
        // sectionKeys null → all sections

        ProjectComparisonResponse response = service.compare(req);

        assertThat(response.getSections()).hasSize(ComparisonSectionKey.values().length);
    }

    @Test
    void missingMeterData_doesNotFailComparison() {
        List<Long> ids = List.of(1L, 2L);
        List<ProjectEntity> projects = List.of(project(1L, "A"), project(2L, "B"));

        when(projectRepository.findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(ids, ReviewStatus.APPROVED))
                .thenReturn(projects);
        stubEmptyBatchRepos(ids);  // all repos return empty → all data missing
        stubSectionBuilder(projects);

        ProjectComparisonRequest req = new ProjectComparisonRequest();
        req.setProjectIds(ids);
        req.setSectionKeys(List.of(ComparisonSectionKey.METER));

        // should not throw
        ProjectComparisonResponse response = service.compare(req);
        assertThat(response).isNotNull();
    }

    @Test
    void builderCredibilityException_doesNotFailComparison() {
        List<Long> ids = List.of(1L, 2L);
        List<ProjectEntity> projects = List.of(project(1L, "A"), project(2L, "B"));

        when(projectRepository.findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(ids, ReviewStatus.APPROVED))
                .thenReturn(projects);
        stubEmptyBatchRepos(ids);
        when(builderCredibilityService.publicGetCredibilitySummary(100L))
                .thenThrow(new RuntimeException("credibility service down"));
        stubSectionBuilder(projects);

        ProjectComparisonRequest req = new ProjectComparisonRequest();
        req.setProjectIds(ids);
        req.setSectionKeys(List.of(ComparisonSectionKey.BUILDER));

        // exception from credibility service must be swallowed
        ProjectComparisonResponse response = service.compare(req);
        assertThat(response).isNotNull();
    }

    @Test
    void valid4ProjectComparison_succeeds() {
        List<Long> ids = List.of(1L, 2L, 3L, 4L);
        List<ProjectEntity> projects = List.of(
                project(1L, "A"), project(2L, "B"), project(3L, "C"), project(4L, "D"));

        when(projectRepository.findByIdInAndPublishedTrueAndActiveTrueAndDeletedFalseAndReviewStatus(ids, ReviewStatus.APPROVED))
                .thenReturn(projects);
        stubEmptyBatchRepos(ids);
        stubSectionBuilder(projects);

        ProjectComparisonRequest req = new ProjectComparisonRequest();
        req.setProjectIds(ids);

        ProjectComparisonResponse response = service.compare(req);

        assertThat(response.getProjects()).hasSize(4);
    }
}
