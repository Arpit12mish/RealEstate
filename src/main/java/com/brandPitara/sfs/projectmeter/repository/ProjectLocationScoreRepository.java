package com.brandPitara.sfs.projectmeter.repository;

import com.brandPitara.sfs.projectmeter.entity.ProjectLocationScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectLocationScoreRepository extends JpaRepository<ProjectLocationScoreEntity, Long> {
    Optional<ProjectLocationScoreEntity> findByProjectId(Long projectId);
}