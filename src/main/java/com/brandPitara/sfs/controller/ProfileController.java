package com.brandPitara.sfs.controller;

import com.brandPitara.sfs.dto.profile.ProfileResponse;
import com.brandPitara.sfs.dto.profile.UpdateProfileRequest;
import com.brandPitara.sfs.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ProfileResponse getMyProfile(Authentication authentication) {
        String phoneNumber = authentication.getName(); // JWT subject
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
}
