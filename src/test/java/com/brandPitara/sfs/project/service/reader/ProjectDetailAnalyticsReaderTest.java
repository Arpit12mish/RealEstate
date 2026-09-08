package com.brandPitara.sfs.project.service.reader;

import com.brandPitara.sfs.projectmeter.dto.ProjectAmenitiesResponse;
import com.brandPitara.sfs.projectmeter.entity.ProjectAmenityProgressEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import com.brandPitara.sfs.projectmeter.mapper.ProjectAmenitiesAssembler;
import com.brandPitara.sfs.projectmeter.repository.ProjectAmenityProgressRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectDetailAnalyticsReaderTest {

    @Test
    void loadsOnlySnapshotAndPublicAmenitiesAndReturnsDetachedValues() {
        ProjectMeterSnapshotRepository snapshots = mock(ProjectMeterSnapshotRepository.class);
        ProjectAmenityProgressRepository amenities = mock(ProjectAmenityProgressRepository.class);
        ProjectAmenitiesAssembler assembler = mock(ProjectAmenitiesAssembler.class);
        ProjectDetailAnalyticsReader reader = new ProjectDetailAnalyticsReader(snapshots, amenities, assembler);
        ProjectMeterSnapshotEntity snapshot = ProjectMeterSnapshotEntity.builder()
                .averageAreaPrice(12500L)
                .priceAppreciationPercent(7.5)
                .amenityScore(64)
                .build();
        List<ProjectAmenityProgressEntity> rows = List.of(ProjectAmenityProgressEntity.builder().id(1L).build());
        ProjectAmenitiesResponse response = ProjectAmenitiesResponse.builder().completionPercent(64).build();
        when(snapshots.findByProjectId(27L)).thenReturn(Optional.of(snapshot));
        when(amenities.findByProjectIdAndActiveTrueAndPublicVisibleTrueOrderByCategoryDisplayOrderAscDisplayOrderAscIdAsc(27L))
                .thenReturn(rows);
        when(assembler.assemble(rows, 64)).thenReturn(response);

        var result = reader.read(27L);

        assertThat(result.snapshotAverageAreaPrice()).isEqualTo(12500L);
        assertThat(result.snapshotAppreciationPercent()).isEqualTo(7.5);
        assertThat(result.amenities()).isSameAs(response);
        verify(snapshots).findByProjectId(27L);
        verify(amenities).findByProjectIdAndActiveTrueAndPublicVisibleTrueOrderByCategoryDisplayOrderAscDisplayOrderAscIdAsc(27L);
    }
}
