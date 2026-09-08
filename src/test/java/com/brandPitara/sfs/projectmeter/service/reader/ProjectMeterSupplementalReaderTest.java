package com.brandPitara.sfs.projectmeter.service.reader;

import com.brandPitara.sfs.project.entity.ProjectConnectivityEntity;
import com.brandPitara.sfs.project.entity.ProjectConnectivityPlaceEntity;
import com.brandPitara.sfs.project.entity.ProjectEntity;
import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectMasterPlanEntity;
import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import com.brandPitara.sfs.project.enums.ProjectConnectivityType;
import com.brandPitara.sfs.project.enums.ProjectMediaType;
import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import com.brandPitara.sfs.project.repository.ProjectConnectivityPlaceRepository;
import com.brandPitara.sfs.project.repository.ProjectConnectivityRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectMasterPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectMediaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMeterSupplementalReaderTest {

    @Mock private ProjectMediaRepository projectMediaRepository;
    @Mock private ProjectMasterPlanRepository projectMasterPlanRepository;
    @Mock private ProjectFloorPlanRepository projectFloorPlanRepository;
    @Mock private ProjectConnectivityRepository projectConnectivityRepository;
    @Mock private ProjectConnectivityPlaceRepository projectConnectivityPlaceRepository;
    @InjectMocks private ProjectMeterSupplementalReader reader;

    @Test
    void loadsEachSupplementalSectionOnceWithoutProjectLookup() {
        ProjectEntity project = project();
        when(projectMediaRepository.findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdDesc(42L))
            .thenReturn(List.of(
                media(project, 1L, ProjectMediaType.IMAGE, "cover.jpg", 0),
                media(project, 2L, ProjectMediaType.BROCHURE_PDF, "brochure.pdf", 1)
            ));
        when(projectMasterPlanRepository.findByProjectIdAndActiveTrueAndDeletedFalse(42L))
            .thenReturn(Optional.of(ProjectMasterPlanEntity.builder()
                .project(project)
                .masterPlanImageUrl("master-plan.jpg")
                .active(true)
                .deleted(false)
                .build()));
        when(projectFloorPlanRepository
            .findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(42L))
            .thenReturn(List.of(
                floorPlan(project, 10L, UnitConfigurationType.BHK_2, 0),
                floorPlan(project, 11L, UnitConfigurationType.BHK_2, 1),
                floorPlan(project, 12L, UnitConfigurationType.BHK_3, 2)
            ));
        when(projectConnectivityRepository.findByProjectIdAndActiveTrueAndDeletedFalse(42L))
            .thenReturn(Optional.of(ProjectConnectivityEntity.builder()
                .project(project)
                .mapImageUrl("map.jpg")
                .active(true)
                .deleted(false)
                .build()));
        when(projectConnectivityPlaceRepository
            .findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(42L))
            .thenReturn(List.of(ProjectConnectivityPlaceEntity.builder()
                .id(20L)
                .project(project)
                .placeName("Metro")
                .placeType(ProjectConnectivityType.METRO)
                .latitude(28.46)
                .longitude(77.09)
                .distanceLabel("1 km")
                .distanceMeters(1_000)
                .sortOrder(0)
                .active(true)
                .deleted(false)
                .build()));

        var sections = reader.read(project);

        assertThat(sections.media().getCoverImageUrl()).isEqualTo("cover.jpg");
        assertThat(sections.media().getItems()).hasSize(1);
        assertThat(sections.brochureUrl()).isEqualTo("brochure.pdf");
        assertThat(sections.masterPlan().getImageUrl()).isEqualTo("master-plan.jpg");
        assertThat(sections.floorPlanGroups()).extracting(group -> group.getGroupKey())
            .containsExactly("BHK_2", "BHK_3");
        assertThat(sections.floorPlanGroups().get(0).getItems()).extracting(item -> item.getId())
            .containsExactly(10L, 11L);
        assertThat(sections.connectivity().getMapImageUrl()).isEqualTo("map.jpg");
        assertThat(sections.connectivity().getProjectLatitude()).isEqualTo(28.45);
        assertThat(sections.connectivity().getPlaces()).hasSize(1);
        verify(projectMediaRepository)
            .findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdDesc(42L);
        verify(projectMasterPlanRepository).findByProjectIdAndActiveTrueAndDeletedFalse(42L);
        verify(projectFloorPlanRepository)
            .findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(42L);
        verify(projectConnectivityRepository).findByProjectIdAndActiveTrueAndDeletedFalse(42L);
        verify(projectConnectivityPlaceRepository)
            .findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(42L);
    }

    @Test
    void missingOptionalRowsReturnNullMasterPlanAndEmptyCollections() {
        ProjectEntity project = project();
        when(projectMediaRepository.findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdDesc(42L))
            .thenReturn(List.of());
        when(projectMasterPlanRepository.findByProjectIdAndActiveTrueAndDeletedFalse(42L))
            .thenReturn(Optional.empty());
        when(projectFloorPlanRepository
            .findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(42L))
            .thenReturn(List.of());
        when(projectConnectivityRepository.findByProjectIdAndActiveTrueAndDeletedFalse(42L))
            .thenReturn(Optional.empty());
        when(projectConnectivityPlaceRepository
            .findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(42L))
            .thenReturn(List.of());

        var sections = reader.read(project);

        assertThat(sections.masterPlan()).isNull();
        assertThat(sections.floorPlanGroups()).isEmpty();
        assertThat(sections.connectivity()).isNotNull();
        assertThat(sections.connectivity().getPlaces()).isEmpty();
        assertThat(sections.connectivity().getProjectAddress()).isEqualTo("Address");
    }

    private ProjectEntity project() {
        return ProjectEntity.builder()
            .id(42L)
            .addressLine("Address")
            .latitude(28.45)
            .longitude(77.08)
            .build();
    }

    private ProjectMediaEntity media(
        ProjectEntity project,
        Long id,
        ProjectMediaType type,
        String url,
        int order
    ) {
        return ProjectMediaEntity.builder()
            .id(id)
            .project(project)
            .mediaType(type)
            .url(url)
            .sortOrder(order)
            .active(true)
            .deleted(false)
            .build();
    }

    private ProjectFloorPlanEntity floorPlan(
        ProjectEntity project,
        Long id,
        UnitConfigurationType type,
        int order
    ) {
        return ProjectFloorPlanEntity.builder()
            .id(id)
            .project(project)
            .title(type.toLabel())
            .imageUrl(type.name() + ".jpg")
            .unitConfigurationType(type)
            .sortOrder(order)
            .active(true)
            .deleted(false)
            .build();
    }
}
