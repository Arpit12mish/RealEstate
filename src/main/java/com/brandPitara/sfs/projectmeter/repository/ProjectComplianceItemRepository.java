package com.brandPitara.sfs.projectmeter.repository;

import com.brandPitara.sfs.projectmeter.entity.ProjectComplianceItemEntity;
import com.brandPitara.sfs.projectmeter.enums.ProjectComplianceGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectComplianceItemRepository extends JpaRepository<ProjectComplianceItemEntity, Long> {
    List<ProjectComplianceItemEntity> findByProjectIdAndItemGroupOrderByDisplayOrderAscIdAsc(Long projectId, ProjectComplianceGroup itemGroup);
    List<ProjectComplianceItemEntity> findByProjectIdOrderByItemGroupAscDisplayOrderAscIdAsc(Long projectId);
    List<ProjectComplianceItemEntity> findByProjectIdInOrderByProjectIdAscItemGroupAscDisplayOrderAscIdAsc(Collection<Long> projectIds);

    List<ProjectComplianceItemEntity> findByProjectIdOrderByDisplayOrderAscIdAsc(Long projectId);

    Optional<ProjectComplianceItemEntity> findByIdAndProjectId(Long id, Long projectId);
}