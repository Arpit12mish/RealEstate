package com.brandPitara.sfs.projectmeter.repository;

import com.brandPitara.sfs.projectmeter.entity.ProjectConstructionStageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectConstructionStageRepository extends JpaRepository<ProjectConstructionStageEntity, Long> {
    List<ProjectConstructionStageEntity> findByProjectIdOrderByDisplayOrderAscIdAsc(Long projectId);
    List<ProjectConstructionStageEntity> findByProjectIdInOrderByProjectIdAscDisplayOrderAscIdAsc(Collection<Long> projectIds);
    Optional<ProjectConstructionStageEntity> findByIdAndProjectId(Long id, Long projectId);
}