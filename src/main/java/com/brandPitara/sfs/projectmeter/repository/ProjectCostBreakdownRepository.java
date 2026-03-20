package com.brandPitara.sfs.projectmeter.repository;

import com.brandPitara.sfs.projectmeter.entity.ProjectCostBreakdownEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectCostBreakdownRepository extends JpaRepository<ProjectCostBreakdownEntity, Long> {
    Optional<ProjectCostBreakdownEntity> findByProjectId(Long projectId);
}