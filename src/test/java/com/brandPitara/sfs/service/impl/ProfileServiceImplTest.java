package com.brandPitara.sfs.service.impl;

import com.brandPitara.sfs.dto.profile.PresignProfilePhotoRequest;
import com.brandPitara.sfs.dto.profile.PresignProfilePhotoResponse;
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
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Proves ProfileServiceImpl.createProfilePhotoPresign calls the
 * MediaStorageService port (not S3Presigner directly) and preserves the
 * profile-scoped storage-key format and returned URL/expiry behavior.
 */
class ProfileServiceImplTest {

    @Test
    void profilePhotoPresignBuildsUserScopedKeyAndDelegatesToPort() {
        UserRepository userRepository = mock(UserRepository.class);
        MediaStorageService mediaStorageService = mock(MediaStorageService.class);
        ProfileServiceImpl service = new ProfileServiceImpl(
                userRepository,
                mock(RefreshTokenRepository.class),
                mock(FavoriteRepository.class),
                mock(UserFavoriteRepository.class),
                mock(LoginHistoryRepository.class),
                mock(GuestSessionRepository.class),
                mediaStorageService
        );

        User user = new User();
        user.setId(9L);
        user.setPhoneNumber("+919876500000");
        when(userRepository.findByPhoneNumber("+919876500000")).thenReturn(Optional.of(user));
        when(mediaStorageService.createPresignedUpload(any(PresignedUploadRequest.class)))
                .thenAnswer(invocation -> {
                    PresignedUploadRequest req = invocation.getArgument(0);
                    return new PresignedUploadResult(
                            "https://upload.example.com/presigned",
                            "https://cdn.squarefootstory.com/" + req.storageKey(),
                            req.storageKey(),
                            300,
                            Map.of()
                    );
                });

        PresignProfilePhotoRequest request = new PresignProfilePhotoRequest();
        request.setContentType("image/webp");

        PresignProfilePhotoResponse response = service.createProfilePhotoPresign("+919876500000", request);

        assertThat(response.getUploadUrl()).isEqualTo("https://upload.example.com/presigned");
        assertThat(response.getStorageKey()).startsWith("users/9/profile/").endsWith(".webp");
        assertThat(response.getPublicUrl()).isEqualTo("https://cdn.squarefootstory.com/" + response.getStorageKey());
        assertThat(response.getExpiresInSeconds()).isEqualTo(300);

        verify(mediaStorageService).createPresignedUpload(argThat(req ->
                req.contentType().equals("image/webp") && req.storageKey().startsWith("users/9/profile/")
        ));
    }

    @Test
    void profilePhotoPresignRejectsUnknownUserAndNeverCallsThePort() {
        UserRepository userRepository = mock(UserRepository.class);
        MediaStorageService mediaStorageService = mock(MediaStorageService.class);
        ProfileServiceImpl service = new ProfileServiceImpl(
                userRepository,
                mock(RefreshTokenRepository.class),
                mock(FavoriteRepository.class),
                mock(UserFavoriteRepository.class),
                mock(LoginHistoryRepository.class),
                mock(GuestSessionRepository.class),
                mediaStorageService
        );

        when(userRepository.findByPhoneNumber("+919999999999")).thenReturn(Optional.empty());

        PresignProfilePhotoRequest request = new PresignProfilePhotoRequest();
        request.setContentType("image/png");

        assertThatThrownBy(() -> service.createProfilePhotoPresign("+919999999999", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verifyNoInteractions(mediaStorageService);
    }
}
