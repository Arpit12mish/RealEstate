package com.brandPitara.sfs.projectmeter.repository;

import com.brandPitara.sfs.projectmeter.entity.ProjectPaymentMilestoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectPaymentMilestoneRepository extends JpaRepository<ProjectPaymentMilestoneEntity, Long> {
    List<ProjectPaymentMilestoneEntity> findByProjectIdAndActiveTrueOrderByDisplayOrderAscIdAsc(Long projectId);
}