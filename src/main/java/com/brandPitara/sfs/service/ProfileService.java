package com.brandPitara.sfs.service;

import com.brandPitara.sfs.dto.profile.ProfileResponse;
import com.brandPitara.sfs.dto.profile.UpdateProfileRequest;

public interface ProfileService {
    ProfileResponse getMyProfile(String phoneNumber);
    ProfileResponse updateMyProfile(String phoneNumber, UpdateProfileRequest request);
}
