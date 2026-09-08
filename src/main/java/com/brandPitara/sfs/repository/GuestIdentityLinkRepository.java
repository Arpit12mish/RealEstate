package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.GuestIdentityLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuestIdentityLinkRepository extends JpaRepository<GuestIdentityLink, Long> {

    Optional<GuestIdentityLink> findByGuestSession_Id(Long guestSessionId);
}
