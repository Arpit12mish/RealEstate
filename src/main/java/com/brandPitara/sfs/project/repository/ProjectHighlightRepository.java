package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.project.entity.ProjectHighlightEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectHighlightRepository extends JpaRepository<ProjectHighlightEntity, Long> {

  List<ProjectHighlightEntity> findByProjectIdAndDeletedFalseOrderBySortOrderAscIdAsc(Long projectId);

  List<ProjectHighlightEntity> findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdAsc(Long projectId);

  Optional<ProjectHighlightEntity> findByIdAndProjectIdAndDeletedFalse(Long id, Long projectId);
}