package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dto.profile.ProfileResponse;
import com.brandPitara.sfs.dto.profile.UpdateProfileRequest;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found for phone: " + phoneNumber));
        return toResponse(user);
    }

    @Override
    public ProfileResponse updateMyProfile(String phoneNumber, UpdateProfileRequest request) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found for phone: " + phoneNumber));

        // Update only if provided (patch style)
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim().toLowerCase();

            // Optional: prevent collisions if you want unique emails
            userRepository.findByEmail(newEmail).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new IllegalArgumentException("Email already in use");
                }
            });

            user.setEmail(newEmail);
        }

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private ProfileResponse toResponse(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .verified(user.isVerified())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
