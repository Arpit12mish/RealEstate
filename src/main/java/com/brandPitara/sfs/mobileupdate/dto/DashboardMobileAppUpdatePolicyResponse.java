package com.brandPitara.sfs.mobileupdate.dto;

import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import com.brandPitara.sfs.mobileupdate.EnforcementMode;
import com.brandPitara.sfs.mobileupdate.PolicyState;
import com.brandPitara.sfs.mobileupdate.StoreAvailability;

import java.time.OffsetDateTime;

public record DashboardMobileAppUpdatePolicyResponse(
        MobilePlatform platform,
        String latestVersion,
        Long latestBuild,
        Long minimumSupportedBuild,
        String storeUrl,
        String title,
        String message,
        String releaseNotes,
        Integer remindAfterHours,
        PolicyState policyState,
        StoreAvailability storeAvailability,
        OffsetDateTime availabilityVerifiedAt,
        Long availabilityVerifiedBy,
        EnforcementMode enforcementMode,
        Boolean emergencyDisabled,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
