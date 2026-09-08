package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.GuestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface GuestSessionRepository extends JpaRepository<GuestSession, Long> {

    Optional<GuestSession> findFirstByInstallationIdAndActiveTrueAndLinkedUserIsNullOrderByIdDesc(
            String installationId
    );

    /**
     * Serializes guest lifecycle changes for one installation, including the
     * no-row-yet creation case that a JPA row lock cannot protect. The lock is
     * transaction-scoped and PostgreSQL releases it automatically on commit or
     * rollback.
     */
    @Query(value = """
            select 1
            from pg_advisory_xact_lock(hashtextextended(cast(:installationId as text), 0))
            """, nativeQuery = true)
    Integer lockInstallation(@Param("installationId") String installationId);

    void deleteByLinkedUser_Id(Long userId);
}
