package com.brandPitara.sfs.controller;

import com.brandPitara.sfs.dto.profile.ConfirmProfilePhotoRequest;
import com.brandPitara.sfs.dto.profile.PresignProfilePhotoRequest;
import com.brandPitara.sfs.dto.profile.PresignProfilePhotoResponse;
import com.brandPitara.sfs.dto.profile.ProfileResponse;
import com.brandPitara.sfs.dto.profile.UpdateProfileRequest;
import com.brandPitara.sfs.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ProfileResponse getMyProfile(Authentication authentication) {
        String phoneNumber = authentication.getName();
        return profileService.getMyProfile(phoneNumber);
    }

    @PutMapping
    public ProfileResponse updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        String phoneNumber = authentication.getName();
        return profileService.updateMyProfile(phoneNumber, request);
    }

    @PostMapping("/photo/presign")
    public PresignProfilePhotoResponse createProfilePhotoPresign(
            Authentication authentication,
            @Valid @RequestBody PresignProfilePhotoRequest request
    ) {
        String phoneNumber = authentication.getName();
        return profileService.createProfilePhotoPresign(phoneNumber, request);
    }

    @PostMapping("/photo/confirm")
    public ProfileResponse confirmProfilePhoto(
            Authentication authentication,
            @Valid @RequestBody ConfirmProfilePhotoRequest request
    ) {
        String phoneNumber = authentication.getName();
        return profileService.confirmProfilePhoto(phoneNumber, request);
    }

    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(Authentication authentication) {
        String phoneNumber = authentication.getName();
        profileService.deleteAccount(phoneNumber);
    }
}