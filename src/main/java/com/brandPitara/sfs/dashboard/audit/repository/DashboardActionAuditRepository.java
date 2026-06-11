package com.brandPitara.sfs.dashboard.audit.repository;

import com.brandPitara.sfs.dashboard.audit.entity.DashboardActionAuditEntity;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DashboardActionAuditRepository
        extends JpaRepository<DashboardActionAuditEntity, Long>,
                JpaSpecificationExecutor<DashboardActionAuditEntity> {

    List<DashboardActionAuditEntity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            ReviewEntityType entityType, Long entityId);

    Page<DashboardActionAuditEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);

    Page<DashboardActionAuditEntity> findByDashboardUserIdOrderByCreatedAtDesc(Long dashboardUserId, Pageable pageable);
}
