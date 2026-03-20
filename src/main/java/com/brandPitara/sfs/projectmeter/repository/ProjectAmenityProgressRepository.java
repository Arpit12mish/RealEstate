package com.brandPitara.sfs.projectmeter.repository;

import com.brandPitara.sfs.projectmeter.entity.ProjectAmenityProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectAmenityProgressRepository extends JpaRepository<ProjectAmenityProgressEntity, Long> {
    List<ProjectAmenityProgressEntity> findByProjectIdOrderByDisplayOrderAscIdAsc(Long projectId);
}