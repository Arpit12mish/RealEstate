package com.brandPitara.sfs.project.service.reader;

import com.brandPitara.sfs.project.dto.ProjectConnectivityResponse;
import com.brandPitara.sfs.project.dto.ProjectMasterPlanResponse;
import com.brandPitara.sfs.project.mapper.ProjectConnectivityMapper;
import com.brandPitara.sfs.project.mapper.ProjectFloorPlanMapper;
import com.brandPitara.sfs.project.mapper.ProjectMasterPlanMapper;
import com.brandPitara.sfs.project.repository.ProjectConnectivityPlaceRepository;
import com.brandPitara.sfs.project.repository.ProjectConnectivityRepository;
import com.brandPitara.sfs.project.repository.ProjectFloorPlanRepository;
import com.brandPitara.sfs.project.repository.ProjectMasterPlanRepository;
import com.brandPitara.sfs.project.service.model.ProjectPublicContentData;
import com.brandPitara.sfs.project.service.model.ProjectPublicCoreData;
import com.brandPitara.sfs.project.service.model.ProjectPublicSectionFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

/**
 * Bounded Project Detail content read. The caller must first validate the project through
 * {@link ProjectPublicBaseReader}; standalone public endpoints retain their own visibility checks.
 */
@Service
@RequiredArgsConstructor
public class ProjectPublicContentReader {

    private final ProjectFloorPlanRepository floorPlanRepository;
    private final ProjectConnectivityRepository connectivityRepository;
    private final ProjectConnectivityPlaceRepository connectivityPlaceRepository;
    private final ProjectMasterPlanRepository masterPlanRepository;

    @Transactional(readOnly = true)
    public ProjectPublicContentData readAfterVisibilityCheck(ProjectPublicCoreData project) {
        Long projectId = project.getId();
        EnumSet<ProjectPublicSectionFailure> failures =
                EnumSet.noneOf(ProjectPublicSectionFailure.class);

        var floorPlans = floorPlanRepository
                .findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(projectId)
                .stream()
                .map(ProjectFloorPlanMapper::toResponse)
                .toList();

        ProjectConnectivityResponse connectivity = null;
        try {
            var overview = connectivityRepository
                    .findByProjectIdAndActiveTrueAndDeletedFalse(projectId)
                    .orElse(null);
            var places = connectivityPlaceRepository
                    .findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(projectId);
            connectivity = ProjectConnectivityMapper.toResponse(
                    overview,
                    projectId,
                    project.getLatitude(),
                    project.getLongitude(),
                    project.getAddressLine(),
                    places
            );
        } catch (Exception ignored) {
            failures.add(ProjectPublicSectionFailure.CONNECTIVITY);
        }

        ProjectMasterPlanResponse masterPlan = null;
        try {
            masterPlan = masterPlanRepository
                    .findByProjectIdAndActiveTrueAndDeletedFalse(projectId)
                    .map(ProjectMasterPlanMapper::toPublicResponse)
                    .orElse(null);
        } catch (Exception ignored) {
            failures.add(ProjectPublicSectionFailure.MASTER_PLAN);
        }

        return new ProjectPublicContentData(floorPlans, connectivity, masterPlan, failures);
    }
}
