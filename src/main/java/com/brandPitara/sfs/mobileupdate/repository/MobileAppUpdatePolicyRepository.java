package com.brandPitara.sfs.mobileupdate.repository;

import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import com.brandPitara.sfs.mobileupdate.entity.MobileAppUpdatePolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileAppUpdatePolicyRepository
        extends JpaRepository<MobileAppUpdatePolicyEntity, MobilePlatform> {
}
