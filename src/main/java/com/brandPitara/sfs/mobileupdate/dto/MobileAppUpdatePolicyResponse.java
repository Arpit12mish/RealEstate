package com.brandPitara.sfs.mobileupdate.dto;

import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import com.brandPitara.sfs.mobileupdate.UpdateStatus;

public record MobileAppUpdatePolicyResponse(
        MobilePlatform platform,
        UpdateStatus status,
        String latestVersion,
        Long latestBuild,
        Long minimumSupportedBuild,
        String title,
        String message,
        String releaseNotes,
        Integer remindAfterHours,
        String storeUrl
) {
}
