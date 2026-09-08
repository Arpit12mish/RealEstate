package com.brandPitara.sfs.mobileupdate.service;

import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import com.brandPitara.sfs.mobileupdate.dto.DashboardMobileAppUpdatePolicyRequest;
import com.brandPitara.sfs.mobileupdate.dto.DashboardMobileAppUpdatePolicyResponse;
import com.brandPitara.sfs.mobileupdate.dto.MobileAppUpdatePolicyResponse;
import com.brandPitara.sfs.mobileupdate.dto.MobileAppUpdateEmergencyRequest;

import java.util.List;

public interface MobileAppUpdatePolicyService {
    MobileAppUpdatePolicyResponse evaluate(MobilePlatform platform, long currentBuild);
    List<DashboardMobileAppUpdatePolicyResponse> list();
    DashboardMobileAppUpdatePolicyResponse get(MobilePlatform platform);
    DashboardMobileAppUpdatePolicyResponse update(
            MobilePlatform platform, DashboardMobileAppUpdatePolicyRequest request);
    DashboardMobileAppUpdatePolicyResponse setEmergencyDisabled(
            MobilePlatform platform, MobileAppUpdateEmergencyRequest request);
}
