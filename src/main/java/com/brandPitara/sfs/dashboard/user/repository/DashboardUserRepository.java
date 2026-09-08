package com.brandPitara.sfs.dashboard.user.repository;

import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.brandPitara.sfs.security.identity.DashboardAuthenticationUserSnapshot;

import java.util.List;
import java.util.Optional;

public interface DashboardUserRepository extends JpaRepository<DashboardUserEntity, Long> {

    Optional<DashboardUserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<DashboardUserEntity> findByRoleAndActiveTrue(DashboardRole role);

    List<DashboardUserEntity> findByActiveTrueOrderByIdAsc();

    @Query("""
            select new com.brandPitara.sfs.security.identity.DashboardAuthenticationUserSnapshot(
                u.id, u.email, u.name, u.role, u.active
            )
            from DashboardUserEntity u
            where u.id = :userId
            """)
    Optional<DashboardAuthenticationUserSnapshot> findAuthenticationSnapshotById(@Param("userId") Long userId);
}
