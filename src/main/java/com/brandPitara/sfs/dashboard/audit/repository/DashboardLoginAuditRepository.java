package com.brandPitara.sfs.dashboard.audit.repository;

import com.brandPitara.sfs.dashboard.audit.entity.DashboardLoginAuditEntity;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardLoginAuditRepository extends JpaRepository<DashboardLoginAuditEntity, Long> {

    Page<DashboardLoginAuditEntity> findByDashboardUserOrderByCreatedAtDesc(
            DashboardUserEntity dashboardUser,
            Pageable pageable
    );

    Page<DashboardLoginAuditEntity> findByEmailIgnoreCaseOrderByCreatedAtDesc(
            String email,
            Pageable pageable
    );

    Page<DashboardLoginAuditEntity> findBySuccessOrderByCreatedAtDesc(
            Boolean success,
            Pageable pageable
    );
}