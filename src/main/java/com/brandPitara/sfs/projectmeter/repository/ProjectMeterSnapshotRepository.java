package com.brandPitara.sfs.projectmeter.repository;

import com.brandPitara.sfs.projectmeter.entity.ProjectMeterSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectMeterSnapshotRepository extends JpaRepository<ProjectMeterSnapshotEntity, Long> {
    Optional<ProjectMeterSnapshotEntity> findByProjectId(Long projectId);
    List<ProjectMeterSnapshotEntity> findByProjectIdIn(Collection<Long> projectIds);
}