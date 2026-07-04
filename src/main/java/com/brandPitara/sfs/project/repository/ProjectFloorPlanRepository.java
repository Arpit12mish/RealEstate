package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.project.entity.ProjectFloorPlanEntity;
import com.brandPitara.sfs.project.enums.UnitConfigurationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectFloorPlanRepository extends JpaRepository<ProjectFloorPlanEntity, Long> {

  List<ProjectFloorPlanEntity> findByProjectIdAndDeletedFalseOrderBySortOrderAscIdAsc(Long projectId);

  List<ProjectFloorPlanEntity> findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long projectId);

  List<ProjectFloorPlanEntity> findByProjectIdInAndActiveTrueAndDeletedFalseOrderByProjectIdAscSortOrderAscIdAsc(Collection<Long> projectIds);

  Optional<ProjectFloorPlanEntity> findByIdAndDeletedFalse(Long id);

  @Query("SELECT DISTINCT fp.project.id FROM ProjectFloorPlanEntity fp " +
         "WHERE fp.unitConfigurationType IN :types " +
         "AND fp.active = true AND fp.deleted = false")
  List<Long> findDistinctProjectIdsByUnitConfigurationTypes(
      @Param("types") List<UnitConfigurationType> types);
}
