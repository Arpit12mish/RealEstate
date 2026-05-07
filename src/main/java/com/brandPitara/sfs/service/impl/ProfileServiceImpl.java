package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dto.profile.ConfirmProfilePhotoRequest;
import com.brandPitara.sfs.dto.profile.PresignProfilePhotoRequest;
import com.brandPitara.sfs.dto.profile.PresignProfilePhotoResponse;
import com.brandPitara.sfs.dto.profile.ProfileResponse;
import com.brandPitara.sfs.dto.profile.UpdateProfileRequest;
import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.media.config.S3Properties;
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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
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
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

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

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(storageKey)
                .contentType(request.getContentType())
                .build();

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(s3Properties.getPresignExpirySeconds()))
                .putObjectRequest(putReq)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignReq);

        return new PresignProfilePhotoResponse(
                presigned.url().toString(),
                buildPublicUrl(storageKey),
                storageKey,
                s3Properties.getPresignExpirySeconds()
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

    private String buildPublicUrl(String storageKey) {
        String base = s3Properties.getPublicBaseUrl();
        if (base != null && !base.isBlank()) {
            String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
            return normalized + "/" + storageKey;
        }
        return "https://" + s3Properties.getBucket() + ".s3." + s3Properties.getRegion() + ".amazonaws.com/" + storageKey;
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