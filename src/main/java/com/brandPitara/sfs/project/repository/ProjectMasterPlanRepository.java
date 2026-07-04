package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.project.entity.ProjectMasterPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectMasterPlanRepository extends JpaRepository<ProjectMasterPlanEntity, Long> {

  Optional<ProjectMasterPlanEntity> findByProjectIdAndDeletedFalse(Long projectId);

  Optional<ProjectMasterPlanEntity> findByProjectIdAndActiveTrueAndDeletedFalse(Long projectId);
}
