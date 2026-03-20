package com.brandPitara.sfs.projectmeter.repository;

import com.brandPitara.sfs.projectmeter.entity.ProjectLandUtilizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectLandUtilizationRepository extends JpaRepository<ProjectLandUtilizationEntity, Long> {
    Optional<ProjectLandUtilizationEntity> findByProjectId(Long projectId);
}