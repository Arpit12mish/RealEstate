package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dto.profile.ConfirmProfilePhotoRequest;
import com.brandPitara.sfs.dto.profile.PresignProfilePhotoRequest;
import com.brandPitara.sfs.dto.profile.PresignProfilePhotoResponse;
import com.brandPitara.sfs.dto.profile.ProfileResponse;
import com.brandPitara.sfs.dto.profile.UpdateProfileRequest;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.media.service.MediaStorageService;
import com.brandPitara.sfs.media.service.PresignedUploadRequest;
import com.brandPitara.sfs.media.service.PresignedUploadResult;
import com.brandPitara.sfs.repository.FavoriteRepository;
import com.brandPitara.sfs.repository.GuestSessionRepository;
import com.brandPitara.sfs.repository.LoginHistoryRepository;
import com.brandPitara.sfs.repository.RefreshTokenRepository;
import com.brandPitara.sfs.repository.UserFavoriteRepository;
import com.brandPitara.sfs.repository.UserRepository;
import com.brandPitara.sfs.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserFavoriteRepository userFavoriteRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final GuestSessionRepository guestSessionRepository;
    private final MediaStorageService mediaStorageService;

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

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim().toLowerCase();

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

    @Override
    public PresignProfilePhotoResponse createProfilePhotoPresign(String phoneNumber, PresignProfilePhotoRequest request) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found for phone: " + phoneNumber));

        String ext = extFromContentType(request.getContentType());
        String storageKey = "users/" + user.getId() + "/profile/" + UUID.randomUUID() + "." + ext;

        PresignedUploadResult result = mediaStorageService.createPresignedUpload(
                new PresignedUploadRequest(storageKey, request.getContentType())
        );

        return new PresignProfilePhotoResponse(
                result.uploadUrl(),
                result.publicUrl(),
                result.storageKey(),
                result.expiresInSeconds()
        );
    }

    @Override
    public ProfileResponse confirmProfilePhoto(String phoneNumber, ConfirmProfilePhotoRequest request) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found for phone: " + phoneNumber));

        user.setProfilePhotoUrl(request.getUrl());
        user.setProfilePhotoStorageKey(request.getStorageKey());

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Override
    public void deleteAccount(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found for phone: " + phoneNumber));

        Long userId = user.getId();

        // Important: unlink guest sessions first, otherwise FK blocks user delete
        guestSessionRepository.unlinkUserFromGuestSessions(userId);

        refreshTokenRepository.deleteAllByUserId(userId);
        favoriteRepository.deleteAllByUserId(userId);
        userFavoriteRepository.deleteAllByUserId(userId);
        loginHistoryRepository.nullifyUserById(userId);

        userRepository.delete(user);
    }

    private String extFromContentType(String ct) {
        return switch (ct) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private ProfileResponse toResponse(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .verified(user.isVerified())
                .role(user.getRole())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}