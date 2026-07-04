package com.brandPitara.sfs.projectmeter.repository;

import com.brandPitara.sfs.projectmeter.entity.ProjectAmenityProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectAmenityProgressRepository extends JpaRepository<ProjectAmenityProgressEntity, Long> {

    List<ProjectAmenityProgressEntity> findByProjectIdOrderByDisplayOrderAscIdAsc(Long projectId);

    List<ProjectAmenityProgressEntity> findByProjectIdAndActiveTrueAndPublicVisibleTrueOrderByCategoryDisplayOrderAscDisplayOrderAscIdAsc(Long projectId);

    List<ProjectAmenityProgressEntity> findByProjectIdInAndActiveTrueAndPublicVisibleTrueOrderByProjectIdAscCategoryDisplayOrderAscDisplayOrderAscIdAsc(Collection<Long> projectIds);

    Optional<ProjectAmenityProgressEntity> findByIdAndProjectId(Long id, Long projectId);
}
