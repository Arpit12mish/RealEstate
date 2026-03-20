package com.brandPitara.sfs.project.repository;

import com.brandPitara.sfs.project.entity.ProjectConnectivityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectConnectivityRepository extends JpaRepository<ProjectConnectivityEntity, Long> {

  Optional<ProjectConnectivityEntity> findByProjectIdAndDeletedFalse(Long projectId);

  Optional<ProjectConnectivityEntity> findByProjectIdAndActiveTrueAndDeletedFalse(Long projectId);
}