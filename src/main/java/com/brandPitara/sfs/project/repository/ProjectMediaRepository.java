package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.project.entity.ProjectMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMediaRepository extends JpaRepository<ProjectMediaEntity, Long> {

  List<ProjectMediaEntity> findByProjectIdAndDeletedFalseOrderBySortOrderAscIdDesc(Long projectId);

  List<ProjectMediaEntity> findByProjectIdAndActiveTrueAndDeletedFalseOrderBySortOrderAscIdDesc(Long projectId);

  Optional<ProjectMediaEntity> findByIdAndProjectIdAndDeletedFalse(Long id, Long projectId);
}
