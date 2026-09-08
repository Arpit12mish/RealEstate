package com.brandPitara.sfs.mobileupdate.service;

import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import com.brandPitara.sfs.mobileupdate.PolicyAuditAction;
import com.brandPitara.sfs.mobileupdate.dto.DashboardMobileAppUpdatePolicyResponse;
import com.brandPitara.sfs.mobileupdate.dto.MobileAppUpdatePolicyAuditResponse;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MobileAppUpdatePolicyAuditService {

    void record(
            PolicyAuditAction action,
            DashboardMobileAppUpdatePolicyResponse previousPolicy,
            DashboardMobileAppUpdatePolicyResponse newPolicy,
            DashboardUserEntity actor,
            String changeReason);

    Page<MobileAppUpdatePolicyAuditResponse> list(MobilePlatform platform, Pageable pageable);
}

