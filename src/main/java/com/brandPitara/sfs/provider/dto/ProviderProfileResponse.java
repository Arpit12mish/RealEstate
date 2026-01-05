package com.brandPitara.sfs.provider.dto;

import com.brandPitara.sfs.provider.enums.ProviderType;
import com.brandPitara.sfs.provider.enums.VerificationStatus;

import java.util.List;

public record ProviderProfileResponse(
        Long id,
        Long userId,
        ProviderType providerType,
        String displayName,
        String headline,
        String bio,
        Long primaryCategoryId,
        String primaryCategoryName,
        Integer experienceYears,
        String businessName,
        String gstNumber,
        VerificationStatus verificationStatus,
        boolean featured,
        Long linkedBusinessId,
        List<ServiceAreaResponse> serviceAreas
) {
    public record ServiceAreaResponse(
            Long cityId,
            String cityName,
            String locality,
            String pincode,
            Double latitude,
            Double longitude
    ) {}
}
