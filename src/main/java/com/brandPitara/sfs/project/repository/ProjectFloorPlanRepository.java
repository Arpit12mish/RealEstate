package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectFloorPlanRepository extends JpaRepository<ProjectFloorPlanEntity, Long> {

  List<ProjectFloorPlanEntity> findByProjectIdAndDeletedFalseOrderBySortOrderAscIdAsc(Long projectId);

  List<ProjectFloorPlanEntity> findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long projectId);

  Optional<ProjectFloorPlanEntity> findByIdAndDeletedFalse(Long id);
}