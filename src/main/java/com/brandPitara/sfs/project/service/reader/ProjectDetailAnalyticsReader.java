package com.brandPitara.sfs.project.service.reader;

import com.brandPitara.sfs.project.service.model.ProjectDetailAnalyticsData;
import com.brandPitara.sfs.projectmeter.entity.ProjectAmenityProgressEntity;
import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import com.brandPitara.sfs.projectmeter.mapper.ProjectAmenitiesAssembler;
import com.brandPitara.sfs.projectmeter.repository.ProjectAmenityProgressRepository;
import com.brandPitara.sfs.projectmeter.repository.ProjectMeterSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectDetailAnalyticsReader {

    private final ProjectMeterSnapshotRepository snapshotRepository;
    private final ProjectAmenityProgressRepository amenityRepository;
    private final ProjectAmenitiesAssembler amenitiesAssembler;

    @Transactional(readOnly = true)
    public ProjectDetailAnalyticsData read(Long projectId) {
        ProjectMeterSnapshotEntity snapshot = snapshotRepository.findByProjectId(projectId).orElse(null);
        List<ProjectAmenityProgressEntity> amenities = amenityRepository
                .findByProjectIdAndActiveTrueAndPublicVisibleTrueOrderByCategoryDisplayOrderAscDisplayOrderAscIdAsc(
                        projectId);

        return new ProjectDetailAnalyticsData(
                snapshot != null ? snapshot.getAverageAreaPrice() : null,
                snapshot != null ? snapshot.getPriceAppreciationPercent() : null,
                amenitiesAssembler.assemble(
                        amenities,
                        snapshot != null ? snapshot.getAmenityScore() : null
                )
        );
    }
}
