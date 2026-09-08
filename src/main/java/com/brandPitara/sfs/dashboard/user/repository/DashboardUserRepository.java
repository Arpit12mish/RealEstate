package com.brandPitara.sfs.dashboard.user.repository;

import com.brandPitara.sfs.dashboard.common.enums.DashboardRole;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.brandPitara.sfs.security.identity.DashboardAuthenticationUserRow;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface DashboardUserRepository extends JpaRepository<DashboardUserEntity, Long>,
        JpaSpecificationExecutor<DashboardUserEntity> {

    Optional<DashboardUserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<DashboardUserEntity> findByRoleAndActiveTrue(DashboardRole role);

    List<DashboardUserEntity> findByActiveTrueOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from DashboardUserEntity u where u.id = :userId")
    Optional<DashboardUserEntity> findByIdForUpdate(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from DashboardUserEntity u
            where u.role = com.brandPitara.sfs.dashboard.common.enums.DashboardRole.ADMIN
              and u.active = true
            order by u.id
            """)
    List<DashboardUserEntity> lockActiveAdministrators();

    @Query("""
            select new com.brandPitara.sfs.security.identity.DashboardAuthenticationUserRow(
                u.id, u.email, u.name, u.role, u.active, p
            )
            from DashboardUserEntity u
            left join u.permissions p
            where u.id = :userId
            """)
    List<DashboardAuthenticationUserRow> findAuthenticationRowsById(@Param("userId") Long userId);
}
