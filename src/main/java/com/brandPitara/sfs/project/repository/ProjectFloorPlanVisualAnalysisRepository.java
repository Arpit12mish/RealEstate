package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.project.entity.ProjectFloorPlanVisualAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectFloorPlanVisualAnalysisRepository extends JpaRepository<ProjectFloorPlanVisualAnalysisEntity, Long> {

  // Dashboard: existing row regardless of active/deleted (get-or-create target)
  Optional<ProjectFloorPlanVisualAnalysisEntity> findByFloorPlanIdAndDeletedFalse(Long floorPlanId);

  // Public: only when active and not deleted
  Optional<ProjectFloorPlanVisualAnalysisEntity> findByFloorPlanIdAndActiveTrueAndDeletedFalse(Long floorPlanId);
}
