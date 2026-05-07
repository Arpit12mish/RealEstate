package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.GuestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface GuestSessionRepository extends JpaRepository<GuestSession, Long> {

    Optional<GuestSession> findByInstallationId(String installationId);

    Optional<GuestSession> findByInstallationIdAndActiveTrue(String installationId);



    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update GuestSession gs
        set gs.linkedUser = null,
            gs.linkedAt = null
        where gs.linkedUser.id = :userId
    """)
    int unlinkUserFromGuestSessions(@Param("userId") Long userId);
}