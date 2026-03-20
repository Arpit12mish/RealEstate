package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.project.entity.ProjectConnectivityPlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectConnectivityPlaceRepository extends JpaRepository<ProjectConnectivityPlaceEntity, Long> {

  List<ProjectConnectivityPlaceEntity> findByProjectIdAndDeletedFalseOrderBySortOrderAscIdAsc(Long projectId);

  List<ProjectConnectivityPlaceEntity> findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long projectId);

  Optional<ProjectConnectivityPlaceEntity> findByIdAndProjectIdAndDeletedFalse(Long id, Long projectId);
}