package com.brandPitara.sfs.projectmeter.repository;

import com.brandPitara.sfs.projectmeter.entity.ProjectPaymentMilestoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectPaymentMilestoneRepository extends JpaRepository<ProjectPaymentMilestoneEntity, Long> {
    List<ProjectPaymentMilestoneEntity> findByProjectIdAndActiveTrueOrderByDisplayOrderAscIdAsc(Long projectId);

    List<ProjectPaymentMilestoneEntity> findByProjectIdOrderByDisplayOrderAscIdAsc(Long projectId);

    Optional<ProjectPaymentMilestoneEntity> findByIdAndProjectId(Long id, Long projectId);
}