package com.brandPitara.sfs.provider.service;

import com.brandPitara.sfs.provider.dto.ProviderProfileResponse;
import com.brandPitara.sfs.provider.dto.ProviderProfileUpsertRequest;

import java.util.List;

public interface ProviderProfileService {

    // used by onboarding + later "edit profile" too
    ProviderProfileResponse onboardOrUpdateMyProfile(Long currentUserId, ProviderProfileUpsertRequest request);

    ProviderProfileResponse getMyProfile(Long currentUserId);

    ProviderProfileResponse getPublicProfile(Long providerId);

    List<ProviderProfileResponse> getSimilarProviders(Long providerId, int limit);
}
