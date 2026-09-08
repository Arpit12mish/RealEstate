package com.brandPitara.sfs.mobileupdate.dto;

import com.brandPitara.sfs.mobileupdate.EnforcementMode;
import com.brandPitara.sfs.mobileupdate.PolicyState;
import com.brandPitara.sfs.mobileupdate.StoreAvailability;
import jakarta.validation.constraints.*;

public record DashboardMobileAppUpdatePolicyRequest(
        @NotBlank @Size(max = 40) String latestVersion,
        @NotNull @PositiveOrZero @Max(9007199254740991L) Long latestBuild,
        @NotNull @PositiveOrZero @Max(9007199254740991L) Long minimumSupportedBuild,
        @NotBlank @Size(max = 500)
        @Pattern(regexp = "^https://.+", message = "storeUrl must be an HTTPS URL") String storeUrl,
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 500) String message,
        @Size(max = 10000) String releaseNotes,
        @NotNull @Min(1) @Max(720) Integer remindAfterHours,
        @NotNull PolicyState policyState,
        @NotNull StoreAvailability storeAvailability,
        @NotNull Boolean availabilityConfirmed,
        @NotNull EnforcementMode enforcementMode,
        @NotNull Boolean emergencyDisabled,
        @NotBlank @Size(max = 500) String changeReason,
        @NotNull @PositiveOrZero Long expectedVersion
) {
}
