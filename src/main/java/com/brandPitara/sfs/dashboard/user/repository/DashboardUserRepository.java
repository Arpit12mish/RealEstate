package com.brandPitara.sfs.dashboard.user.repository;

import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DashboardUserRepository extends JpaRepository<DashboardUserEntity, Long> {

    Optional<DashboardUserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<DashboardUserEntity> findByRoleAndActiveTrue(DashboardRole role);

    List<DashboardUserEntity> findByActiveTrueOrderByIdAsc();
}