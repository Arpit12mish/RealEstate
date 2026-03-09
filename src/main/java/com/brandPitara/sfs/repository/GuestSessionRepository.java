package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.GuestSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuestSessionRepository extends JpaRepository<GuestSession, Long> {

    Optional<GuestSession> findByInstallationId(String installationId);

    Optional<GuestSession> findByInstallationIdAndActiveTrue(String installationId);
}