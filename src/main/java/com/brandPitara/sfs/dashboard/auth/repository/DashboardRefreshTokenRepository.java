package com.brandPitara.sfs.dashboard.auth.repository;

import com.brandPitara.sfs.dashboard.auth.entity.DashboardRefreshTokenEntity;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface DashboardRefreshTokenRepository extends JpaRepository<DashboardRefreshTokenEntity, Long> {

    Optional<DashboardRefreshTokenEntity> findByTokenHash(String tokenHash);

    List<DashboardRefreshTokenEntity> findByDashboardUserAndRevokedFalse(DashboardUserEntity dashboardUser);

    long countByDashboardUserAndRevokedFalseAndExpiresAtAfter(
            DashboardUserEntity dashboardUser,
            OffsetDateTime now
    );

    void deleteByRevokedTrueAndRevokedAtBefore(OffsetDateTime before);

    void deleteByExpiresAtBefore(OffsetDateTime before);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DashboardRefreshTokenEntity token
               set token.revoked = true, token.revokedAt = :revokedAt
             where token.dashboardUser.id = :dashboardUserId
               and token.revoked = false
            """)
    int revokeAllActiveByDashboardUserId(
            @Param("dashboardUserId") Long dashboardUserId,
            @Param("revokedAt") OffsetDateTime revokedAt
    );
}
