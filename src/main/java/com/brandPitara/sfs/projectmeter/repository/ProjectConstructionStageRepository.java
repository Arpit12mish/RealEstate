package com.brandPitara.sfs.projectmeter.repository;

import com.brandPitara.sfs.projectmeter.entity.ProjectConstructionStageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectConstructionStageRepository extends JpaRepository<ProjectConstructionStageEntity, Long> {
    List<ProjectConstructionStageEntity> findByProjectIdOrderByDisplayOrderAscIdAsc(Long projectId);
}