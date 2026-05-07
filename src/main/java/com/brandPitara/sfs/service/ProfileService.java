package com.brandPitara.sfs.service;

import com.brandPitara.sfs.dto.profile.ConfirmProfilePhotoRequest;
import com.brandPitara.sfs.dto.profile.PresignProfilePhotoRequest;
import com.brandPitara.sfs.dto.profile.PresignProfilePhotoResponse;
import com.brandPitara.sfs.dto.profile.ProfileResponse;
import com.brandPitara.sfs.dto.profile.UpdateProfileRequest;

public interface ProfileService {
    ProfileResponse getMyProfile(String phoneNumber);
    ProfileResponse updateMyProfile(String phoneNumber, UpdateProfileRequest request);

    PresignProfilePhotoResponse createProfilePhotoPresign(String phoneNumber, PresignProfilePhotoRequest request);
    ProfileResponse confirmProfilePhoto(String phoneNumber, ConfirmProfilePhotoRequest request);

    void deleteAccount(String phoneNumber);
}