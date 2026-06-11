package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.project.entity.ProjectFloorPlanRoomDimensionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectFloorPlanRoomDimensionRepository extends JpaRepository<ProjectFloorPlanRoomDimensionEntity, Long> {

  // Admin: all non-deleted rooms
  List<ProjectFloorPlanRoomDimensionEntity> findByFloorPlanIdAndDeletedFalseOrderBySortOrderAscIdAsc(Long floorPlanId);

  // Public: active + non-deleted
  List<ProjectFloorPlanRoomDimensionEntity> findByFloorPlanIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long floorPlanId);

  Optional<ProjectFloorPlanRoomDimensionEntity> findByIdAndFloorPlanIdAndDeletedFalse(Long id, Long floorPlanId);

  void deleteByFloorPlanId(Long floorPlanId);
}
