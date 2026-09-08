package com.brandPitara.sfs.mobileupdate.repository;

import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import com.brandPitara.sfs.mobileupdate.entity.MobileAppUpdatePolicyAuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileAppUpdatePolicyAuditRepository
        extends JpaRepository<MobileAppUpdatePolicyAuditEntity, Long> {

    Page<MobileAppUpdatePolicyAuditEntity> findByPlatformOrderByCreatedAtDesc(
            MobilePlatform platform, Pageable pageable);
}

