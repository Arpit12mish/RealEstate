package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.project.entity.ProjectFloorPlanInsightEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectFloorPlanInsightRepository extends JpaRepository<ProjectFloorPlanInsightEntity, Long> {

  // Admin: all non-deleted insights for a floor plan
  List<ProjectFloorPlanInsightEntity> findByFloorPlanIdAndDeletedFalseOrderBySortOrderAscIdAsc(Long floorPlanId);

  // Public: only publicVisible + active + non-deleted
  List<ProjectFloorPlanInsightEntity> findByFloorPlanIdAndPublicVisibleTrueAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long floorPlanId);

  // Check whether any publicVisible active insight exists for insightsAvailable flag
  boolean existsByFloorPlanIdAndPublicVisibleTrueAndActiveTrueAndDeletedFalse(Long floorPlanId);

  Optional<ProjectFloorPlanInsightEntity> findByIdAndFloorPlanId(Long id, Long floorPlanId);

  void deleteByFloorPlanId(Long floorPlanId);
}
