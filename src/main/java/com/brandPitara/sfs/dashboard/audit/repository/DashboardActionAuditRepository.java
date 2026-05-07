package com.brandPitara.sfs.dashboard.audit.repository;

import com.brandPitara.sfs.dashboard.audit.entity.DashboardActionAuditEntity;
import com.brandPitara.sfs.dashboard.common.enums.DashboardAuditAction;
import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.common.enums.ReviewEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface DashboardActionAuditRepository extends JpaRepository<DashboardActionAuditEntity, Long> {

    List<DashboardActionAuditEntity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            ReviewEntityType entityType, Long entityId);

    Page<DashboardActionAuditEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);

    Page<DashboardActionAuditEntity> findByDashboardUserIdOrderByCreatedAtDesc(Long dashboardUserId, Pageable pageable);

    @Query("""
            SELECT a FROM DashboardActionAuditEntity a
            WHERE (:fromDate IS NULL OR a.createdAt >= :fromDate)
              AND (:toDate   IS NULL OR a.createdAt <  :toDate)
              AND (:action     IS NULL OR a.action           = :action)
              AND (:entityType IS NULL OR a.entityType       = :entityType)
              AND (:userRole   IS NULL OR a.dashboardUserRole = :userRole)
            ORDER BY a.createdAt DESC
            """)
    Page<DashboardActionAuditEntity> findAllFiltered(
            @Param("fromDate")   OffsetDateTime fromDate,
            @Param("toDate")     OffsetDateTime toDate,
            @Param("action")     DashboardAuditAction action,
            @Param("entityType") ReviewEntityType entityType,
            @Param("userRole")   DashboardRole userRole,
            Pageable pageable
    );
}
