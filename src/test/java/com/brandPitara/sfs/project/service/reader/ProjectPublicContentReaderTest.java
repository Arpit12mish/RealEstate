package com.brandPitara.sfs.project.service.reader;

import com.brandPitara.sfs.project.entity.ProjectConnectivityEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectMasterPlanEntity;
import com.brandPitara.sfs.project.repository.ProjectConnectivityPlaceRepository;
import com.brandPitara.sfs.project.repository.ProjectConnectivityRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectMasterPlanRepository;
import com.brandPitara.sfs.project.service.model.ProjectPublicCoreData;
import com.brandPitara.sfs.project.service.model.ProjectPublicSectionFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectPublicContentReaderTest {

    @Mock private ProjectFloorPlanRepository floorPlans;
    @Mock private ProjectConnectivityRepository connectivity;
    @Mock private ProjectConnectivityPlaceRepository connectivityPlaces;
    @Mock private ProjectMasterPlanRepository masterPlans;

    private ProjectPublicContentReader reader;

    @BeforeEach
    void setUp() {
        reader = new ProjectPublicContentReader(
                floorPlans, connectivity, connectivityPlaces, masterPlans);
    }

    @Test
    void readsDetachedContentWithoutReloadingTheAlreadyValidatedProject() {
        ProjectPublicCoreData project = project();
        when(floorPlans.findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(27L))
                .thenReturn(List.of(ProjectFloorPlanEntity.builder()
                        .id(7L).title("3 BHK").imageUrl("floor.jpg")
                        .active(true).deleted(false).build()));
        when(connectivity.findByProjectIdAndActiveTrueAndDeletedFalse(27L))
                .thenReturn(Optional.of(ProjectConnectivityEntity.builder()
                        .active(true).deleted(false).mapImageUrl("map.jpg").build()));
        when(connectivityPlaces
                .findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(27L))
                .thenReturn(List.of());
        when(masterPlans.findByProjectIdAndActiveTrueAndDeletedFalse(27L))
                .thenReturn(Optional.of(ProjectMasterPlanEntity.builder()
                        .active(true).deleted(false).masterPlanImageUrl("master.jpg").build()));

        var result = reader.readAfterVisibilityCheck(project);

        assertThat(result.floorPlans()).singleElement().satisfies(plan ->
                assertThat(plan.getId()).isEqualTo(7L));
        assertThat(result.connectivity().getProjectId()).isEqualTo(27L);
        assertThat(result.connectivity().getProjectLatitude()).isEqualTo(28.45);
        assertThat(result.connectivity().getProjectLongitude()).isEqualTo(77.02);
        assertThat(result.connectivity().getProjectAddress()).isEqualTo("Sector 27");
        assertThat(result.masterPlan().getImageUrl()).isEqualTo("master.jpg");
        assertThat(result.failedSections()).isEmpty();
        assertThat(ProjectPublicContentReader.class.getDeclaredFields())
                .noneMatch(field -> field.getType().getSimpleName().equals("ProjectRepository"));
    }

    @Test
    void preservesMandatoryFloorPlansAndMarksOptionalFailures() {
        ProjectPublicCoreData project = project();
        when(floorPlans.findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(27L))
                .thenReturn(List.of());
        when(connectivity.findByProjectIdAndActiveTrueAndDeletedFalse(27L))
                .thenThrow(new IllegalStateException("connectivity unavailable"));
        when(masterPlans.findByProjectIdAndActiveTrueAndDeletedFalse(27L))
                .thenThrow(new IllegalStateException("master plan unavailable"));

        var result = reader.readAfterVisibilityCheck(project);

        assertThat(result.floorPlans()).isEmpty();
        assertThat(result.connectivity()).isNull();
        assertThat(result.masterPlan()).isNull();
        assertThat(result.failedSections()).containsExactlyInAnyOrder(
                ProjectPublicSectionFailure.CONNECTIVITY,
                ProjectPublicSectionFailure.MASTER_PLAN);
    }

    private ProjectPublicCoreData project() {
        return ProjectPublicCoreData.builder()
                .id(27L)
                .latitude(28.45)
                .longitude(77.02)
                .addressLine("Sector 27")
                .propertyTypes(Set.of())
                .glimpses(List.of())
                .failedSections(Set.of())
                .build();
    }
}
